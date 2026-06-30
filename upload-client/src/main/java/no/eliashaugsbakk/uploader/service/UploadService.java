package no.eliashaugsbakk.uploader.service;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handles the upload workflow: serializes data, constructs multipart payloads, and sends to the
 * server. Does NOT handle connectivity diagnostics.
 */
public class UploadService {
  private final HttpClientService httpClient;
  private final String baseUrl;
  private final String authToken;

  public UploadService(HttpClientService httpClient, String baseUrl, String authToken) {
    this.httpClient = httpClient;
    this.baseUrl = baseUrl;
    this.authToken = authToken;
  }

  /**
   * Upload a bundle (file, images, hash, filename) to the server.
   *
   * @param data     the file bytes
   * @param fileName the file name
   * @param hash     the SHA-256 hash
   * @throws IOException on upload failure
   */
  public void uploadBundle(byte[] data, String fileName, String hash) throws IOException {
    RequestBody fileBody =
        RequestBody.create(data, MediaType.parse("application/json; charset=utf-8"));
    RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("file", fileName, fileBody).addFormDataPart("sha256", hash).build();

    try (Response response = httpClient.post(baseUrl + "/upload", authToken, requestBody)) {
      if (!response.isSuccessful()) {
        throw new IOException("Server returned " + response.code() + ": " + response.message());
      }
      String responseBody = response.body() != null ? response.body().string() : "";
      IO.println("Upload successful. Server response: " + responseBody);
    }
  }
}
