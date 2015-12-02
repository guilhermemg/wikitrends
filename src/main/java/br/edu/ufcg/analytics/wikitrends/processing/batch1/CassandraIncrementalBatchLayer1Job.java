package br.edu.ufcg.analytics.wikitrends.processing.batch1;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.javaFunctions;
import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.spark.connector.japi.CassandraJavaUtil;
import com.datastax.spark.connector.japi.CassandraRow;

import br.edu.ufcg.analytics.wikitrends.storage.raw.types.EditType;
import br.edu.ufcg.analytics.wikitrends.storage.serving1.types.AbsoluteValuesShot;
import br.edu.ufcg.analytics.wikitrends.storage.serving1.types.TopClass;
import scala.Tuple2;

/**
 * @author Guilherme Gadelha
 * @author Ricardo Araújo Santos - ricoaraujosantos@gmail.com
 */
public class CassandraIncrementalBatchLayer1Job extends BatchLayer1Job {

	/**
	 * SerialVersionUID to class CassandraIncrementalBatchLayer1Job
	 * 
	 *  @since December 1, 2015
	 */
	private static final long serialVersionUID = 7386905244759035777L;

	private LocalDateTime now;
	private LocalDateTime end;
	private String[] seeds;

	private String batchViewsKeyspace;
	private String pagesTable;
	private String contentPagesTable;
	private String serversTable;
	private String usersTable;
	private String absoluteValuesTable;

	/**
	 * Default constructor
	 * 
	 * @param configuration
	 */
	public CassandraIncrementalBatchLayer1Job(Configuration configuration) {
		super(configuration);
		batchViewsKeyspace = configuration.getString("wikitrends.batch.cassandra.keyspace");
		pagesTable = configuration.getString("wikitrends.batch.cassandra.table.pages");
		contentPagesTable = configuration.getString("wikitrends.batch.cassandra.table.contentpages");
		serversTable = configuration.getString("wikitrends.batch.cassandra.table.servers");
		usersTable = configuration.getString("wikitrends.batch.cassandra.table.users");
		absoluteValuesTable = configuration.getString("wikitrends.batch.cassandra.table.absolutevalues");

		seeds = configuration.getStringArray("spark.cassandra.connection.host");

		try (Cluster cluster = Cluster.builder().addContactPoints(seeds).build();
				Session session = cluster.newSession();) {
			ResultSet resultSet = session.execute("SELECT * FROM batch_views.status WHERE id = ? LIMIT 1", "servers_ranking");
			List<Row> all = resultSet.all();
			if(!all.isEmpty()){
				Row row = all.get(0);
				now = LocalDateTime.of(row.getInt("year"), row.getInt("month"), row.getInt("day"), row.getInt("hour"), 0).plusHours(1) ;
			}else{
				now = LocalDateTime.ofInstant(Instant.ofEpochMilli(configuration.getLong("wikitrends.batch.incremental.starttime") * 1000), ZoneId.systemDefault());
			}
		}

		//		end = LocalDateTime.ofInstant(Instant.ofEpochMilli((System.currentTimeMillis() / 3600000) * 3600000), ZoneId.systemDefault());
		end = LocalDateTime.ofInstant(Instant.ofEpochMilli(configuration.getLong("wikitrends.batch.incremental.stoptime") * 1000), ZoneId.systemDefault());
		//		end = LocalDateTime.of(2015, 11, 9, 12, 0) ;
	}

