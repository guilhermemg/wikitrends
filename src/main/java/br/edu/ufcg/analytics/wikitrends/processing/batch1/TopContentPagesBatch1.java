package br.edu.ufcg.analytics.wikitrends.processing.batch1;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.mapToRow;

import java.util.ArrayList;

import org.apache.commons.configuration.Configuration;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import com.datastax.spark.connector.japi.CassandraJavaUtil;

import br.edu.ufcg.analytics.wikitrends.storage.raw.types.EditType;
import br.edu.ufcg.analytics.wikitrends.storage.serving1.types.TopClass;
import scala.Tuple2;

public class TopContentPagesBatch1 extends BatchLayer1Job {

	private static final long serialVersionUID = 5005439419731611631L;

	private String contentPagesTable;

	public TopContentPagesBatch1(Configuration configuration) {
		super(configuration);
		
		contentPagesTable = configuration.getString("wikitrends.batch.cassandra.table.contentpages");
	}

	public void process(JavaSparkContext sc) {
		JavaRDD<EditType> wikipediaEdits = read(sc)
				.filter(edit -> edit.getCommon_server_name().endsWith("wikipedia.org"))
				.cache();
		
		JavaPairRDD<String, Integer> contentTitleRDD = wikipediaEdits
			.filter(edits -> "0".equals(edits.getCommon_event_namespace()))
			.mapPartitionsToPair( iterator -> {
				ArrayList<Tuple2<String, Integer>> pairs = new ArrayList<>();
				while(iterator.hasNext()){
					EditType edit = iterator.next();
					pairs.add(new Tuple2<String, Integer>(edit.getCommon_event_title(), 1));
				}
				return pairs;
			});
		
		JavaRDD<TopClass> contentTitleRanking = processRankingEntry(sc, contentTitleRDD);
		
		CassandraJavaUtil.javaFunctions(contentTitleRanking)
			.writerBuilder(getBatchViewsKeyspace(), contentPagesTable, mapToRow(TopClass.class))
			.saveToCassandra();
	}
}