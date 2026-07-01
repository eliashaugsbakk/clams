package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import java.util.Optional;
import no.eliashaugsbakk.clams.server.model.Post;
import no.eliashaugsbakk.clams.server.model.PostMetaData;

public interface BlogPostRepo {
  List<PostMetaData> listPostsByPublishedDesc();
  Optional<Post> getPost(String slug);
}
