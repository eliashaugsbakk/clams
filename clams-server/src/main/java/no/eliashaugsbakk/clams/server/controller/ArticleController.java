package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import no.eliashaugsbakk.clams.server.model.Article;
import no.eliashaugsbakk.clams.server.model.ArticleDTO;
import no.eliashaugsbakk.clams.server.repository.ArticlesRepo;
import no.eliashaugsbakk.clams.server.service.SlugService;

public class ArticleController {
  private final ArticlesRepo articlesRepo;
  private final SlugService slugService;

  public ArticleController(ArticlesRepo articlesRepo, SlugService slugService) {
    this.articlesRepo = articlesRepo;
    this.slugService = slugService;
  }

  public void handlePostArticle(Context ctx) {
    ArticleDTO newArticle = ctx.bodyAsClass(ArticleDTO.class);
    articlesRepo.addArticle(new Article(newArticle, slugService.toSlug(newArticle.title())));
  }

  public void handlePutArticle(Context ctx) {
    ArticleDTO updatedArticle = ctx.bodyAsClass(ArticleDTO.class);
    articlesRepo.updateArticle(new Article(updatedArticle, ctx.pathParam("slug")));
  }

  public void handleDeleteArticle(Context ctx) {
    articlesRepo.deleteArticle(ctx.pathParam("slug"));
  }
}
