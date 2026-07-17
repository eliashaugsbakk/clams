package no.eliashaugsbakk.clams.server.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepo;

public class SlugService {
  private final BlogPostRepo blogPostRepo;
  private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
  private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

  public SlugService(BlogPostRepo blogPostRepo) {
    this.blogPostRepo = blogPostRepo;
  }

  public String toSlug(String input) {
    if (input == null) {
      return "";
    }

    String slug = input.toLowerCase(Locale.ENGLISH);
    slug = slug.replace("æ", "ae")
        .replace("ø", "oe")
        .replace("å", "aa");

    slug = WHITESPACE.matcher(slug).replaceAll("-");
    slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
    slug = NON_LATIN.matcher(slug).replaceAll("");

    String finalSlug = slug;
    int counter = 2;
    while (true) {
      if (blogPostRepo.existsPostBySlug(finalSlug)) {
        finalSlug = slug + "-" + counter;
        counter++;
      } else {
        return finalSlug;
      }
    }
  }
}
