package br.edu.ufcg.analytics.wikitrends.data;

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
            
            session.execute("CREATE KEYSPACE master_dataset WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
            
            session.execute("CREATE TABLE IF NOT EXISTS master_dataset." +
								"log" +
							    "(log_uuid UUID," +
							    "log_id INT," +
								"log_type TEXT," +
								"log_action TEXT," +
								"log_params TEXT," +
								"log_action_comment TEXT," +
								
					            "common_server_url TEXT," +
								"common_server_name TEXT," +
								"common_server_script_path TEXT," +
								"common_server_wiki TEXT," +
								
								"common_event_type TEXT," +
								"common_event_namespace TEXT," +
								"common_event_user TEXT," +
								"common_event_bot BOOLEAN," +
								"common_event_comment TEXT," +
								"common_event_title TEXT," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
			    				"PRIMARY KEY((log_uuid), year, month, day, hour)," +
			    				") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
            
            // to types 'edit' and 'external'
            session.execute("CREATE TABLE IF NOT EXISTS master_dataset." +
								"edit" +
							    "(edit_uuid UUID," + 
							    "edit_id INT," +
								"edit_minor BOOLEAN," +
								"edit_patrolled BOOLEAN," +
								"edit_length MAP<TEXT, INT>," +
								"edit_revision MAP<TEXT, INT>," +

					            "common_server_url TEXT," +
								"common_server_name TEXT," +
								"common_server_script_path TEXT," +
								"common_server_wiki TEXT," +
								
								"common_event_type TEXT," +
								"common_event_namespace TEXT," +
								"common_event_user TEXT," +
								"common_event_bot BOOLEAN," +
								"common_event_comment TEXT," +
								"common_event_title TEXT," +
								
								"year INT," +
								"month INT," +
								"day INT," +
								"hour INT," +
								"event_time TIMESTAMP," +
								
								"PRIMARY KEY((edit_uuid), year, month, day, hour)," +
								") WITH CLUSTERING ORDER BY (year DESC, month DESC, day DESC, hour DESC);"
            		);
        }
	}

}