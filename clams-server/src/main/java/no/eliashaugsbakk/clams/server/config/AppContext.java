package no.eliashaugsbakk.clams.server.config;

import java.nio.file.Path;
import no.eliashaugsbakk.clams.server.controller.BlogController;
import no.eliashaugsbakk.clams.server.controller.MediaController;
import no.eliashaugsbakk.clams.server.controller.PostController;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepo;
import no.eliashaugsbakk.clams.server.repository.BlogPostRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.MediaRepo;
import no.eliashaugsbakk.clams.server.repository.MediaRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;
import no.eliashaugsbakk.clams.server.service.SlugService;

public class AppContext implements AutoCloseable {
  private final SqliteManager dbManager;
  private final BlogController blogController;
  private final MediaController mediaController;
  private final PostController postController;

  public AppContext() {
    AppConfig appConfig = new AppConfig();
    appConfig.loadConfig();
    String dbUrl = Path.of(appConfig.getStorageLocation()).resolve("clams.db").toString();

    this.dbManager = new SqliteManager(dbUrl);
    this.dbManager.init();

    BlogPostRepo blogPostRepo = new BlogPostRepoSqlite(dbManager);
    MediaRepo mediaRepo = new MediaRepoSqlite(dbManager);
    SlugService slugService = new SlugService(blogPostRepo);

    this.blogController = new BlogController(dbManager);
    this.mediaController = new MediaController(mediaRepo, appConfig);
    this.postController = new PostController(blogPostRepo, slugService);
  }

  public BlogController getBlogController() { return blogController; }
  public MediaController getMediaController() { return mediaController; }
  public PostController getPostController() { return postController; }

  @Override
  public void close() {
    if (dbManager != null) {
      dbManager.close();
    }
  }
}
