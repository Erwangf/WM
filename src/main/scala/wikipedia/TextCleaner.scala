package wikipedia

object TextCleaner {

  /**
    * Clean a wikipedia page from Markdown.
    * <b> Do not use on multiple pages, or fraction of one page ! </b>
    * @param text the text of one wikipedia page
    * @return
    */
  def cleanPage(text: String): String = {
    var output = text

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

    // remove the rest of {{shit...{{other nested shit ...}} ...}} (we iterate max = 10 times) :
    0.to(10).foreach(_ => {
      output = output.replaceAll("\\{\\{[^{}]*\\}\\}", "")
    })

    // [[info (more)]] ==> [[info]]
    output = output.replaceAll("(\\[\\[[^\\]]*?)\\([^\\]]*?\\)(.*?\\]\\])", "$1$2")

    // [[a|b]] ==> [[b]]
    output = output.replaceAll("\\[\\[[^\\]]+?\\|(.+?)\\]\\]", "[[$1]]")

    // remove anoying [[Field:Value]
    output = output.replaceAll("\\[\\[[^]]*?:[^]]*?\\]\\]", "")

    // [[b]] ==> b
    output = output.replaceAll("\\[\\[(.+?)\\]\\]", "$1")

    // remove ref
    output = output.replaceAll("&lt;ref&gt;.*?&lt;\\/ref&gt;", "")

    // remove special characters
    output = output.replaceAll("[,;:(|)]"," ")

    // reduce multiple spaces to 1 space
    output = output.replaceAll(" {2,}"," ")

    // delete multiple dots : . .
    output = output.replaceAll("\\. \\.",".")

    // delete numbers
    output = output.replaceAll("[0-9]","")

    output

  }

}
