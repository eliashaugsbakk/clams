package no.eliashaugsbakk.clams.server.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {
  private final Properties properties = new Properties();
  private Path configPath;

  public void loadConfig() {
    String userHome = System.getProperty("user.home");
    configPath = Paths.get(userHome, ".config", "clams", "clams.properties");

    if (!Files.exists(configPath)) {
      generateDefaultConfig();
    }

    try (var reader = Files.newBufferedReader(configPath)) {
      properties.load(reader);
      ensureStorageDirectoryExists();
    } catch (IOException e) {
      System.err.println("Failed to load configuration file: " + e.getMessage());
    }
  }

  private void ensureStorageDirectoryExists() {
    try {
      Path storageDir = Path.of(getStorageLocation());
      if (!Files.exists(storageDir)) {
        Files.createDirectories(storageDir);
        System.out.println("Created application storage directory at: " + storageDir.toAbsolutePath());
      }
    } catch (IOException e) {
      System.err.println("Warning: Could not verify or create storage directory: " + e.getMessage());
    }
  }

  private void generateDefaultConfig() {
    try {
      Files.createDirectories(configPath.getParent());

      try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
        writer.write("storage_location=./data/\n\n");
      }
      System.out.println("Generated a default configuration file at: " + configPath);

    } catch (IOException e) {
      System.err.println("Could not create default config file: " + e.getMessage());
    }
  }

  public String getStorageLocation() {
    return properties.getProperty("storage_location", "./data");
  }
}
