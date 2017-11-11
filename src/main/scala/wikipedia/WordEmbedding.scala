package wikipedia

import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.feature.Word2VecModel
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.sql.SparkSession
import tool.VectorMath

import scala.collection.mutable
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.functions._
import wikipedia._

import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import scala.collection.mutable
import scala.util.matching.Regex
import org.apache.spark.sql.types._

object WordEmbedding {
	/**Perform a word embedding on text dataframe and return the model
	 * @param df a DataFrame containing a "text" collumns with raw text, it is splitted by sentence
	 * @param dimension the dimension of the embedded space
	 * @param window size of the windows to define the neighborhood of a word
	 * @param iteration number of iteration
	 * @return Word2VecModel Model from WordEmbedding learning
	 * (Model.getVectors() Returns a dataframe with two fields, "word" and "vector", 
	 * with "word" being a String and "vector" the vector the DenseVector that it is mapped to.)
	 */

	def runWord2Vec(ss: SparkSession,df: DataFrame, dimension : Int, window : Int, iteration : Int):  Word2VecModel ={

			import ss.sqlContext.implicits._
			var raw_sent =  df.select("text").rdd
			.map(x=>x.get(0).asInstanceOf[String])
			.flatMap{
			case(text) => text.split("\\.")
			}.filter(_.length>0)

			var documentDF = raw_sent.map(w=>w.trim).map(_.split("[ ']").filter(_.length()>0).map(t=>t.toLowerCase())).toDF("text")
			documentDF.select("text").show()

			val word2Vec = new Word2Vec()
			.setInputCol("text")
			.setOutputCol("result")
			.setVectorSize(dimension)
			.setWindowSize(window)
			.setMaxIter(iteration)
			.setNumPartitions(32)
			.setMinCount(0)

			// train the model
			val model = word2Vec.fit(documentDF)

			//			Extract Vocabulary 
			var vocab =  raw_sent.flatMap(_.split("[ ']").filter(_.length()>0).map(t=>t.toLowerCase())).distinct().map(Array(_)).toDF("text")
			vocab.select("text")
			.map(x=> x.get(0).asInstanceOf[mutable.WrappedArray[String]](0))
			.foreach(println(_))
			// compute words of the corpus
			val result = model.transform(vocab)
			model
			//			val synonyms = model.findSynonyms("adolfo", 10)
			//			val synonyms = model.findSynonyms("publiés", 5)
			//			synonyms.select(synonyms.columns(0)).map(x=>x.get(0).asInstanceOf[String]).foreach(println(_))
			//
			//			val coords = model.transform(
			//					ss.sqlContext.createDataFrame(Seq(targetWord)
			//							.map(Array(_)).map(Tuple1.apply)).toDF("text")).collect()
			//			val myWordVec = coords(0).get(1).asInstanceOf[DenseVector].values
			//
			//			result
			//			.collect()
			//			.map(r => (r.get(0).asInstanceOf[mutable.WrappedArray[String]], r.get(1).asInstanceOf[DenseVector].values))
			//			.map(r => (r._1.asInstanceOf[mutable.WrappedArray[String]].reduce(_ + " " + _), VectorMath.distVec(r._2, myWordVec)))
			//			.sortBy[Double](r => r._2)
			//			.take(20)


	}
	/**Get vocabulary from  Word2VecModel 
	 * @param mod Word2VecModel from WordEmbedding learning
	 * @param ss the current Spark Session
	 * @return RDD[String]
	 */
	def getVocabulary(mod : Word2VecModel,ss : SparkSession): RDD[String]  ={

			import ss.sqlContext.implicits._
			mod.getVectors
			.select("word")
			.rdd.map(x=>x.get(0).asInstanceOf[String])
	}  
		/**Sum two word vectors and get the resulting vector
	 * @param mod Word2VecModel from WordEmbedding learning
	 * @param ss the current Spark Session
	 * @return RDD[String]
	 */
	def sumWords(mod : Word2VecModel,ss : SparkSession,word1 : String,word2 : String): String  ={
			import ss.sqlContext.implicits._
			var space = mod.getVectors
			var v1 = space.filter(space("word")===word1).first().asInstanceOf[(String,Array[Float])]
			var v2 = space.filter(space("word")===word2).first().asInstanceOf[(String,Array[Float])]
//			.select("word")
//			.rdd.map(x=>x.get(0).asInstanceOf[String])
						"nn"
	}  
}
