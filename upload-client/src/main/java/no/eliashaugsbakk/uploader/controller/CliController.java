package no.eliashaugsbakk.uploader.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import no.eliashaugsbakk.uploader.model.Input;
import no.eliashaugsbakk.uploader.service.UploaderOrchestrator;

/**
 * CLI entry point. Parses arguments and delegates to UploaderOrchestrator.
 */
public class CliController {
  private final UploaderOrchestrator orchestrator;

  public CliController(UploaderOrchestrator orchestrator) {
    this.orchestrator = orchestrator;
  }

  public void execute(String[] args) throws Exception {
    Input input = new ArgParser().parse(args);

    if (input.helpRequested()) {
      printHelp();
      return;
    }

    // Update config
    if (input.url() != null) {
      orchestrator.setServerUrl(input.url());
      IO.println("URL has been set: " + input.url());
    }
    if (input.generateToken()) {
      orchestrator.generateToken();
      IO.println("Token has been generated.");
    }
    if (input.token() != null) {
      orchestrator.setToken(input.token());
      IO.println("Token has been set.");
    }

    // Execute actions
    if (input.networkTest()) {
      orchestrator.testConnectivity();
      IO.println("Connection test completed.");
    }

    if (!input.filePaths().isEmpty()) {
      String summary = IO.readln("Short description/summary of the document: ");
      String tags = IO.readln("Tags separated by comma: ");

      List<String> tagsList = Arrays.stream(tags.split(","))
          .map(String::trim)
          .map(String::toLowerCase)
          .filter(tag -> !tag.isEmpty())
          .collect(Collectors.toList());

      IO.println("Uploading file(s)...");
      orchestrator.uploadFiles(input.filePaths(), summary, tagsList);
      IO.println("File(s) have been uploaded.");
    }
  }

  private void printHelp() {
    IO.println("""
        Help:
        uploadClient [-h | --help]
        
        General use:
        Pass in relative or absolute file paths for files to upload.
          (Only supports one .md file at a time. Supported images: png, jpg, jpeg, bmp, webp)
        
        Options:
        uploadClient [option] [choice]
        
        Options:
        [-t | --setToken]               - generates a new Authentication Token.
        [-u | --setUrl]   <url>         - sets the host url.
        [-n | --networkTest]            - tests the network connection.
        """);
  }
}
