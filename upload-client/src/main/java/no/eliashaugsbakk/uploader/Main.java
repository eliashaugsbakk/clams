package no.eliashaugsbakk.uploader;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import no.eliashaugsbakk.uploader.config.Config;
import no.eliashaugsbakk.uploader.controller.CliController;
import no.eliashaugsbakk.uploader.exception.UploaderException;
import no.eliashaugsbakk.uploader.service.ConnectivityChecker;
import no.eliashaugsbakk.uploader.service.HttpClientService;
import no.eliashaugsbakk.uploader.service.UploadService;
import no.eliashaugsbakk.uploader.service.UploaderOrchestrator;

public class Main {
  static void main(String[] args) {
    try {
      CliController controller = getCliController();
      controller.execute(args);

    } catch (UploaderException e) {
      System.err.println("\n[!] CONFIGURATION ERROR: " + e.getMessage());
      System.exit(1);

    } catch (IOException e) {
      System.err.println("\n[X] FILE SYSTEM ERROR");
      System.err.println("    Could not read file: " + e.getMessage());
      System.exit(1);

    } catch (Exception e) {
      System.err.println("\n[X] ERROR: " + e.getLocalizedMessage());
      System.exit(1);
    }
  }

  private static CliController getCliController() {
    HttpClientService httpClientService = new HttpClientService(20);
    UploadService uploadService =
        new UploadService(httpClientService, Config.getInstance().getUrl(),
            Config.getInstance().getToken());
    ConnectivityChecker connectivityChecker = new ConnectivityChecker(httpClientService);
    UploaderOrchestrator uploaderOrchestrator =
        new UploaderOrchestrator(uploadService, connectivityChecker);
    return new CliController(uploaderOrchestrator);
  }
}
