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

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.classic.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.ProjectionConstants;
import org.hibernate.search.Search;
import org.ucieffe.model.Text;

/**
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 *
 */
public class QueryTester {
	
	private static final int LOOPS = 1;
	private final SessionFactory sessionFactory;

	public QueryTester(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public static void main(String[] args) {
		Configuration cfg = new Configuration();
		cfg.configure();
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		QueryTester qt = new QueryTester( sessionFactory );
		qt.runTests();
	}

	private void runTests() {
		for ( int i = 0; i < LOOPS; i++ ) {
			Session session = sessionFactory.openSession();
			try {
				FullTextSession fullTextSession = Search.getFullTextSession( session );
				runTest( fullTextSession );
			}
			finally {
				session.close();
			}
		}
	}

	private void runTest(FullTextSession fullTextSession) {
		MatchAllDocsQuery q = new MatchAllDocsQuery();
		List<Object[]> list = fullTextSession
			.createFullTextQuery( q, Text.class )
			.setProjection( ProjectionConstants.DOCUMENT_ID, ProjectionConstants.THIS )
			.setMaxResults( 10 )
			.list();
		showResults(list);
	}

	private void showResults(List<Object[]> list) {
		for (Object[] e : list) {
			System.out.println( Arrays.toString( e ) );
		}
	}

}
