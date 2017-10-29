package run.local

import org.apache.spark.sql.SparkSession
import run.shared.Word2VecExample

object Word2VecLocalExample {




  def main(args: Array[String]): Unit = {

    // default values
    var filePath = Word2VecLocalExample.getClass.getClassLoader.getResource("hp1.txt").getPath
    var masterInfo = "local[*]"
    var targetWord = "Harry"



    val ss = SparkSession.builder().appName("Word2Vec Local Example").master(masterInfo).getOrCreate()

//    val filePath = "hdfs://master.atscluster:8020/hp1.txt"
//    val masterInfo = "spark://master.atscluster:7077"

    val result =  Word2VecExample.runWord2Vec(ss,targetWord,filePath)
    result.foreach(println)




  }
}
