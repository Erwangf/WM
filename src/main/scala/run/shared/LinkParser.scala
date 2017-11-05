package run.shared

import scala.util.matching.Regex
import scala.xml.XML
import java.io._
import run.local.Word2VecLocalExample


class LinkParser {

  private var vertices: Array[String] = Array()
  private var edges: Array[(String, String)] = Array()

  /**
    * Load an XML File, parse wikipedia, and extract 2 arrays, vertices and edges
    * @param xmlPath The file to parse
    */
  def simpleXMLParser(xmlPath: String): Unit = {

    //	We load the file and create an Array of all "page" markup
    val xml = XML.loadFile(xmlPath) \\ "page"

    //	We set a regex for parsing all [[]] sub strings
    val keyValPattern: Regex = "\\[\\[(.+?)\\]\\]".r

    xml.foreach { n =>
      //ns is 0 if it is a page (i.e not a disucssion, forum etc..)
      val ns = (n \ "ns").text
      if (ns == "0") {
        val title = (n \ "title").text
        vertices = vertices :+ title

        for (e <- (n \\ "revision" \ "text").text.split('\n')) {
          for (patternMatch <- keyValPattern.findAllMatchIn(e)) {
            val link = patternMatch.group(1)
            if (!link.contains(":")) {
              var out = link
              out = out.split("#")(0)
              out = out.split("\\|")(0)
              edges = edges :+ (title, out)
            }
          }
        }
      }
    }

  }
  /**
    * @return an Array of (origin,destination) tuples
    */
  def getEdges: Array[(String, String)] = edges

  /**
    * @return an Array of vertices
    */
  def getVertices: Array[String] = vertices

  /**
    * In case you want to reset the link parser and clear the data
    */
  def clearData(): Unit = {
    vertices = Array()
    edges = Array()
  }

  /**
    * print the vertices and the edges into 2 files
    *
    * @param verticesPath the file path for the vertices
    * @param edgesPath    the file path for the edges
    */
  def printVerticesAndEdges(verticesPath: String, edgesPath: String): Unit = {


    val bvert = new BufferedWriter(new FileWriter(new File(verticesPath)))
    val bedges = new BufferedWriter(new FileWriter(new File(edgesPath)))

    vertices.foreach(v => bvert.write(v + "\n"))
    edges.foreach(e => bedges.write(e._1 + "; " + e._2 + "\n"))

    bedges.close()
    bvert.close()
  }
    /**THis is a public function to be used, no object creation needed
    * take a title and a row string to create an Array of vertices
    *
    * @param title the page title (meaning the starting edge)
    * @param bob   the raw text to parse in order to extract references to other pages
    * @return Array[(String,String)] of start_edge(title) -> end_edge(referenced page)
    */
 def externalParser(title: String, bob: String): Array[(String, String)] = {
    val keyValPattern: Regex = "\\[\\[(.+?)\\]\\]".r
    var out_final: Array[(String, String)] = Array()
    for (e <- bob.split('\n')) {
      for (patternMatch <- keyValPattern.findAllMatchIn(e)) {
        val link = patternMatch.group(1)
        if (!link.contains(":")) {
          // Sometimes, link.split("#")(0).split("\\|") is empty causing an ArrayIndexOutOfBoundsException
          if(link.split("#").length!=0 && link.split("#")(0).split("\\|").length!=0){
            out_final = out_final :+ (title, link.split("#")(0).split("\\|")(0))
          }
        }
      }
    }
    out_final
  }
}