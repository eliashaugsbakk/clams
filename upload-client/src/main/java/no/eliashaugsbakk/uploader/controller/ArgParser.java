package no.eliashaugsbakk.uploader.controller;

import java.util.ArrayList;
import java.util.List;
import no.eliashaugsbakk.uploader.exception.UploaderException;
import no.eliashaugsbakk.uploader.model.Input;

public class ArgParser {
  public Input parse(String[] args) {

    boolean markdownFilePassed = false;

    List<String> filePaths = new ArrayList<>();
    String url = null;
    String token = null;
    boolean generateToken = false;
    boolean networkTest = false;
    boolean helpRequested = args.length == 0;

    for (int i = 0; i < args.length; i++) {
      String arg = args[i];

      if (arg.startsWith("-")) {
        switch (arg) {
          case "-u", "--setUrl" -> url = getNext(args, i++);
          case "--setToken" -> token = getNext(args, i++);
          case "-t", "--genToken" -> generateToken = true;
          case "-n", "--networkTest" -> networkTest = true;
          case "-h", "--help" -> helpRequested = true;
          default -> throw new UploaderException("Unknown flag: " + arg);
        }
      } else {
        if (!markdownFilePassed && arg.endsWith(".md")) {
          filePaths.add(arg);
          IO.println("Markdown file passed: " + arg);
          markdownFilePassed = true;
        } else if (arg.endsWith(".png") || arg.endsWith(".jpg") || arg.endsWith(".jpeg") ||
            arg.endsWith(".gif")) {
          filePaths.add(arg);
          IO.println("Image added: " + arg);
        } else {
          throw new UploaderException("Only one markdown file allowed per upload.");
        }
      }
    }
    return new Input(filePaths, url, token, generateToken, networkTest, helpRequested);
  }

  private String getNext(String[] args, int index) {
    if (index + 1 >= args.length) {
      throw new UploaderException("Missing value after flag " + args[index]);
    }
    return args[index + 1];
  }

}
