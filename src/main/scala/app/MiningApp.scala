package app

import org.apache.spark.graphx.Graph
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import run.local.WikiDumpImport

import scala.collection.mutable

object MiningApp {

  var pages : DataFrame = _
  var graph : Graph[String,Long] = _

  var ss : SparkSession = _

  def init(session:SparkSession): Unit ={
    ss = session
  }
  def importDump(filePath:String): Unit ={
    pages = WikiDumpImport.importDumpAndGetDF(filePath,ss)
  }

  case class Page(title:String,text:String,edges:Array[(String,String)])
  def getPage(pageName:String) : Page = {
    val row = pages.select("title","text","edges")
      .filter("title = \""+pageName+"\"").first()
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
