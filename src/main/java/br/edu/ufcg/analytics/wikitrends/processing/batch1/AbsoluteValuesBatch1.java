package br.edu.ufcg.analytics.wikitrends.processing.batch1;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.spark.api.java.JavaRDD;

import com.datastax.spark.connector.japi.CassandraJavaUtil;

import br.edu.ufcg.analytics.wikitrends.processing.JobStatusID;
import br.edu.ufcg.analytics.wikitrends.storage.raw.types.EditChange;
import br.edu.ufcg.analytics.wikitrends.storage.serving1.types.AbsoluteValuesShot;

public class AbsoluteValuesBatch1 extends BatchLayer1Job {

	private static final long serialVersionUID = 4394268380743075556L;

	private static final JobStatusID ABSOLUTE_VALUES_STATUS_ID = JobStatusID.ABS_VALUES_BATCH_1;

	private String absoluteValuesTable;

	public AbsoluteValuesBatch1(Configuration configuration) {
		super(configuration, ABSOLUTE_VALUES_STATUS_ID);
		
		absoluteValuesTable = configuration.getString("wikitrends.serving1.cassandra.table.absolutevalues");
	}
	
	public void process() {
		JavaRDD<EditChange> wikipediaEdits = read()
				.filter(edit -> edit.getServerName().endsWith("wikipedia.org"))
				.cache();
		
		Long all_edits = countAllEdits(wikipediaEdits);
		Long minor_edits = countMinorEdits(wikipediaEdits);
		Long average_size = calcAverageEditLength(wikipediaEdits);

		Set<String> distincts_pages_set = distinctPages(wikipediaEdits);
		Set<String> distincts_editors_set = distinctEditors(wikipediaEdits);
		Set<String> distincts_servers_set = distinctServers(wikipediaEdits);

		Long smaller_origin = getOrigin(wikipediaEdits); 

		List<AbsoluteValuesShot> output = Arrays.asList(new AbsoluteValuesShot(all_edits,
				minor_edits,
				average_size,
				distincts_pages_set,
				distincts_editors_set,
				distincts_servers_set,
				smaller_origin,
				getCurrentTime().getYear(),
				getCurrentTime().getMonthValue(),
				getCurrentTime().getDayOfMonth(),
				getCurrentTime().getHour()));
		
		CassandraJavaUtil.javaFunctions(getJavaSparkContext().parallelize(output))
			.writerBuilder(getBatchViews1Keyspace(), absoluteValuesTable, mapToRow(AbsoluteValuesShot.class))
			.saveToCassandra();
	}
	
	private Long countAllEdits(JavaRDD<EditChange> wikipediaEdits) {
		return wikipediaEdits.count();
	}

	private Long countMinorEdits(JavaRDD<EditChange> wikipediaEdits) {
		return wikipediaEdits.filter(edit -> {
			return edit.getMinor() != null && edit.getMinor();
		}).count();
	}

	private Long calcAverageEditLength(JavaRDD<EditChange> wikipediaEdits) {
		if(wikipediaEdits.count() == 0) {
			return 0L;
		}
		JavaRDD<Long> result = wikipediaEdits.map( edit -> {
			Map<String, Long> length = edit.getLength();
			long oldLength = length.containsKey("old")? length.get("old"): 0;
			long newLength = length.containsKey("new")? length.get("new"): 0;
			return newLength - oldLength;
		});
		if(result.count() > 0) {
			return result.reduce((a, b) -> a+b) / result.count();
		}
		return 0L;
	}

	private Set<String> distinctPages(JavaRDD<EditChange> wikipediaEdits) {
		return new HashSet<String>(wikipediaEdits.map(edit -> edit.getTitle()).distinct().collect());
	}

	private Set<String> distinctServers(JavaRDD<EditChange> wikipediaEdits) {
		return new HashSet<String>(wikipediaEdits.map(edit -> edit.getServerName()).distinct().collect());
	}

	private Set<String> distinctEditors(JavaRDD<EditChange> wikipediaEdits) {
		return new HashSet<String>(
				wikipediaEdits.filter(edit -> {
					return edit.getBot() != null && !edit.getBot();
				})
				.map(edit -> edit.getUser()).distinct().collect());
	}

	private Long getOrigin(JavaRDD<EditChange> wikipediaEdits) {
		return wikipediaEdits.min(new EditChangeComparator()).getEventTimestamp().getTime();
	}
	
	
	private class EditChangeComparator implements Comparator<EditChange>, Serializable {
		private static final long serialVersionUID = 270079506147530661L;

		@Override
		public int compare(EditChange o1, EditChange o2) {
			return o1.getEventTimestamp().compareTo(o2.getEventTimestamp());
		}
	}


	@Override
	public JavaRDD<EditChange> read() {
	JavaRDD<EditChange> wikipediaEdits = CassandraJavaUtil.javaFunctions(getJavaSparkContext()).cassandraTable("master_dataset", "edits")
			.select("event_timestamp", "bot", "title", "server_name", "user", "namespace", "minor", "length")
			.where("year = ? and month = ? and day = ? and hour = ?", getCurrentTime().getYear(), getCurrentTime().getMonthValue(), getCurrentTime().getDayOfMonth(), getCurrentTime().getHour())
			.map( row -> {
				EditChange edit = new EditChange();
				edit.setEventTimestamp(row.getDate("event_timestamp"));
				edit.setBot(row.getBoolean("bot"));
				edit.setTitle(row.getString("title"));
				edit.setUser(row.getString("user"));
				edit.setNamespace(row.getInt("namespace"));
				edit.setServerName(row.getString("server_name"));
				edit.setMinor(row.getBoolean("minor"));
				edit.setLength(row.getMap("length", CassandraJavaUtil.typeConverter(String.class), CassandraJavaUtil.typeConverter(Long.class)));
				return edit;
			});
	return wikipediaEdits;
}
}

