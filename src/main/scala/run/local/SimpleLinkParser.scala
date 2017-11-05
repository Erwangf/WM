package run.local
import run.shared.linkParser

object SimpleLinkParser {


  def main(args: Array[String]): Unit = {

    //	We use here an extract of the wikipedia dump
    var fileXMLPath = Word2VecLocalExample.getClass.getClassLoader.getResource("xml.txt").getPath

    // We create the LinkParser
    val linkParser = new LinkParser()

    // Load our XML file
    linkParser.simpleXMLParser(fileXMLPath)

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
