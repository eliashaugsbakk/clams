package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;

public interface BlogPostRepo {
  List<PostMetaData> listPostsMetaData();
  Optional<Post> getPost(String slug);
  List<PostMetaData> searchPostsBody(String query);
}
