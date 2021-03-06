package wikipedia

import scala.util.matching.Regex
import org.apache.spark.{HashPartitioner, SparkContext}
import org.apache.spark.SparkContext._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.rdd.MLPairRDDFunctions.fromPairRDD

object GraphOperator {
  /** Take a title and a row string to create an Array of vertices
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
    * @param data simple edge list (by default it's directed
    * @return Graph[String,Long] Return a GraphX's graph. See GarphX doc for further information
    */
  def unweightedStringEdgeListViaJoin(data: RDD[(String, String)]): Graph[String, Long] = {

    data.cache()

    val _flat = data
      .flatMap({ case (src, dst) => Seq((src, 0), (dst, 0)) })
      .reduceByKey(_ + _)
      .map({ case (name, _) => name })
      .cache()
    _flat.count()
    // IMPORTANT:  you must materialize the rdd prior to using zipWithUniqueId or zipWithIndex
    //  if you do not do this step duplicate ids can be generated.
    val flat = _flat
      .zipWithUniqueId()
      .cache()

    val srcEncoded = data.join(flat).map({ case (_, (dstStr, srcLong)) => (dstStr, srcLong) })
    val edges = srcEncoded.join(flat).map({ case (_, (srcLong, dstLong)) => Edge(srcLong, dstLong, 1L) }).cache()
    val vertices = flat.map({ case (name, id) => (id, name) }).cache()

    println("edges: " + edges.count())
    println("vertices: " + vertices.count())
    data.unpersist()
    flat.unpersist()
    _flat.unpersist()

    Graph(vertices, edges).partitionBy(PartitionStrategy.EdgePartition2D)
  }

  /** Perform a pagerank on the graph then
   *  Return the top 10 vertices and their ten neighbor with the highest pageRank
    * @param thegraph the graph
    * @param sc  current SparkContext
    * @return Array[Long] ID of the top 10 vertices and their ten neighbor with the highest pageRank (max 100)
    */
  def pageRanker(thegraph: Graph[String, Long], sc: SparkContext): Array[Long] = {
    //	  Constant
    val direction: EdgeDirection = EdgeDirection.Either
    // Run PageRank
    val ranks = thegraph.pageRank(0.0001).vertices
    //					On garde slmnt les + importants (les 10)
    val mostImp = ranks
      .takeOrdered(10)(Ordering[Double].reverse.on { x => x._2 })
      .map(x => x._1)

    val best_graph = sc.parallelize(thegraph.collectNeighborIds(direction).filter(x => mostImp contains x._1)
      .map(x => x._2.map(i => (x._1, i)))
      .flatMap(x => x).collect())

    val best_graph2 = best_graph
      .map(_.swap).join(ranks).map {
      case (nei, (src, rank)) => (src, (nei, rank))
    }
      .groupByKey()

    val out1 = best_graph2
      .map(x => (x._1, x._2.toList.sortBy(_._2).take(10)))
      .flatMap {
        case (id, arr) => arr.map(i => (id, i._1))
      }
    //

    val out = out1.flatMap(x => Array(x._1, x._2)).distinct
    out.collect()
  }

  /** Community counter
   *  Return the number of partition without edges between them
    * @param thegraph the graph
    * @param sc  SparkContext
    * @return Long number of communities
    */
  def communityCounter(thegraph: Graph[String, Long], sc: SparkContext): Long = {
    val cc = thegraph.connectedComponents().vertices
    var bib = cc.map(x => x._2).distinct()
    bib.count()
  }
}