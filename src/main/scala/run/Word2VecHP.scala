package run

import java.io.InputStream

import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.SparkSession

import scala.collection.mutable

object Word2VecHP {

  def distVec(a: Array[Double], b: Array[Double]): Double = {
    var sum = 0d
    for (elem <- a.indices) {
      if(a.length!=b.length)throw new Error("Alerte ! Les vecteurs n'ont pas la même taille !")
      sum = sum + ((a(elem) - b(elem)) * (a(elem) - b(elem)))
    }
    Math.sqrt(sum)
  }

  def addVec(a: Array[Double], b: Array[Double]): Array[Double] = {
    var result: Array[Double] = Array()

    for (elem <- a.indices) {
      result = result :+ (a(elem) + b(elem))
    }

    result
  }

  def subVec(a: Array[Double], b: Array[Double]): Array[Double] = {
    var result: Array[Double] = Array()

    for (elem <- a.indices) {
      result = result :+ (a(elem) - b(elem))
    }

    result
  }


  def main(args: Array[String]): Unit = {

//    val filePath = "hdfs://master.atscluster:8020/hp1.txt"
//    val masterInfo = "spark://master.atscluster:7077"
    import java.net.URL
    val filePath = Word2VecHP.getClass.getClassLoader.getResource("hp1.txt").getPath
//    val filePath = "C:\\Users\\Erwan\\Documents\\Projets\\wordembedding\\src\\main\\resources\\hp1.txt"
    val masterInfo = "local[*]"

    // initialisation de la session spark
    val ss = SparkSession.builder.appName("Word2Vec Harry Potter").master(masterInfo).getOrCreate()

    import ss.implicits._
    val lines = ss.sparkContext.textFile(filePath,64)
    val documentDF = lines.filter(_.length>0).map(_.split(" ")).toDF("text")

    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(300)
      .setWindowSize(5)
      .setMaxIter(2)
      .setNumPartitions(64)

    // train the model
    val model = word2Vec.fit(documentDF)

    val result = model.transform(lines.filter(_.length>0).flatMap(_.split(" ")).distinct().map(Array(_)).toDF("text"))
    //    result.select("result").take(5).foreach(println)

    val myWord = "Hermione"
    val coords = model.transform(
      ss.sqlContext.createDataFrame(Seq(myWord)
      .map(Array(_)).map(Tuple1.apply)).toDF("text")).collect()
    val myWordVec = coords(0).get(1).asInstanceOf[DenseVector].values



    result
      .collect()
      .map(r => {
        (r.get(0).asInstanceOf[mutable.WrappedArray[String]], r.get(1).asInstanceOf[DenseVector].values)
      })
      .map(r => (r._1.asInstanceOf[mutable.WrappedArray[String]].reduce(_ + " " + _), distVec(r._2, myWordVec)))
      .sortBy[Double](r => r._2)
      .take(100)
      .foreach(println)


  }
}
