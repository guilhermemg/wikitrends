package br.edu.ufcg.analytics.wikitrends.storage.serving2;

import java.io.Serializable;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

public class CassandraServingLayer2Manager implements Serializable {
	

	/**
	 * SerialVersionUID for CassandraResultsLayerManager
	 * 
	 * @since November 25, 2015
	 */
	private static final long serialVersionUID = -1465057423342253096L;

	// Prepare the schema
	public void createTables(Session session) {
            session.execute("DROP KEYSPACE IF EXISTS results");
            
            session.execute("CREATE KEYSPACE results WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
            
            session.execute("CREATE TABLE IF NOT EXISTS results." +
								"top_editor" +
								"(id UUID," +
								"editor TEXT," +
								"count INT," +
								
								"PRIMARY KEY((id), count)" +
								") WITH CLUSTERING ORDER BY (count DESC);"
					);
            
            session.execute("CREATE TABLE IF NOT EXISTS results." +
								"top_idiom" +
								"(id UUID," +
								"idiom TEXT," +
								"count INT," +
								
								"PRIMARY KEY((id), count)" +
								") WITH CLUSTERING ORDER BY (count DESC);"
					);
           
            session.execute("CREATE TABLE IF NOT EXISTS results.servers_ranking" +
								"(id text," +
								"position bigint," +
								"server_name TEXT," +
								"count INT," +
								"PRIMARY KEY((id), position)" +
								") WITH CLUSTERING ORDER BY (position ASC);"
					);
           
            session.execute("CREATE TABLE IF NOT EXISTS results." +
								"top_page" +
								"(id UUID," +
								"page TEXT," +
								"count BIGINT," +
								
								"PRIMARY KEY((id), count)" +
								") WITH CLUSTERING ORDER BY (count DESC);"
            		);
            
            session.execute("CREATE TABLE IF NOT EXISTS results." +
								"top_content_page" +
								"(id UUID," +
								"content_page TEXT," +
								"count BIGINT," +
								
								"PRIMARY KEY((id), count)" +
								") WITH CLUSTERING ORDER BY (count DESC);"
            		);
            
            session.execute("CREATE TABLE IF NOT EXISTS results." +
					            "absolute_values" +
								"(id UUID," +
								"all_edits BIGINT," +
								"minor_edits BIGINT," +
								"average_size BIGINT," +
								
								"distinct_pages_count BIGINT," +
								"distinct_editors_count INT," +
								"distinct_servers_count INT," +
								
								"smaller_origin BIGINT," +
					
								"PRIMARY KEY(id)" +
								");"
					);
	}
	
	
	
	/**
	 * Entry point
	 * 
	 * @param args
	 *            cassandra seed node address.
	 */
	public static void main(String[] args) {

		if (args.length < 2) {
			System.err.println(
					"Usage: java -cp <CLASSPATH> br.edu.ufcg.analytics.wikitrends.storage.batch1.CassandraServingLayerManager OPERATION <seed_address>");
			System.exit(1);
		}

		String operation = args[0];
		String seedNode = args[1];
		
		CassandraServingLayer2Manager manager = new CassandraServingLayer2Manager();
		
		switch (operation) {
		case "CREATE":
			try (Cluster cluster = Cluster.builder().addContactPoints(seedNode).build();
					Session session = cluster.newSession();) {
				manager.createTables(session);
			}
			break;
		default:
			System.err.println("Unsupported operation. Choose CREATE as operation.");
			break;
		}

	}
}
