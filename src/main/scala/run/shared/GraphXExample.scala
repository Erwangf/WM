package run.shared

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.log4j.Logger
import org.apache.log4j.Level

object GraphXExample {


  def runGraphXExample(ss: SparkSession): Unit = {


    Logger.getLogger("org").setLevel(Level.OFF)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val sc = ss.sparkContext
    val users: RDD[(VertexId, (String, String))] = sc.parallelize(Array(
      (1L, ("Erwan GF", "student")),
      (2L, ("Antoine G.", "student")),
      (3L, ("Julien J.", "prof")),
      (4L, ("Margot S.", "phd-student"))
    ))

    // Create an RDD for edges
    val relationships: RDD[Edge[String]] = sc.parallelize(Array(
      Edge(1L, 2L, "collab"),
      Edge(3L, 1L, "teach"),
      Edge(3L, 2L, "teach"),
      Edge(3L, 4L, "supervise")
    ))
    // Define a default user in case there are relationship with missing user
    val defaultUser = ("DEFAULT_USER", "MISSING")

    // Build the initial Graph
    val graph = Graph(users, relationships, defaultUser)

    println("Edges :")
    graph.edges.foreach(println)

    println("Vertices :")
    graph.vertices.foreach(println)

    println("Triplets :")
    graph.triplets.foreach(println)

    println("About Erwan :")
    val idErwan = graph.vertices.filter(_._2._1 == "Erwan GF").collect()(0)._1
    graph.triplets
      .filter(r => r.dstId == idErwan || r.srcId == idErwan)
      .foreach(r => {
        println(s"${r.srcAttr._1}--(${r.attr})--${r.dstAttr._1}")
      })
  }

}
