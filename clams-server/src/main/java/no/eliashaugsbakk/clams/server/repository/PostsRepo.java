package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;

public interface PostsRepo {
  List<PostMetaData> listPostsMetaData();
  Optional<Post> getPost(long id);
  List<PostMetaData> searchPostsBody(String query);

  long addPost(Post post);
  void updatePost(Post post);
  boolean deletePost(long id);
  boolean existsPostBySlug(String slug);
  List<String> findPostTitlesReferencing(String imageReference);
}
