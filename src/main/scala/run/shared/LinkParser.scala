package run.shared

import scala.util.matching.Regex
import scala.xml.XML
import java.io._

object LinkParser{
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