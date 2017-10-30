package run.local
import scala.util.matching.Regex
import scala.io.Source
import scala.xml.XML
import java.io._


object LinkParser {
	def main(args: Array[String]): Unit = {
			// FileWriter buffer opening
			val vertices = Word2VecLocalExample.getClass.getClassLoader.getResource("vertices.txt").getPath
					val edges = Word2VecLocalExample.getClass.getClassLoader.getResource("edges.txt").getPath
					val bvert = new BufferedWriter(new FileWriter(new File(vertices)))
					val bedges = new BufferedWriter(new FileWriter(new File(edges)))

					//			We use here an extract of the wikipedia dump
					var fileXMLPath = Word2VecLocalExample.getClass.getClassLoader.getResource("xml.txt").getPath

					//					We load the file and create an Array of all "page" markup 
					val xml = ( XML.loadFile(fileXMLPath) \\ "page")

					//	  We set a regex for parsing all [[]] sub strings
					val keyValPattern: Regex = "\\[\\[(.+?)\\]\\]".r

					xml.foreach{ n => 
					//ns is 0 if it is a page (i.e not a disucssion, forum etc..)
					val ns  = (n \ "ns").text
					if(ns == "0"){
						val title  = (n \ "title").text
								bvert.write(s"$title")
								bvert.newLine()
								for (e <- (n \\ "revision" \ "text").text.split('\n')){
									for (patternMatch <- keyValPattern.findAllMatchIn(e)){
										var link = patternMatch.group(1)
												if (!link.contains(":")) {
													var out = link
															out = out.split("#")(0)
															out = out.split("\\|")(0)
															bedges.write(s"$title; $out")
															bedges.newLine()
												}
									}
								}
					}
					}
					bedges.close()
					bvert.close()
	}
}