	@Override
	public void run() {

		SparkConf conf = new SparkConf();
		conf.setAppName(configuration.getString("wikitrends.batch.id"));

		Iterator<String> keys = configuration.getKeys();
		while (keys.hasNext()) {
			String key = keys.next();
			conf.set(key, configuration.getString(key));
		}

		try(JavaSparkContext sc = new JavaSparkContext(conf);
				Cluster cluster = Cluster.builder().addContactPoints(seeds).build();
				Session session = cluster.newSession();) {

			while(now.isBefore(end)){
				this.run(sc);
				session.execute("INSERT INTO batch_views.status (id, year, month, day, hour) VALUES (?, ?, ?, ?, ?)", "servers_ranking", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
				now = now.plusHours(1);
			}
		}
	}

	@Override
	public void run(JavaSparkContext sc) {
		processEditorsRanking(sc);
	}

	public void processEditorsRanking(JavaSparkContext sc) {
		JavaRDD<EditType> wikipediaEdits = readEditorsRDD(sc)
				.filter(edit -> edit.getCommon_server_name().endsWith("wikipedia.org"))
				.cache();

		JavaPairRDD<String, Integer> userRDD = wikipediaEdits
				.mapPartitionsToPair( iterator -> {
					ArrayList<Tuple2<String, Integer>> pairs = new ArrayList<>();
					while(iterator.hasNext()){
						EditType edit = iterator.next();
						pairs.add(new Tuple2<String, Integer>(edit.getCommon_event_user(), 1));
					}
					return pairs;
				});
		JavaRDD<TopClass> userRanking = processRankingEntry(sc, userRDD);
		saveUserRanking(sc, userRanking);
	}

	protected JavaRDD<EditType> readEditorsRDD(JavaSparkContext sc) {
		JavaRDD<EditType> wikipediaEdits = javaFunctions(sc).cassandraTable("master_dataset", "edits")
				.select("common_event_bot", "common_server_name", "common_event_user", "common_event_namespace", "edit_minor")
				.where("year = ? and month = ? and day = ? and hour = ?", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour())
				.map(row -> {
					EditType edit = new EditType();
					edit.setCommon_event_bot(row.getBoolean("common_event_bot"));
					edit.setCommon_server_name(row.getString("common_server_name"));
					edit.setCommon_event_user(row.getString("common_event_user"));
					edit.setCommon_event_namespace(row.getInt("common_event_namespace"));
					edit.setEditMinor(row.getBoolean("edit_minor"));
					return edit;
				});
		return wikipediaEdits;
	}

	protected JavaRDD<TopClass> processRankingEntry(JavaSparkContext sc, JavaPairRDD<String,Integer> pairRDD) {
		JavaRDD<TopClass> result = pairRDD
				.reduceByKey( (a,b) -> a+b )
				.map( edit -> new TopClass(edit._1, (long) edit._2, now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour()) );
		
		return result;
	}

	private void saveUserRanking(JavaSparkContext sc, JavaRDD<TopClass> userRanking) {
		CassandraJavaUtil.javaFunctions(userRanking)
		.writerBuilder(batchViewsKeyspace, usersTable, mapToRow(TopClass.class))
		.saveToCassandra();
	}


	@Override
	protected JavaRDD<EditType> readRDD(JavaSparkContext sc) {
		JavaRDD<EditType> wikipediaEdits = javaFunctions(sc).cassandraTable("master_dataset", "edits")
				.select("event_time", "common_event_bot", "common_event_title", "common_server_name", "common_event_user",
						"common_event_namespace", "edit_minor", "edit_length")
				.where("year = ? and month = ? and day = ? and hour = ?", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour())
				.map(new Function<CassandraRow, EditType>() {
					private static final long serialVersionUID = 1L;

					@Override
					public EditType call(CassandraRow v1) throws Exception {
						EditType edit = new EditType();
						edit.setEvent_time(v1.getDate("event_time"));
						edit.setCommon_event_bot(v1.getBoolean("common_event_bot"));
						edit.setCommon_event_title(v1.getString("common_event_title"));
						edit.setCommon_event_user(v1.getString("common_event_user"));
						edit.setCommon_event_namespace(v1.getInt("common_event_namespace"));
						edit.setCommon_server_name(v1.getString("common_server_name"));
						edit.setEditMinor(v1.getBoolean("edit_minor"));
						edit.setEdit_length(v1.getMap("edit_length", CassandraJavaUtil.typeConverter(String.class), CassandraJavaUtil.typeConverter(Long.class)));
						return edit;
					}

				});
		return wikipediaEdits;
	}

	@Override
	protected void saveServerRanking(JavaSparkContext sc, JavaRDD<BatchLayer1Output<Integer>> serverRanking) {
		CassandraJavaUtil.javaFunctions(serverRanking.map(entry -> new TopClass(entry.getKey(), (long) entry.getValue(), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour())))
		.writerBuilder(batchViewsKeyspace, serversTable, mapToRow(TopClass.class))
		.saveToCassandra();
	}


	@Override
	protected void saveTitleRanking(JavaSparkContext sc, JavaRDD<BatchLayer1Output<Integer>> titleRanking) {
		CassandraJavaUtil.javaFunctions(titleRanking.map(entry -> new TopClass(entry.getKey(), (long) entry.getValue(), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour())))
		.writerBuilder(batchViewsKeyspace, pagesTable, mapToRow(TopClass.class))
		.saveToCassandra();
	}

	@Override
	protected void saveContentTitleRanking(JavaSparkContext sc,
			JavaRDD<BatchLayer1Output<Integer>> contentTitleRanking) {
		CassandraJavaUtil.javaFunctions(contentTitleRanking.map(entry -> new TopClass(entry.getKey(), (long) entry.getValue(), now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour())))
		.writerBuilder(batchViewsKeyspace, contentPagesTable, mapToRow(TopClass.class))
		.saveToCassandra();

	}

	/* (non-Javadoc)
	 * @see br.edu.ufcg.analytics.wikitrends.processing.batch1.BatchLayer1Job#processStatistics(org.apache.spark.api.java.JavaSparkContext, org.apache.spark.api.java.JavaRDD)
	 */
	@Override
	protected void processStatistics(JavaSparkContext sc, JavaRDD<EditType> wikipediaEdits) {
		Map<String, Long> edits_data = new HashMap<String, Long>();
		edits_data.put("all_edits", countAllEdits(wikipediaEdits));
		edits_data.put("minor_edits", countMinorEdits(wikipediaEdits));
		edits_data.put("average_size", calcAverageEditLength(wikipediaEdits));

		Set<String> distincts_pages_set = distinctPages(wikipediaEdits);
		Set<String> distincts_editors_set = distinctEditors(wikipediaEdits);
		Set<String> distincts_servers_set = distinctServers(wikipediaEdits);

		//		System.out.println(distincts_pages_set.size()); // 359185
		//		System.out.println(distincts_editors_set.size()); // 57978
		//		System.out.println(distincts_servers_set.size()); // 215

		Long smaller_origin = getOrigin(wikipediaEdits); 

		List<AbsoluteValuesShot> output = Arrays.asList(new AbsoluteValuesShot(edits_data, 
				distincts_pages_set,
				distincts_editors_set,
				distincts_servers_set,
				smaller_origin));
		CassandraJavaUtil.javaFunctions(sc.parallelize(output))
		.writerBuilder(batchViewsKeyspace, absoluteValuesTable, mapToRow(AbsoluteValuesShot.class))
		.saveToCassandra();
	}

}
