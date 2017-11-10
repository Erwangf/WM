package run.shared

import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.SparkSession
import tool.VectorMath

import scala.collection.mutable

object Word2VecExample {

  def runWord2Vec(ss: SparkSession,targetWord:String,filePath:String): Array[(String,Double)] ={

    import ss.implicits._
    val lines = ss.sparkContext.textFile(filePath,64)
    val documentDF = lines.filter(_.length>0).map(_.split(" ")).toDF("text")

    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(300)
      .setWindowSize(10)
      .setMaxIter(3)
      .setNumPartitions(32)

    // train the model
    val model = word2Vec.fit(documentDF)

    // compute words of the corpus
    var tempTT2 = lines.filter(_.length>0).flatMap(_.split(" "))
    
    var tempTT =tempTT2.distinct().map(Array(_)).toDF("text")
    val result = model.transform(tempTT)


    val coords = model.transform(
      ss.sqlContext.createDataFrame(Seq(targetWord)
        .map(Array(_)).map(Tuple1.apply)).toDF("text")).collect()
    val myWordVec = coords(0).get(1).asInstanceOf[DenseVector].values

    result
      .collect()
      .map(r => (r.get(0).asInstanceOf[mutable.WrappedArray[String]], r.get(1).asInstanceOf[DenseVector].values))
      .map(r => (r._1.asInstanceOf[mutable.WrappedArray[String]].reduce(_ + " " + _), VectorMath.distVec(r._2, myWordVec)))
      .sortBy[Double](r => r._2)
      .take(20)


  }

}
