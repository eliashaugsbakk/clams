package no.eliashaugsbakk.clams.server.config;

import java.nio.file.Path;
import no.eliashaugsbakk.clams.server.controller.PostsController;
import no.eliashaugsbakk.clams.server.controller.MediaController;
import no.eliashaugsbakk.clams.server.controller.PostController;
import no.eliashaugsbakk.clams.server.controller.ProjectsController;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;
import no.eliashaugsbakk.clams.server.repository.PostsRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.MediaRepo;
import no.eliashaugsbakk.clams.server.repository.MediaRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.ProjectsRepo;
import no.eliashaugsbakk.clams.server.repository.ProjectsRepoSqlite;
import no.eliashaugsbakk.clams.server.repository.SqliteManager;
import no.eliashaugsbakk.clams.server.service.AuthService;
import no.eliashaugsbakk.clams.server.service.SlugService;

public class AppContext implements AutoCloseable {
  private final SqliteManager dbManager;
  private final PostsController postsController;
  private final MediaController mediaController;
  private final PostController postController;
  private final ProjectsController projectsController;
  private final AuthService authService;

  public AppContext() {
    AppConfig appConfig = new AppConfig();
    appConfig.loadConfig();
    String dbUrl = Path.of(appConfig.getStorageLocation()).resolve("clams.db").toString();

    this.dbManager = new SqliteManager(dbUrl);
    this.dbManager.init();

    PostsRepo postsRepo = new PostsRepoSqlite(dbManager);
    MediaRepo mediaRepo = new MediaRepoSqlite(dbManager);
    ProjectsRepo projectsRepo = new ProjectsRepoSqlite(dbManager);
    SlugService slugService = new SlugService(postsRepo);

    this.postsController = new PostsController(dbManager);
    this.mediaController = new MediaController(mediaRepo, appConfig);
    this.projectsController = new ProjectsController(projectsRepo);
    this.postController = new PostController(postsRepo, slugService);
    this.authService = new AuthService(appConfig);
  }

  public PostsController getPostsController() { return postsController; }
  public MediaController getMediaController() { return mediaController; }
  public ProjectsController getProjectsController() { return projectsController; }
  public PostController getPostController() { return postController; }
  public AuthService getAuthService() { return authService; }

  @Override
  public void close() {
    if (dbManager != null) {
      dbManager.close();
    }
  }
}
