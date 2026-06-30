package no.eliashaugsbakk.uploader.service;

import no.eliashaugsbakk.uploader.config.Config;
import no.eliashaugsbakk.uploader.exception.UploaderException;
import no.eliashaugsbakk.uploader.model.NetworkConfig;
import no.eliashaugsbakk.uploader.utils.AuthUtils;
import no.eliashaugsbakk.utils.DocumentUpload;
import no.eliashaugsbakk.utils.HashUtils;
import no.eliashaugsbakk.utils.JsonUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Core upload orchestration logic — independent of UI/CLI.
 * Handles configuration, normalization, serialization, and transmission.
 */
public class UploaderOrchestrator {
  private final UploadService uploadService;
  private final ConnectivityChecker connectivityChecker;
  private final JsonUtils jsonUtils;
  private final HashUtils hashUtils;
  private final AuthUtils authUtils;

  public UploaderOrchestrator(UploadService uploadService, ConnectivityChecker connectivityChecker) {
    this.uploadService = uploadService;
    this.connectivityChecker = connectivityChecker;
    this.jsonUtils = new JsonUtils();
    this.hashUtils = new HashUtils();
    this.authUtils = new AuthUtils();
  }

  /**
   * Test connectivity to the configured server.
   * @throws UploaderException if configuration is invalid
   */
  public void testConnectivity() {
    NetworkConfig cfg = getValidatedConfig();
    connectivityChecker.testConnectivity(cfg.url());
  }

  /**
   * Perform a complete upload workflow.
   * @param filePaths paths to files (1 .md + N images)
   * @throws Exception on any step failure
   */
  public void uploadFiles(List<String> filePaths, String summary, List<String> tags) throws Exception {

    // Normalize and serialize
    DataNormalizerService normalizer = new DataNormalizerService(filePaths);
    DocumentUpload post = new DocumentUpload(
        normalizer.getTextFile().title(),
        normalizer.getTextFile().body(),
        summary,
        tags,
        normalizer.getImagesFiles()
    );

    String json = jsonUtils.getJson(post);
    String hash = hashUtils.calculateSHA256(json.getBytes(StandardCharsets.UTF_8));

    // Upload
    uploadService.uploadBundle(
        json.getBytes(StandardCharsets.UTF_8),
        "Upload_JSON_" + System.currentTimeMillis(),
        hash
    );
  }

  /**
   * Update server URL in persistent config.
   */
  public void setServerUrl(String url) throws IOException {
    Config.getInstance().setUrl(url);
  }

  /**
   * Generate and store a new authentication token.
   */
  public void generateToken() throws IOException {
    String token = authUtils.generateAuthKey(32);
    Config.getInstance().setToken(token);
  }

  /**
   * Store a user-provided authentication token.
   */
  public void setToken(String token) {
    Config.getInstance().setToken(token);
  }

  /**
   * Validate that required configuration exists.
   */
  private NetworkConfig getValidatedConfig() {
    String url = Config.getInstance().getUrl();
    String token = Config.getInstance().getToken();
    if (url == null || url.equals("-")) {
      throw new UploaderException("Incomplete url configuration. Use --setUrl <url> to set the url.");
    }
    if (token == null || token.equals("-")) {
      throw new UploaderException("Incomplete token configuration. Use --setToken to generate a new token.");
    }
    return new NetworkConfig(url, token);
  }
}
