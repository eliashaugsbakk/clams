package no.eliashaugsbakk.webserver;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Config {
  private static Config instance;
  private final Properties props = new Properties();

  private Config() {
    try (FileInputStream fis = new FileInputStream("config.properties")) {
      props.load(fis);
    } catch (IOException e) {
      System.err.println("Could not load config.properties, using defaults.");
    }
  }

  public static synchronized Config getInstance() {
    if (instance == null) {
      instance = new Config();
    }
    return instance;
  }

  public String getDbPath() {
    // Returns the path from file, or a default
    return props.getProperty("db.path", "dev_database.db");
  }
}
