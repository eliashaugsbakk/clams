package no.eliashaugsbakk.clams.server.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.eliashaugsbakk.clams.server.model.PostMetaData;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;

public class PostsSearchService {
  private final PostsRepo postsRepo;

  public PostsSearchService(PostsRepo postsRepo) {
    this.postsRepo = postsRepo;
  }

  public List<PostMetaData> searchPosts(String query) {
    if (query == null || query.isBlank()) return List.of();

    int titleValue = 10;
    int summaryValue = 5;
    int bodyValue = 1;

    Map<PostMetaData, Integer> searchRanking = new HashMap<>();
    String lowerQuery = query.toLowerCase();

    List<PostMetaData> postsMetaData = postsRepo.listPostsMetaData();
    List<PostMetaData> bodyResults = postsRepo.searchPostsBody(query);

    postsMetaData.stream()
        .filter(post -> post.title().toLowerCase().contains(lowerQuery))
        .forEach(post -> searchRanking.merge(post, titleValue, Integer::sum));

    postsMetaData.stream()
        .filter(post -> post.summary() != null && post.summary().toLowerCase().contains(lowerQuery))
        .forEach(post -> searchRanking.merge(post, summaryValue, Integer::sum));

    bodyResults.forEach(post -> searchRanking.merge(post, bodyValue, Integer::sum));

    return searchRanking.entrySet().stream()
        .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
        .map(Map.Entry::getKey)
        .toList();
  }
}
