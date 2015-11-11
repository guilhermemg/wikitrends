package br.edu.ufcg.analytics.wikitrends;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

import br.edu.ufcg.analytics.wikitrends.data.DataGenerator;
import br.edu.ufcg.analytics.wikitrends.data.TablesGenerator;

public class Main2 {
	public static void main(String[] args) {
		SparkConf conf = new SparkConf();
        conf.setAppName("Java API demo");
        conf.setMaster("local");
        conf.set("spark.cassandra.connection.host", "localhost");
        
        JavaSparkContext sc = new JavaSparkContext(conf);
		
		TablesGenerator tGen = new TablesGenerator(sc);
		tGen.generateTables();
		
		DataGenerator gen = new DataGenerator();
		gen.populateDatabase("/home/guilhermemg/gadbi/wikitrends/spark/java/Wikitrends/sample.json");
	}
}