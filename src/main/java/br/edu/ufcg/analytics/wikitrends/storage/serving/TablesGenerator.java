package br.edu.ufcg.analytics.wikitrends.storage.serving;

import org.apache.spark.api.java.JavaSparkContext;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;

public class TablesGenerator {
	protected JavaSparkContext sc;
	
	public TablesGenerator(JavaSparkContext sc2) {
		this.sc = sc2;
	}
	
	public void generateTables() {
		CassandraConnector connector = CassandraConnector.apply(sc.getConf());

        // Prepare the schema
        try (Session session = connector.openSession()) {
            session.execute("DROP KEYSPACE IF EXISTS batch_views");
            
            session.execute("CREATE KEYSPACE batch_views WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
            
            session.execute("CREATE TABLE IF NOT EXISTS batch_views." +
								"top_editors" +
							    "(id UUID," +
								"data MAP<TEXT,INT>," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((id), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
            
            session.execute("CREATE TABLE IF NOT EXISTS batch_views." +
								"top_idioms" +
								"(id UUID," +
								"data MAP<TEXT,INT>," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((id), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
           
            session.execute("CREATE TABLE IF NOT EXISTS batch_views." +
								"top_pages" +
								"(id UUID," +
								"data MAP<TEXT,INT>," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((id), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
            
            session.execute("CREATE TABLE IF NOT EXISTS batch_views." +
								"top_content_pages" +
								"(id UUID," +
								"data MAP<TEXT,INT>," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((id), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
            
            session.execute("CREATE TABLE IF NOT EXISTS batch_views." +
								"absolute_values" +
								"(id UUID," +
								"data MAP<TEXT,TEXT>," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((id), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
            
        }
	}
}
