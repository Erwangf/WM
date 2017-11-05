package run.local

import run.shared.LinkParser
import org.apache.spark.sql.{Row, SparkSession}
import scala.collection.mutable
import scala.util.matching.Regex
object LinkParserCluster {

  def main(args: Array[String]): Unit = {


    var filePath = this.getClass.getClassLoader.getResource("xml.txt").getPath

    var masterInfo = "local[*]"
    val ss = SparkSession.builder().appName("LinkParser").master(masterInfo).getOrCreate()

    val sqlContext = ss.sqlContext
    var df = sqlContext.read
      .format("com.databricks.spark.xml")
      .option("rowTag", "page")
      .option("rootTag", "pages")
      .load(filePath)

    df.printSchema()

    // ns is 0 if it is a page (i.e not a disucssion, forum etc..), we select only title
    // and revison from the dataframe created
    df = df.filter(df.col("ns").===(0)).select("title", "revision.text._VALUE").withColumnRenamed("_VALUE", "text")


    // We will work with DataFrames, not RDDs. DataFrames support SQL, so we will apply an user-defined function
    // (UDF) on one specific column, then create a new column ( .withColumn  )

    // we need to import spark.sql.functions._ in order to use the udf function
    import org.apache.spark.sql.functions._
    val parser = (s1: String, s2: String) => LinkParser.externalParser(s1, s2)

    // udf transform a function to an user-defined function, usable on columns
    val udfParser = udf(parser)

    // Then we apply our spark-friendly parser function on our rows :

    df = df.withColumn("edges", udfParser(df.col("title"), df.col("text")))

    println("Loaded "+df.count()+" pages !")

    // Now we will extract all the edges
    // first we take 1000 edges, then convert Array[WrappedArray[Row]] to Array[WrappedArray[(String,String)]]
    // That's because when importing a Tuple (String,String), Spark convert it to a struct, which is basically a Row.
    // So we got Rows (our edges) containings Rows (origin,destination)... :p
    df.select("edges")
      .take(1000)
      .map(_.get(0)
        .asInstanceOf[mutable.WrappedArray[Row]]
        .map(r => (r.get(0).asInstanceOf[String], r.get(1).asInstanceOf[String]))
      ) // then we print all this edges
      .foreach(e => e.foreach(i => println(i._1 + ";" + i._2)))
  }
}
