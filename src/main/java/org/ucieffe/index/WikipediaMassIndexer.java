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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.FlushModeType;
import javax.persistence.Persistence;

import org.hibernate.search.genericjpa.JPASearchFactoryController;
import org.hibernate.search.genericjpa.Setup;
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

	private static final int WORDS = 5_000;
	private static final int WORDS_PER_TEXT = 10_000;

	private static final boolean SETUP = false;

	private static void setup(EntityManagerFactory entityManagerFactory) {
		EntityManager entityManager = entityManagerFactory.createEntityManager();
		entityManager.setFlushMode( FlushModeType.AUTO );
		entityManager.getTransaction().begin();
		for ( int i = 0; i < WORDS; ++i ) {
			Text text = new Text();
			text.setId( i );
			text.setText( generateRandomText( WORDS_PER_TEXT ) );
			entityManager.persist( text );
			if ( i % 1000 == 0 ) {
				entityManager.flush();
				entityManager.getTransaction().commit();
				entityManager.getTransaction().begin();
			}
			System.out.println( "Text: " + i );
		}
		entityManager.getTransaction().commit();
		entityManager.close();

		System.out.println( "setup complete!" );
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "massindexer-test" );
		if ( SETUP ) {
			setup( entityManagerFactory );
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

		ftEntityManager.createIndexer( Text.class ).purgeAllOnStart( true ).optimizeAfterPurge( true ).optimizeOnFinish( true ).batchSizeToLoadIds( 400 )
				.batchSizeToLoadObjects( 2 ).threadsToLoadIds( 1 ).threadsToLoadObjects( 2 ).createNewIdEntityManagerAfter( 400 ).startAndWait();

		System.out.println( "indexing!" );

		ftEntityManager.close();
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
