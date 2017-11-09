package app

import java.io.FileNotFoundException
import java.nio.file.{Files, Paths}

import org.apache.spark.graphx.Graph
import org.apache.spark.sql.{DataFrame, Row, SaveMode, SparkSession}
import run.local.WikiDumpImport

import scala.collection.mutable

object MiningApp {

  final val LOCAL_PAGES_PATH = "d:\\local_pages_path.parquet"

  var pages : DataFrame = _
  var graph : Graph[String,Long] = _

  var ss : SparkSession = _

  def init(session:SparkSession): Unit ={
    ss = session
  }

  @throws(classOf[FileNotFoundException])
  def importPages(): Unit ={
    if(Files.exists(Paths.get(LOCAL_PAGES_PATH))){
      pages = ss.read.parquet(LOCAL_PAGES_PATH)
    }
    else {
      throw new FileNotFoundException()
    }

  }

  def exportPages(): Unit ={
    pages.write.mode(SaveMode.Overwrite).parquet(LOCAL_PAGES_PATH)
  }

  def importWikiDump(filePath:String): Unit ={
    pages = WikiDumpImport.importDumpAndGetDF(filePath,ss)
    exportPages()
  }

  case class Page(title:String,text:String,edges:Array[(String,String)])

  def getPage(pageName:String) : Page = {
    val rows = pages.select("title","text","edges")
      .filter("title = \""+pageName+"\"").take(1)
    if(rows.length!=1) return null

    val row = rows(0)

    val edges = row.get(2)
      .asInstanceOf[mutable.WrappedArray[Row]]
      .toArray[Row]
      .map(r=>(r.get(0).asInstanceOf[String],r.get(1).asInstanceOf[String]))


    Page(row.get(0).asInstanceOf[String],row.get(1).asInstanceOf[String],edges)
  }

  def pageCount() : Long = {
    pages.count()
  }



}
