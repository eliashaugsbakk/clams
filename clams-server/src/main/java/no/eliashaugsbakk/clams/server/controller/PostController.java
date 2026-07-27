package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
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
    ctx.status(HttpStatus.CREATED);
  }

  public void handlePutPost(Context ctx) {
    String slug = ctx.pathParam("slug");
    PostDTO updatedPost = ctx.bodyAsClass(PostDTO.class);

    postsRepo.getPost(slug)
        .map(existing -> Post.fromUpdated(existing, updatedPost))
        .ifPresentOrElse(
            postsRepo::updatePost,
            () -> ctx.status(HttpStatus.NOT_FOUND));
  }

  public void handleDeletePost(Context ctx) {
    if (!postsRepo.deletePost(ctx.pathParam("slug"))) {
      ctx.status(HttpStatus.NOT_FOUND);
    } else {
      ctx.status(HttpStatus.NO_CONTENT);
    }
  }
}
