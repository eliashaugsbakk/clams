package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostDTO;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepo;
import no.eliashaugsbakk.clams.server.service.SlugService;

public class PostController {
  private final BlogPostRepo blogPostRepo;
  private final SlugService slugService;

  public PostController(BlogPostRepo blogPostRepo, SlugService slugService) {
    this.blogPostRepo = blogPostRepo;
    this.slugService = slugService;
  }

  public void handlePostPost(Context ctx) {
    PostDTO newPost = ctx.bodyAsClass(PostDTO.class);
    blogPostRepo.addBlogPost(new Post(newPost, slugService.toSlug(newPost.title())));
  }

  public void handlePutPost(Context ctx) {
    PostDTO updatedPost = ctx.bodyAsClass(PostDTO.class);
    blogPostRepo.updateBlogPost(new Post(updatedPost, ctx.pathParam("slug")));
  }

  public void handleDeletePost(Context ctx) {
    blogPostRepo.deleteBlogPost(ctx.pathParam("slug"));
  }
}
