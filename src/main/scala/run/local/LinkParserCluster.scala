package run.local

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{StructType, StructField, StringType}
import java.io._
import scala.util.matching.Regex
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row

object LinkParserCluster {
	/**
	 * take a title and a row string to create an Array of vertices
	 * @param title the page title (meaning the starting edge)
	 * @param bob the raw text to parse in order to extract references to other pages
	 * @return Array[(String,String)] of start_edge(title) -> end_edge(referenced page)
	 */
	def parse(title: String, bob : String): Array[(String, String)] = {
			val keyValPattern: Regex = "\\[\\[(.+?)\\]\\]".r
					var out_final: Array[(String, String)] = Array()
					for (e <- bob.split('\n')){
						for (patternMatch <- keyValPattern.findAllMatchIn(e)) {
							val link = patternMatch.group(1)
									if (!link.contains(":")) {
										var out = link
												out = out.split("#")(0)
												out = out.split("\\|")(0)
												out_final = out_final :+ (title,out)
									}
						}
					}
	return out_final
	}
	def main(args: Array[String]): Unit = {
			// default values
			var filePath = this.getClass.getClassLoader.getResource("xml.txt").getPath
					//					println(filePath)
					var masterInfo = "local[*]"
					val ss = SparkSession.builder().appName("LinkParser").master(masterInfo).getOrCreate()

					val sqlContext = ss.sqlContext
					val df = sqlContext.read
					.format("com.databricks.spark.xml")
					.option("rowTag", "page")
					.option("rootTag", "pages")
					.load(filePath)

					df.printSchema()
					
					//ns is 0 if it is a page (i.e not a disucssion, forum etc..), we select only title 
					//and revison from the dataframe created
					val raw=df.filter(df.col("ns").===(0)).select("title","revision.text")
					
          //Here I need your knowledge.
					val rows: RDD[Row] = raw.rdd
					val arout = rows.map(t => parse(t(0).toString(),t(1).toString())).collect()
					arout.foreach(e => e.foreach(i => println(i._1 +";" + i._2)))
	}
}
