package wikipedia

import org.apache.spark.ml.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import tool.VectorMath
import org.apache.spark.sql.SaveMode

import scala.collection.mutable

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
			model.getVectors.rdd.saveAsTextFile(s"D:/Bureau/bob")
			model
	}
	/**Get vocabulary from  a Word2VecModel 
	 * @param mod Word2VecModel from a previous Word2Vec learning
	 * @param ss the current Spark Session
	 * @return RDD[String] a list of the vocabulary
	 */
	def getVocabulary(mod : Word2VecModel,ss : SparkSession): RDD[String]  ={
			mod.getVectors
			.select("word")
			.rdd.map(x=>x.get(0).asInstanceOf[String])
	}  
	/**Sum two word vectors and get the resulting vector as a list of the closest words
	 * @param mod Word2VecModel from WordEmbedding learning
	 * @param ss the current Spark Session
	 * @param word1 the first word to sum
	 * @param word2 the second, if minus is true, word2 vector representation is inverted
	 * @param num_results the number of closest words to return
	 * @param minus if true, the word2 is substracted to the first
	 * @return RDD[String] list of the closest words to the sum vector (the first being the closest)
	 */
	def sumWords(mod : Word2VecModel,ss : SparkSession,word1 : String,word2 : String, num_results : Int, minus : Boolean): Array[String]  ={
			import ss.sqlContext.implicits._
			var space = mod.getVectors
			val w1 = space.filter($"word"===word1.toLowerCase())
			.first()(1)
			.asInstanceOf[DenseVector].values
				var w2 = space.filter($"word"===word2.toLowerCase())
			.first()(1)
			.asInstanceOf[DenseVector].values
			if(minus){
				w2 = VectorMath.opposite(w2)
			}
			var vec_result = new DenseVector(VectorMath.addVec(w1, w2))
			
			var bob = mod.findSynonyms(vec_result,num_results)
			bob.foreach(x=>println(x(0)))
			bob.map(x=>x(0).asInstanceOf[String]).collect()
	}  
}
