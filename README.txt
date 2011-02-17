This is a tool to perform some load testing on Hibernate Search and Infinispan's Lucene Directory.

It creates a database containing a dump of the Wikipedia, and then creates an Apache Lucene index.

The goal to provide reproducible load tests for:
 * Hibernate Search - http://search.hibernate.org/
 * Infinispan's distributed Lucene Directory implementation - http://infinispan.org/ - http://community.jboss.org/wiki/infinispanasadirectoryforlucene

As a source of documents to index we use a specific dump of the Wikipedia database.
MySQL is expected currently, there are some placeholders for PostgreSQL support (TODO)

1 - Setup a MySQL database, create users: http://dev.mysql.com/doc/refman/5.1/en/adding-users.html
	And create database: http://dev.mysql.com/doc/refman/5.1/en/create-database.html
	
2 - Modify db.properties file changing jdbc.schemaname, jdbc.username and jdbc.password with database name, user and password respectively.

The ANT build file has the following main tasks:

1 - have-empty-schema: it drops all the database tables and recreates them.

2 - download-wikipedia: it downloads the reference wikipedia dump, containing only last version of each article, in English only,
	downloading it from:
	http://download.wikimedia.org/enwiki/20101011/pages-meta-current.xml.bz2
	[WARNING! 12GB sized download]

3 - import-wikipedia: it executes data import downloading and running mwdumper.jar (described at http://www.mediawiki.org/wiki/Manual:MWDumper)
	This is the tool able to efficiently dump the wikipedia database, and restore it to your database.

4 - run-indexing: it creates Apache Lucene index from database content using the hibernate-search library.

5 - create-hibernate-config: it creates hibernate.cfg.xml and hibernatesearch-infinispan.cfg.xml configuration files using the content of db.properties file.

6 - clean: it cleans the environment. In particular it deletes the reference wikipedia dump and mwdumper.jar library 
	and get and apply the database schema.
	Before doing this, it asks confirmation to the user. If you do not want to be asked for confirmation, you have to add 'database.autoclenaup=n' property
	to db.properties file.

