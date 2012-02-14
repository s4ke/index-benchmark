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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.ucieffe.model.Text;

/**
 * Starts a batch operation to rebuild the Lucene index out of the database data.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org>
 * @author Davide Di Somma <davide.disomma@gmail.com>
 */
public class WikipediaMassIndexer {

	public static void main(String[] args) {

		EntityManagerFactory entityManagerFactory = Persistence.createEntityManagerFactory( "wikipedia" );
		try {
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			FullTextEntityManager ftEntityManager = Search.getFullTextEntityManager( entityManager );
	
			MassIndexerProgressMonitor monitor = new SimpleIndexingProgressMonitor( 5000 );
			ftEntityManager.createIndexer( Text.class )
				.purgeAllOnStart( true )
				.optimizeAfterPurge( true )
				.optimizeOnFinish( true )
				.limitIndexedObjectsTo( 10000 ) // to try it out without waiting 30 minutes
				.batchSizeToLoadObjects( 30 )
				.threadsForSubsequentFetching( 4 )
				.threadsToLoadObjects( 8 )
				.idFetchSize( Integer.MIN_VALUE ) // MySQL special weirdness
				.threadsForIndexWriter( 3 )
				.progressMonitor( monitor )
				.cacheMode( CacheMode.IGNORE )
				.startAndWait();
		}
		catch ( InterruptedException e ) {
			e.printStackTrace();
		}
		finally {
			entityManagerFactory.close();
		}
		
	}
}
