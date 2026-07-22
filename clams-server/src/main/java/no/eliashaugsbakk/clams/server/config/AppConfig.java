package no.eliashaugsbakk.clams.server.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;
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
      throw new RuntimeException("Failed to read configuration file: " + configPath, e);
    }
  }

  private void ensureStorageDirectoryExists() {
    try {
      Path storageDir = Path.of(getStorageLocation());
      if (!Files.exists(storageDir)) {
        Files.createDirectories(storageDir);
        System.out.println("Created application storage directory at: " + storageDir.toAbsolutePath());
      }
      setOwnerOnlyPermissions(storageDir);
    } catch (IOException e) {
      System.err.println("Warning: Could not verify or create storage directory: " + e.getMessage());
    }
  }

  private void setOwnerOnlyPermissions(Path path) throws IOException {
    if (!Files.exists(path)) {
      return;
    }

    try {
      Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
    } catch (UnsupportedOperationException e) {
      // Ignore on non-POSIX file systems.
    }
  }

  private void generateDefaultConfig() {
    try {
      Files.createDirectories(configPath.getParent());

      byte[] tokenBytes = new byte[32];
      new SecureRandom().nextBytes(tokenBytes);
      String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

      Files.createDirectories(configPath.getParent());
      try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
        writer.write(String.format("""
            storage_location=./data/
            
            authorization_token=%s
            """, token));
      }
      setOwnerOnlyPermissions(configPath);
      IO.println("Generated a default configuration file at: " + configPath);

    } catch (IOException e) {
      System.err.println("Could not create default config file: " + e.getMessage());
    }
  }

  public String getStorageLocation() {
    return properties.getProperty("storage_location", "./data");
  }

  public String getAuthToken() {
    String token = properties.getProperty("authorization_token");
    if (token == null || token.isBlank()) {
      System.err.println("CRITICAL: 'authorization_token' is missing or empty in config properties!");

      throw new IllegalStateException("CRITICAL: 'authorization_token' is missing or empty in config properties!");
    }
    return token;
  }
}
