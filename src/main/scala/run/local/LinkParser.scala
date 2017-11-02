package run.local

import scala.util.matching.Regex
import scala.xml.XML
import java.io._


class LinkParser {

  private var vertices: Array[String] = Array()
  private var edges: Array[(String, String)] = Array()

  /**
    * Load an XML File, parse wikipedia, and extract 2 arrays, vertices and edges
    * @param xmlPath The file to parse
    */
  def loadXMLFile(xmlPath: String): Unit = {

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

}


object LinkParser {


  def main(args: Array[String]): Unit = {

    //	We use here an extract of the wikipedia dump
    var fileXMLPath = Word2VecLocalExample.getClass.getClassLoader.getResource("xml.txt").getPath

    // We create the LinkParser
    val linkParser = new LinkParser()

    // Load our XML file
    linkParser.loadXMLFile(fileXMLPath)

    // print some informations about the edges and vertices
    println(s"loaded ${linkParser.getEdges.length} edges")
    println(s"loaded ${linkParser.getVertices.length} vertices")

    // then save our data to disk
    val verticesPath = Word2VecLocalExample.getClass.getClassLoader.getResource("vertices.txt").getPath
    val edgesPath = Word2VecLocalExample.getClass.getClassLoader.getResource("edges.txt").getPath

    linkParser.printVerticesAndEdges(verticesPath, edgesPath)

    println("Edges and vertices saved to edges.txt and vertices.txt")


  }


}
