package no.eliashaugsbakk.clams.server.utils;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class MarkdownConverter {
  public static String convertToHtml(String markdown) {
    Parser parser = Parser.builder().build();
    HtmlRenderer renderer = HtmlRenderer.builder().escapeHtml(true).build();

    Node document = parser.parse(markdown);
    return renderer.render(document);
  }
}
