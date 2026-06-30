package no.eliashaugsbakk.uploader.service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * HTTP client and request execution. Handles connection pooling, timeouts, and low-level HTTP
 * operations.
 */
public class HttpClientService {
  private final OkHttpClient client;

  public HttpClientService(int timeoutSeconds) {
    this.client =
        new OkHttpClient.Builder().connectTimeout(timeoutSeconds, TimeUnit.SECONDS).build();
  }

  /**
   * Execute an HTTP POST request and return the response.
   *
   * @param url         the target URL
   * @param authToken   bearer token (may be null)
   * @param requestBody the request body
   * @return the server response
   * @throws IOException on network or HTTP errors
   */
  public Response post(String url, String authToken, RequestBody requestBody) throws IOException {
    Request.Builder builder = new Request.Builder().url(url).post(requestBody);

    if (authToken != null) {
      builder.header("Authorization", "Bearer " + authToken);
    }

    Request request = builder.build();
    return client.newCall(request).execute();
  }

  /**
   * Execute a simple GET request.
   *
   * @param url the target URL
   * @return the server response
   * @throws IOException on network errors
   */
  public Response get(String url) throws IOException {
    Request request = new Request.Builder().url(url).get().build();
    return client.newCall(request).execute();
  }
}
