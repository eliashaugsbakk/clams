package no.eliashaugsbakk.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;


public class JsonUtils {

  private final ObjectMapper mapper = new ObjectMapper();

  public String getJson(DocumentUpload post) throws IOException {
    return mapper.writeValueAsString(post);
  }

  public DocumentUpload getPost(String jsonString) throws IOException {
    return mapper.readValue(jsonString, DocumentUpload.class);
  }
}
