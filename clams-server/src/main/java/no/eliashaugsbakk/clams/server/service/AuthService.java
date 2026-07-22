package no.eliashaugsbakk.clams.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import no.eliashaugsbakk.clams.server.config.AppConfig;

public class AuthService {
  private final String tokenHash;

  public AuthService(AppConfig appConfig) {
    this.tokenHash = appConfig.getAuthToken();
  }

  public boolean isValid(String incomingToken) {
    if (incomingToken == null || incomingToken.isBlank()) {
      return false;
    }

    try {
      return MessageDigest.isEqual(
          tokenHash.getBytes(StandardCharsets.UTF_8),
          incomingToken.getBytes(StandardCharsets.UTF_8));
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
