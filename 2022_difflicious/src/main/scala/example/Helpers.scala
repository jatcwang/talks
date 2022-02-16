package example

import difflicious.DiffResult

object Helpers {
  def printHtml(diffResult: DiffResult) = {

    val RED = "\u001b[31m"
    val GREEN = "\u001b[32m"
    val GRAY = "\u001b[90m"
    val RESET = "\u001b[39m"
    difflicious.DiffResultPrinter
      .consoleOutput(diffResult, 0)
      .render
      .replace(RED, """<span class="red">""")
      .replace(GREEN, """<span class="green">""")
      .replace(GRAY, """<span class="gray">""")
      .replace(RESET, "</span>")
  }
}
