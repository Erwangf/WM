package app

import java.io.FileNotFoundException
import java.io.File
import java.nio.file.{Files, Paths}

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.graphx._
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}
import run.local.WikiDumpImport
import wikipedia.GraphOperator
import  wikipedia.WordEmbedding
import org.apache.spark.ml.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.ml.linalg.DenseVector
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, SparkSession}
import tool.VectorMath
import org.apache.spark.sql.SaveMode

import scala.collection.mutable

object Status extends Enumeration {
	val AVAILABLE, RUNNING = Value
}

object MiningApp {

	private final val LOCAL_PAGES_PATH = "./tmp/local_pages_path.parquet"
			private final val LOCAL_GRAPH_VERTICES_PATH = "./tmp/local_graph_vertices_path.save"
			private final val LOCAL_GRAPH_EDGES_PATH = "./tmp/local_graph_edges_path.save"

			private var pages: DataFrame = _
			private var graph: Graph[String, Long] = _
			private var embedded_space: Word2VecModel = _
			private var ss: SparkSession = _
			private var status: Status.Value = Status.AVAILABLE
			private var loadedFile : String = "NA"

			import org.apache.hadoop.fs.FileSystem

			private val hdfs: FileSystem = FileSystem.get(new Configuration())

			def init(session: SparkSession): Unit = {
					ss = session
	}

	def getStatus: Status.Value = status
			def getLoadedFile(): String =  loadedFile

			@throws(classOf[FileNotFoundException])
			private def importPages(): Unit = {
					if (Files.exists(Paths.get(LOCAL_PAGES_PATH))) {
						pages = ss.read.parquet(LOCAL_PAGES_PATH)
					}
					else {
						throw new FileNotFoundException()
					}

			}

			def exportPages(): Unit = {
					pages.write.mode(SaveMode.Overwrite).parquet(LOCAL_PAGES_PATH)
			}

			def exportGraph(): Unit = {

					if (Files.exists(Paths.get(LOCAL_GRAPH_VERTICES_PATH))) {
						hdfs.delete(new Path(LOCAL_GRAPH_VERTICES_PATH), true)
					}

					if (Files.exists(Paths.get(LOCAL_GRAPH_EDGES_PATH))) {
						hdfs.delete(new Path(LOCAL_GRAPH_EDGES_PATH), true)
					}

					graph.vertices.saveAsObjectFile(LOCAL_GRAPH_VERTICES_PATH)
					graph.edges.saveAsObjectFile(LOCAL_GRAPH_EDGES_PATH)
			}

			def clearLocal(): Unit = {
					if (Files.exists(Paths.get(LOCAL_GRAPH_VERTICES_PATH))) {
						hdfs.delete(new Path(LOCAL_GRAPH_VERTICES_PATH), true)
					}
					if (Files.exists(Paths.get(LOCAL_GRAPH_EDGES_PATH))) {
						hdfs.delete(new Path(LOCAL_GRAPH_EDGES_PATH), true)
					}
					if (Files.exists(Paths.get(LOCAL_PAGES_PATH))) {
						hdfs.delete(new Path(LOCAL_PAGES_PATH), true)
					}

			}

			@throws(classOf[FileNotFoundException])
			private def importGraph(): Unit = {
					if (Files.exists(Paths.get(LOCAL_GRAPH_VERTICES_PATH)) && Files.exists(Paths.get(LOCAL_GRAPH_EDGES_PATH))) {
						val vertices = ss.sparkContext.objectFile[(VertexId, String)](LOCAL_GRAPH_VERTICES_PATH)
								val edges = ss.sparkContext.objectFile[Edge[Long]](LOCAL_GRAPH_EDGES_PATH)
								graph = Graph[String, Long](vertices, edges)
					}
					else throw new FileNotFoundException()
			}

			@throws(classOf[FileNotFoundException])
			def importLocal(): Unit = {
					status = Status.RUNNING
							new Thread(new Runnable {
								override def run(): Unit = {
										importGraph()
										importPages()
										status = Status.AVAILABLE
										loadedFile = "localStoredDF"
								}
							}).start()

			}

			def importWikiDumpInBackground(filePath: String): Unit = {
					status = Status.RUNNING
							new Thread(new Runnable {
								override def run(): Unit = {
										val result = WikiDumpImport.importDumpAndGetDF(filePath, ss)
												pages = result._1
												graph = result._2

												exportPages()
												exportGraph()
												status = Status.AVAILABLE
												loadedFile = (new File(filePath)).getName()
								}
							}).start()

			}
			def wordEmbedding(dimension : Int, window : Int, iteration : Int): Unit = {	
					status = Status.RUNNING
							new Thread(new Runnable {
								override def run(): Unit = {
										embedded_space = WordEmbedding.runWord2Vec(ss,pages,dimension,window,iteration)
										status = Status.AVAILABLE
								}
							}).start()

			}
			def pagesLoaded(): Boolean = {
					pages != null
			}
case class Page(title: String, text: String, edges: Array[(String, String)])

def getPage(pageName: String): Page = {
		val rows = pages.select("title", "text", "edges")
				.filter("title = \"" + pageName + "\"").take(1)
				if (rows.length != 1) return null

						val row = rows(0)

						val edges = row.get(2)
						.asInstanceOf[mutable.WrappedArray[Row]]
								.toArray[Row]
										.map(r => (r.get(0).asInstanceOf[String], r.get(1).asInstanceOf[String]))


										Page(row.get(0).asInstanceOf[String], row.get(1).asInstanceOf[String], edges)
			}

			def pageCount(): Long = {
					pages.count()
			}

case class EdgesAndVertices(edges: Array[EdgeLight], vertices: Array[VertexLight])

case class EdgeLight(from: Long, to: Long)

case class VertexLight(id: Long, label: String)

def getBestPageRankGraph: EdgesAndVertices = {
		var ind = GraphOperator.pageRanker(graph, ss.sparkContext)
				val bestGraph = graph.subgraph(_ => true, (a, _) => ind contains a)

				EdgesAndVertices(
						bestGraph.edges.collect().map(e => EdgeLight(e.srcId.toLong, e.dstId.toLong)),
						bestGraph.vertices.collect().map(v => VertexLight(v._1.toLong, v._2))
						)
			}


}
