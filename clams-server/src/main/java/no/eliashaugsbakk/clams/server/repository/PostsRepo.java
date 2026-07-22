package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;

public interface PostsRepo {
  List<PostMetaData> listPostsMetaData();
  Optional<Post> getPost(String slug);
  List<PostMetaData> searchPostsBody(String query);

  void addPost(Post post);
  void updatePost(Post post);
  void deletePost(String slug);
  boolean existsPostBySlug(String slug);
}
