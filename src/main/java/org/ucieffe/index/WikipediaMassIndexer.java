/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.ucieffe.index;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.Setup;
import org.hibernate.search.genericjpa.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.genericjpa.db.events.MySQLTriggerSQLStringSource;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.ucieffe.model.Text;

/**
 * Starts a batch operation to rebuild the Lucene index out of the database data.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org>
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class WikipediaMassIndexer {

	private static final int THREADS_FOR_SETUP = 10;
	private static final int TEXT_PER_THREAD = 5000;
	private static final int TEXT_COUNT = TEXT_PER_THREAD * THREADS_FOR_SETUP;

	private static CountDownLatch latch = new CountDownLatch( THREADS_FOR_SETUP );

	private static final int WORDS_PER_TEXT = 1_000;
	private static final boolean SETUP = true;

	private static void setup(EntityManagerFactory entityManagerFactory) {
		ExecutorService exec = Executors.newFixedThreadPool( THREADS_FOR_SETUP );
		for ( int i = 0; i < THREADS_FOR_SETUP; ++i ) {
			int startId = i * TEXT_PER_THREAD;
			exec.submit( () -> {
				EntityManager entityManager = entityManagerFactory.createEntityManager();

				entityManager.getTransaction().begin();
				entityManager.createQuery( "DELETE FROM Text" ).executeUpdate();
				entityManager.getTransaction().commit();

				entityManager.setFlushMode( FlushModeType.AUTO );
				entityManager.getTransaction().begin();
				for ( int textNumber = 0; textNumber < TEXT_PER_THREAD; ++textNumber ) {
					Text text = new Text();
					text.setId( startId + textNumber );
					text.setText( generateRandomText( WORDS_PER_TEXT ) );
					entityManager.persist( text );
					if ( textNumber % 1000 == 0 ) {
						entityManager.flush();
						entityManager.clear();
						entityManager.getTransaction().commit();
						entityManager.getTransaction().begin();
					}
					System.out.println( "Text: " + ( startId + textNumber ) );
				}
				entityManager.getTransaction().commit();
				entityManager.close();

				latch.countDown();
				System.out.println( "setup complete!" );
			} );
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "massindexer-test" );
		if ( SETUP ) {
			setup( entityManagerFactory );
			latch.await();
		}

		EntityManager entityManager = entityManagerFactory.createEntityManager();

		Properties properties = new Properties();
		try (InputStream is = WikipediaMassIndexer.class.getResourceAsStream( "/hsearch.properties" )) {
			properties.load( is );
		}
		properties.setProperty( "org.hibernate.search.genericjpa.searchfactory.name", "test" );
		properties.setProperty( "org.hibernate.search.genericjpa.searchfactory.triggerSource", MySQLTriggerSQLStringSource.class.getName() );
		properties.setProperty( "org.hibernate.search.genericjpa.searchfactory.type", "sql" );
		JPASearchFactoryController searchFactoryController = Setup.createUnmanagedSearchFactory( entityManagerFactory, properties, null );

		FullTextEntityManager ftEntityManager = searchFactoryController.getFullTextEntityManager( entityManager );

		ftEntityManager.createIndexer( Text.class ).purgeAllOnStart( true ).optimizeAfterPurge( true ).optimizeOnFinish( true ).batchSizeToLoadIds( 100 )
				.batchSizeToLoadObjects( 10 ).threadsToLoadIds( 1 ).threadsToLoadObjects( 20 )
				.progressMonitor( new MassIndexerProgressMonitor() {

					@Override
					public void objectsLoaded(Class<?> entityType, int count) {
						System.out.println( "objects loaded: " + count );
					}

					@Override
					public void idsLoaded(Class<?> entityType, int count) {
						System.out.println( "loaded ids: " + count );
					}

					@Override
					public void documentsBuilt(Class<?> entityType, int count) {
						System.out.println( "documents built: " + count );
					}

					@Override
					public void documentsAdded(int count) {
						System.out.println( "documents added: " + count );
					}

				} ).startAndWait();

		System.out.println( "indexing complete!" );

		ftEntityManager.close();

		searchFactoryController.close();
		entityManagerFactory.close();
	}

	public static String generateRandomText(int numberOfWords) {
		StringBuilder builder = new StringBuilder();
		Random random = new Random();
		for ( int i = 0; i < numberOfWords; i++ ) {
			char[] word = new char[random.nextInt( 4 ) + 15]; // words of length 3 through 10. (1 and 2 letter words are
																// boring.)
			for ( int j = 0; j < word.length; j++ ) {
				word[j] = (char) ( 'a' + random.nextInt( 26 ) );
			}
			builder.append( word ).append( " " );
		}
		return builder.toString();
	}

}
