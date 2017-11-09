package wikipedia

import scala.util.matching.Regex
import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.rdd.MLPairRDDFunctions.fromPairRDD

object GraphOperator {
	/** This is a public function to be used, no object creation needed
	 * take a title and a row string to create an Array of vertices
	 *
	 * @param title the page title (meaning the starting edge)
	 * @param bob   the raw text to parse in order to extract references to other pages
	 * @return Array[(String,String)] of start_edge(title) -> end_edge(referenced page)
	 */
	def LinkParser(title: String, bob: String): Array[(String, String)] = {
			val keyValPattern: Regex = "\\[\\[(.+?)\\]\\]".r
					var out_final: Array[(String, String)] = Array()
					if (bob != null && title != null) {
						for (e <- bob.split('\n')) {
							for (patternMatch <- keyValPattern.findAllMatchIn(e)) {
								val link = patternMatch.group(1)
										if (!link.contains(":")) {
											// Sometimes, link.split("#")(0).split("\\|") is empty causing an ArrayIndexOutOfBoundsException
											if (link.split("#").length != 0 && link.split("#")(0).split("\\|").length != 0) {
												out_final = out_final :+ (title, link.split("#")(0).split("\\|")(0))
											}
										}
							}
						}
					}
	out_final
	}
	/**
	 * @author eric-kimbrel
	 *         https://gist.github.com/eric-kimbrel/01ab2f97c4438ba7bd9a
	 *         Converts an RDD[(String,String)] into Graph[String,Long], all edge weights are set to 1L
	 *         Performs joins to associate edges with Long Id's.  May be slow but will support large data sets
	 * @param data simple edge list
	 * @return Graph[String,Long] with all edge weights 1L
	 */
	def unweightedStringEdgeListViaJoin(data: RDD[(String, String)]): Graph[String, Long] = {

			data.cache()

			val _flat = data
			.flatMap({ case (src, dst) => Seq((src, 0), (dst, 0)) })
			.reduceByKey(_ + _)
			.map({ case (name, count) => name })
			.cache()
			_flat.count()
			// IMPORTANT:  you must materialize the rdd prior to using zipWithUniqueId or zipWithIndex
			//  if you do not do this step duplicate ids can be generated.
			val flat = _flat
			.zipWithUniqueId()
			.cache()

			val srcEncoded = data.join(flat).map({ case (srcStr, (dstStr, srcLong)) => (dstStr, srcLong) })
			val edges = srcEncoded.join(flat).map({ case (dstStr, (srcLong, dstLong)) => Edge(srcLong, dstLong, 1L) }).cache()
			val vertices = flat.map({ case (name, id) => (id, name) }).cache()

			println("edges: " + edges.count())
			println("vertices: " + vertices.count())
			data.unpersist()
			flat.unpersist()
			_flat.unpersist()

			Graph(vertices, edges).partitionBy(PartitionStrategy.EdgePartition2D)
	}
	/** This is a public function to be used, no object creation neede
	 * take a title and a row string to create an Array of vertices
	 *
	 * @param bob   the graph
	 * @param sc	SparkContext
	 * @return Array[Long] ID with highest pageRank values and their neighboors with highest pagerank value (10x10)
	 */
	def pageRanker(bob : Graph[String, Long], sc : SparkContext):  Array[Long] = {
			//	  Constant
			val direction: EdgeDirection=EdgeDirection.Either
					// Run PageRank  
					val ranks = bob.pageRank(0.0001).vertices
					//					On garde slmnt les + importants (les 10)
					val mostImp = ranks
					.takeOrdered(10)(Ordering[Double]
							.reverse.on { x => x._2})
					.map(x => x._1)

					var best_graph =sc.parallelize(bob.collectNeighborIds(direction).filter(x=>mostImp contains x._1)
							.map(x =>  x._2.map(i => (x._1,i)))
							.flatMap(x => x).collect())
							
					var	best_graph2 = best_graph
					.map(_.swap).join(ranks).map {
					case (nei, (src, rank)) => (src,(nei,rank))
	        }
	        .groupByKey()
	        
	        var out1 = best_graph2
	        .map(x => (x._1,x._2.toList.sortBy(_._2).take(10)))
	        .flatMap{
	          case (id,arr) => arr.map(i => (id,i._1))
	          }
//	        
	        
	        var out = out1.collect().map(x => x._2)
	        .union(
	            out1.collect().map(x => x._1)
	            ).distinct
	        out
	}
}