package no.eliashaugsbakk.webserver;


import java.io.IOException;
import no.eliashaugsbakk.webserver.db.Jdbc.DatabaseManager;
import no.eliashaugsbakk.webserver.db.Jdbc.JdbcPageRepository;
import no.eliashaugsbakk.webserver.db.Jdbc.JdbcTokenRepository;
import no.eliashaugsbakk.webserver.db.PageRepository;
import no.eliashaugsbakk.webserver.db.TokenRepository;
import no.eliashaugsbakk.webserver.service.PostStorageService;

class WebApp {
  static void main(String[] args) throws Exception {

    DatabaseManager databaseManager = new DatabaseManager();
    PageRepository pageRepo = new JdbcPageRepository(databaseManager);
    PostStorageService postStorage = new PostStorageService(pageRepo);
    TokenRepository tokenRepo = new JdbcTokenRepository(databaseManager);

    databaseManager.initialize();

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (arg.equals("--addToken")) {
        if (!tokenRepo.addToken(args[++i])) {
          System.err.println("Failed to add token");
        }
      }
    }

    try {
      Server server = new Server();
      server.start(pageRepo, postStorage, tokenRepo);
    } catch (IOException e) {
      System.err.println("Could not start server: " + e.getMessage());
    }
  }
}
