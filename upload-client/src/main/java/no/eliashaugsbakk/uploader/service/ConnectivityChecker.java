package no.eliashaugsbakk.uploader.service;

import java.io.IOException;
import okhttp3.Response;

/**
 * Diagnostic utilities for testing server connectivity. Separate from upload logic to keep concerns
 * isolated.
 */
public class ConnectivityChecker {
  private final HttpClientService httpClient;

  public ConnectivityChecker(HttpClientService httpClient) {
    this.httpClient = httpClient;
  }

  /**
   * Test connectivity to a server and report results.
   *
   * @param url the server URL
   */
  public void testConnectivity(String url) {
    IO.println("Pinging server: " + url);
    try (Response response = httpClient.get(url)) {
      IO.println("Server is reachable");
      if (response.code() == 200) {
        IO.println("Status code: " + response.code() + " - OK");
      } else {
        IO.println("Status code: " + response.code());
      }
    } catch (java.net.ConnectException e) {
      System.err.println("\n[!] CONNECTION REFUSED");
      System.err.println("    Could not connect to the server.");
      System.err.println("    Verify that the URL is correct and the server is running.");
    } catch (java.net.SocketException e) {
      System.err.println("\n[!] NETWORK ERROR: " + e.getMessage());
      System.err.println("    Verify your internet connection.");
      System.err.println("    Is the server running correctly?");
    } catch (IOException e) {
      System.err.println("\n[X] I/O ERROR: " + e.getMessage());
    }
  }
}
