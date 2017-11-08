package wikipedia

import scala.util.matching.Regex
import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

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
	 * @param title the page title (meaning the starting edge)
	 * @param bob   the raw text to parse in order to extract references to other pages
	 * @return Array[(String,String)] of start_edge(title) -> end_edge(referenced page)
	 */
	def PageRank(title: String, bob : Graph[String, Long]): Unit = {
			// Run PageRank
			val ranks = bob.pageRank(0.0001).vertices
	}
}