package wikipedia

object TextCleaner {

	/**
	 * Clean a wikipedia page from Markdown.
	 * <b> Do not use on multiple pages, or fraction of one page ! </b>
	 * @param text the text of one wikipedia page
	 * @return the input text, cleaned from markdown
	 */
	def cleanPage(text: String): String = {
			var output = text
					try
			{
						var index_start_tag = output.indexOf("<")
						var eq = false
						if(index_start_tag != -1){
									eq = true
								}
						while(eq){
							var index_end_tag = output.indexOf(">",index_start_tag+1)
									//        println(index_end_tag)
									var label_tag = "/"+output.substring(index_start_tag+1,index_end_tag)
//									        println(label_tag)
									var index_closing_tag = output.indexOf(label_tag,index_end_tag)
									if(index_closing_tag == -1){
										output = output.substring(0,index_start_tag-1) + output.substring(index_end_tag+1)
									}else{
									//        println(index_closing_tag)
									var out = index_closing_tag+label_tag.length+1
									//        println(output.substring(index_start_tag,out))
									output = output.substring(0,index_start_tag-1) + output.substring(out+1)
									//       println(output)
									}
									index_start_tag = output.indexOf("<")
									//       println(index_start_tag)
									if(index_start_tag == -1){
										eq = false
									}
						}
//						println(output)
			} 
			catch
			{
			case e: Throwable => e.printStackTrace
			}
			//[[info]] ([move info and links]) ==> [[info]]
			output = output.replaceAll("(\\[\\[.*?\\]\\]).*?\\(\\[.*?\\]\\)", "$1")

					// remove headers
					output = output.replaceAll("=+\\h*(.*)\\h=+", "")

					// remove '' and ''' patterns
					output = output.replaceAll("'{2,3}", "")

					// remove list markup  : *
					output = output.replaceAll("\\n\\* ", "\n")

					// remove \n
					output = output.replaceAll("\n", " ")

					// preserve date
					output = output.replaceAll("\\{\\{Date.*?\\|(.*?)\\}\\}", "$1")

					// remove the rest of {{shit...{{other nested shit ...}} ...}} (we iterate max = 10 times) :
					0.to(10).foreach(_ => {
						output = output.replaceAll("\\{\\{[^{}]*\\}\\}", "")
					})

					// remove ref
					output = output.replaceAll("&lt;ref&gt;.*?&lt;\\/ref&gt;", "")
					//    output = output.replaceAll("<ref>.*?<\\/ref>", "")

					// [[info (more)]] ==> [[info]]
					output = output.replaceAll("(\\[\\[[^\\]]*?)\\([^\\]]*?\\)(.*?\\]\\])", "$1$2")

					// [[a|b]] ==> [[b]]
					output = output.replaceAll("\\[\\[[^\\]]+?\\|(.+?)\\]\\]", "[[$1]]")

					// remove anoying [[Field:Value]
					output = output.replaceAll("\\[\\[[^]]*?:[^]]*?\\]\\]", "")

					// [[b]] ==> b
					output = output.replaceAll("\\[\\[(.+?)\\]\\]", "$1")



					// remove special characters
					output = output.replaceAll("[,;:(|)]"," ")

					// reduce multiple spaces to 1 space
					output = output.replaceAll(" {2,}"," ")

					// delete multiple dots : . .
					output = output.replaceAll("\\. \\.",".")

					//    replace diagonal quotes
					output = output.replaceAll("\\’","\\'")
					output

	}

}
