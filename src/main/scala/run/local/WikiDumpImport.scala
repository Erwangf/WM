package run.local

import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._
import wikipedia._

import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.util.matching.Regex

object WikiDumpImport {

  def main(args: Array[String]): Unit = {
    var filePath = this.getClass.getClassLoader.getResource("xml.txt").getPath
    var masterInfo = "local[*]"
    val ss = SparkSession.builder().appName("LinkParser").master(masterInfo).getOrCreate()
    val df = importDumpAndGetDF(filePath, ss)

  }

  def importDumpAndGetDF(filePath: String, ss: SparkSession): DataFrame = {

    val sqlContext = ss.sqlContext
    var df = sqlContext.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("rootTag", "pages")
      .load(filePath)

    // ns is 0 if it is a page (i.e not a disucssion, forum etc..), we select only title
    // and revison from the dataframe created
    df = df.filter(df.col("ns").===(0)).select("title", "revision.text._VALUE").withColumnRenamed("_VALUE", "text")

    // sometimes some pages have a null text ( why ?? ) so we filter them
    df = df.filter(df.col("text").isNotNull)

    // We will work with DataFrames, not RDDs. DataFrames support SQL, so we will apply an user-defined function
    // (UDF) on one specific column, then create a new column ( .withColumn  )
    val parser = (s1: String, s2: String) => GraphOperator.LinkParser(s1, s2)

    // udf transform a function to an user-defined function, usable on columns
    val udfLinkParser = udf(parser)

    // Then we apply our spark-friendly parser function on our rows :
    df = df.withColumn("edges", udfLinkParser(df.col("title"), df.col("text")))

    // Next we clean the text from wikipedia markup
    val textCleaner = (s: String) => TextCleaner.cleanPage(s)
    val udfTextCleaner = udf(textCleaner)
    df = df.withColumn("text", udfTextCleaner(df.col("text")))


    //					Graph construction -> com to come
    import ss.implicits._
    var a = ss.sparkContext.parallelize(df.select("edges")
      .take(10).flatMap(_.get(0)
      .asInstanceOf[mutable.WrappedArray[Row]]
      .map(r => (r.get(0).asInstanceOf[String], r.get(1).asInstanceOf[String]))))
    var graph = GraphOperator.unweightedStringEdgeListViaJoin(a)
    //					graph.vertices.foreach(println)
    val direction: EdgeDirection = EdgeDirection.Either
    //			   var bob = graph.collectNeighborIds(direction).lookup(189).flatten.foreach(println(_))
    var ind = GraphOperator.pageRanker(graph, ss.sparkContext)
    ind.foreach(x => println(x))
    graph.vertices.filter(x => ind contains x._1).foreach(println(_))

    //					most_impo.vertices.foreach(x=> println(x._1 +" "+ x._2))
    //TODO : ecrire sur le disk

    ////					 Now we will extract all the edges
    ////					 first we take 1000 edges, then convert Array[WrappedArray[Row]] to Array[WrappedArray[(String,String)]]
    ////					 That's because when importing a Tuple (String,String), Spark convert it to a struct, which is basically a Row.
    ////					 So we got Rows (our edges) containings Rows (origin,destination)... :p
    //												    df.select("edges")
    //												      .take(10)
    //												      .map(_.get(0)
    //												        .asInstanceOf[mutable.WrappedArray[Row]]
    //												        .map(r => (r.get(0).asInstanceOf[String], r.get(1).asInstanceOf[String]))
    //												      ) // then we print all this edges
    //					      .foreach(e => e.foreach(i => println(i._1 + ";" + i._2)))
    ////					 Calculate number of vertices and edges with a Reduce
    ////					 we need to import sparkSession.implicits._ in order to map Row into Tuple
    //					    import ss.implicits._
    //					    val counts = df
    //					      .map(r => (1, r.get(r.fieldIndex("edges")).asInstanceOf[mutable.WrappedArray[Row]].length))
    //					      .reduce((a, b) => (a._1 + b._1, a._2 + b._2))
    //					    println(s"Nb Vertices : ${counts._1}\nNb Edges : ${counts._2}")


    //TODO : Clean text

    // we return the dataframe df
    df
  }


}
