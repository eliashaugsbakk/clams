package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostDTO;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;
import no.eliashaugsbakk.clams.server.service.SlugService;

public class PostController {
  private final PostsRepo postsRepo;
  private final SlugService slugService;

  public PostController(PostsRepo postsRepo, SlugService slugService) {
    this.postsRepo = postsRepo;
    this.slugService = slugService;
  }

  public void handlePostPost(Context ctx) {
    PostDTO newPost = ctx.bodyAsClass(PostDTO.class);
    postsRepo.addPost(new Post(newPost, slugService.toSlug(newPost.title())));
  }

  public void handlePutPost(Context ctx) {
    PostDTO updatedPost = ctx.bodyAsClass(PostDTO.class);
    postsRepo.updatePost(new Post(updatedPost, ctx.pathParam("slug")));
  }

  public void handleDeletePost(Context ctx) {
    postsRepo.deletePost(ctx.pathParam("slug"));
  }
}
