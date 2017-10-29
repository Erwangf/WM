package run.cluster

import org.apache.spark.sql.SparkSession
import run.shared.Word2VecExample

class Word2VecClusterExample {

  def main(args: Array[String]): Unit = {
    // default values
    var filePath = "hdfs://master.atscluster:8020/hp1.txt"
    var masterInfo = "spark://master.atscluster:7077"
    var targetWord = "Harry"



    val ss = SparkSession.builder().appName("Word2Vec Cluster Example").master(masterInfo).getOrCreate()

    val result =  Word2VecExample.runWord2Vec(ss,targetWord,filePath)
    result.foreach(println)
  }

}
