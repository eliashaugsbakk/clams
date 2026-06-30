package no.eliashaugsbakk.uploader.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigIO {
  private static final Path CONFIG_PATH =
      Paths.get(System.getProperty("user.home"), ".config", "cms", "config.properties");

  public static Properties load() {
    Properties props = new Properties();
    if (Files.exists(CONFIG_PATH)) {
      try (FileInputStream fis = new FileInputStream(CONFIG_PATH.toFile())) {
        props.load(fis);
      } catch (IOException e) {
        System.err.println("Could not load config: " + e.getMessage());
      }
    }
    return props;
  }

  public static void save(Properties props) {
    try {
      Files.createDirectories(CONFIG_PATH.getParent());
      try (FileOutputStream fos = new FileOutputStream(CONFIG_PATH.toFile())) {
        props.store(fos, "config for uploader");
      }
    } catch (IOException e) {
      System.err.println("Could not save config: " + e.getMessage());
    }
  }
}
