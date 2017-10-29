package run.local

import org.apache.spark.sql.SparkSession
import run.shared.GraphXExample

object GraphXLocalExample {

  def main(args: Array[String]): Unit = {
    val ss = SparkSession.builder().appName("Word2Vec Local Example").master("local[*]").getOrCreate()
    GraphXExample.runGraphXExample(ss)
  }
}
