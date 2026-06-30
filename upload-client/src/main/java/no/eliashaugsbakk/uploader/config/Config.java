package no.eliashaugsbakk.uploader.config;

import java.util.Properties;

public class Config {
  private static Config instance;
  private Properties props = new Properties();

  private Config(Properties props) {
    this.props = props;
  }

  public static Config getInstance() {
    if (instance == null) {
      instance = new Config(ConfigIO.load());
    }
    return instance;
  }

  public String getToken() {
    return props.getProperty("token", "");
  }

  public String getUrl() {
    return props.getProperty("url", "");
  }

  public int getMaxImageSize() {
    return Integer.parseInt(props.getProperty("maxImageSize", "1920"));
  }

  public float getImageQuality() {
    return Float.parseFloat(props.getProperty("imageQuality", "0.85"));
  }

  public void setToken(String token) {
    props.setProperty("token", token);
    ConfigIO.save(props);
  }

  public void setUrl(String url) {
    props.setProperty("url", url);
    ConfigIO.save(props);
  }

  public void setMaxImageWidth(int width) {
    props.setProperty("maxImageWidth", String.valueOf(width));
    ConfigIO.save(props);
  }

  public void setImageQuality(float quality) {
    props.setProperty("imageQuality", String.valueOf(quality));
    ConfigIO.save(props);
  }
}
