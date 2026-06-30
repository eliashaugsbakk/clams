package no.eliashaugsbakk.uploader.utils;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.stream.Collectors;

/**
 * Generation of cryptographically secure authentication tokens.
 */
public class AuthUtils {
  private static final String CHARS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final SecureRandom SECURE_RANDOM;

  static {
    try {
      SECURE_RANDOM = SecureRandom.getInstanceStrong();
    } catch (NoSuchAlgorithmException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Generate a cryptographically secure random token.
   *
   * @param length the desired token length
   * @return a random token of the specified length
   */
  public String generateAuthKey(int length) {
    return SECURE_RANDOM.ints(length, 0, CHARS.length()).mapToObj(CHARS::charAt)
        .map(String::valueOf).collect(Collectors.joining());
  }
}
