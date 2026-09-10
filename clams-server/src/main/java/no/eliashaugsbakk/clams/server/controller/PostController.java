package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostDTO;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;
import no.eliashaugsbakk.clams.server.service.SlugService;
import no.eliashaugsbakk.clams.server.utils.ApiValidation;
import no.eliashaugsbakk.clams.server.utils.ErrorResponses;

// BEGIN LLM EDIT: Added the following disclaimer while integrating the CLI post API.
/**
 * Authenticated post mutation controller.
 *
 * <p>Disclaimer: This file was touched by an LLM while wiring the CLI's post API integration;
 * no post mutation behavior was intentionally changed here.</p>
 */
// END LLM EDIT
public class PostController {
  private final PostsRepo postsRepo;
  private final SlugService slugService;

  public PostController(PostsRepo postsRepo, SlugService slugService) {
    this.postsRepo = postsRepo;
    this.slugService = slugService;
  }

  public void handlePostPost(Context ctx) {
    PostDTO newPost = ctx.bodyAsClass(PostDTO.class);
    validatePost(newPost);
    String slug = slugService.toSlug(newPost.title());
    if (slug.isBlank()) {
      throw new io.javalin.http.BadRequestResponse("The title must contain letters or numbers.");
    }
    postsRepo.addPost(new Post(newPost, slug));
    ctx.status(HttpStatus.CREATED);
  }

  public void handlePutPost(Context ctx) {
    long id = parseId(ctx);
    PostDTO updatedPost = ctx.bodyAsClass(PostDTO.class);
    validatePost(updatedPost);

    postsRepo.getPost(id)
        .ifPresentOrElse(
            existing -> {
              postsRepo.updatePost(Post.fromUpdated(existing, updatedPost));
              ctx.status(HttpStatus.NO_CONTENT);
            },
            () -> ErrorResponses.notFound(ctx, "Post not found."));
  }

  public void handleDeletePost(Context ctx) {
    if (!postsRepo.deletePost(parseId(ctx))) {
      ErrorResponses.notFound(ctx, "Post not found.");
    } else {
      ctx.status(HttpStatus.NO_CONTENT);
    }
  }

  private long parseId(Context ctx) {
    try {
      return Long.parseLong(ctx.pathParam("id"));
    } catch (NumberFormatException e) {
      throw new io.javalin.http.BadRequestResponse("Invalid post ID format");
    }
  }

  // BEGIN LLM EDIT: Apply stable size and required-field constraints to post payloads.
  private void validatePost(PostDTO post) {
    ApiValidation.requiredText("title", post.title(), 200);
    ApiValidation.requiredText("content", post.content(), 1_000_000);
    ApiValidation.optionalText("summary", post.summary(), 1_000);
  }
  // END LLM EDIT
}
