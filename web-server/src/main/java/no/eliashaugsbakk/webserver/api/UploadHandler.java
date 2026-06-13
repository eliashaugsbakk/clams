package no.eliashaugsbakk.webserver.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import no.eliashaugsbakk.utils.HashUtils;
import no.eliashaugsbakk.utils.JsonUtils;
import no.eliashaugsbakk.utils.Post;
import no.eliashaugsbakk.webserver.db.TokenRepository;
import no.eliashaugsbakk.webserver.service.PostStorageService;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

public class UploadHandler implements HttpHandler {
    private final PostStorageService postStorage;
    private final TokenRepository tokenRepo;

    public UploadHandler(PostStorageService postStorage, TokenRepository tokenRepo) {
        this.postStorage = postStorage;
        this.tokenRepo = tokenRepo;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.err.println("Bad token format");
            respond(exchange,HttpURLConnection.HTTP_UNAUTHORIZED);
            return;
        }

        String rawToken = authHeader.substring(7).trim();

        try {
            if (!tokenRepo.isValid(rawToken)) {
                System.err.println("Invalid token");
                respond(exchange,HttpURLConnection.HTTP_UNAUTHORIZED);
                return;
            }
        } catch (Exception e) {
            System.err.println("Database or system error: " + e.getMessage());
            respond(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR);
        }


        InputStream is = exchange.getRequestBody();
        String fullBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        String clientHash = getPartValue(fullBody, "sha256");
        String jsonBundle = getPartValue(fullBody, "file");

        try {
            if (jsonBundle == null) {
                System.err.println("Bundle is null");
                respond(exchange,HttpURLConnection.HTTP_BAD_REQUEST);
            }
            assert jsonBundle != null;
            if (!(new HashUtils().calculateSHA256(jsonBundle.getBytes())).equals(clientHash) ) {
                System.err.println("Hash mismatch");
                respond(exchange,HttpURLConnection.HTTP_BAD_REQUEST);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Post post = new JsonUtils().getPost(jsonBundle);

        try {
            if (!postStorage.createPage(post.title(), post.html(), post.images())) {
                throw new IOException("Failed to create page");
            }
            respond(exchange,HttpURLConnection.HTTP_ACCEPTED);
        } catch (Exception e) {
            respond(exchange,HttpURLConnection.HTTP_INTERNAL_ERROR);
            System.err.println("Could not add post to storage: " + e.getMessage());
        }
    }


    private void respond(HttpExchange exchange, int code) {
        try {

            exchange.sendResponseHeaders(code, -1);

        } catch (IOException e) {
            System.err.println("Could not send response: " + e.getMessage());
        }
    }

    /**
     * AI generated method to extract data from the http body.
     * To avoid any heavy dependencies.
     *
     * @param body the body of the http message
     * @param partName the part of the body to extract
     * @return the extracted value
     */
    private String getPartValue(String body, String partName) {
        // Look for the header: name="partName"
        String marker = "name=\"" + partName + "\"";
        int nameIndex = body.indexOf(marker);
        if (nameIndex == -1) return null;

        // The actual content starts after the double CRLF (\r\n\r\n)
        int contentStart = body.indexOf("\r\n\r\n", nameIndex) + 4;

        // The content ends at the next boundary start (\r\n--)
        int contentEnd = body.indexOf("\r\n--", contentStart);

        if (contentStart > 3 && contentEnd > contentStart) {
            return body.substring(contentStart, contentEnd).trim();
        }
        return null;
    }
}
