package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.eliashaugsbakk.clams.server.config.AppConfig;
import no.eliashaugsbakk.clams.server.model.ImageMetaData;
import no.eliashaugsbakk.clams.server.repository.MediaRepo;
import no.eliashaugsbakk.clams.server.repository.PostsRepo;
import no.eliashaugsbakk.clams.server.utils.ErrorResponses;
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;

// BEGIN LLM EDIT: Documented the public image URL behavior change.
/**
 * Media upload and retrieval controller.
 *
 * <p>Disclaimer: The following change was written by an LLM: successful image uploads now return
 * a public {@code /media/{uuid}} URL instead of the authenticated API URL.</p>
 */
// END LLM EDIT
public class MediaController {
  // BEGIN LLM EDIT: Add advisory and hard upload-size thresholds for image safety.
  private static final long IMAGE_WARNING_BYTES = 5L * 1024 * 1024;
  private static final long IMAGE_MAX_BYTES = 20L * 1024 * 1024;
  private static final long REQUEST_MAX_BYTES = 22L * 1024 * 1024;
  // END LLM EDIT

  private final MediaRepo mediaRepo;
  private final PostsRepo postsRepo;
  private final AppConfig appConfig;

  public MediaController(MediaRepo mediaRepo, PostsRepo postsRepo, AppConfig appConfig) {
    this.mediaRepo = mediaRepo;
    this.postsRepo = postsRepo;
    this.appConfig = appConfig;
  }

  // BEGIN LLM EDIT: Return complete upload metadata for CLI confirmation and image reuse.
  public record ImageUploadResponse(UUID uuid, String url, String originalFilename,
                                    String contentType, String timeUploaded) {
  }
  // END LLM EDIT

  public void handlePostMedia(Context ctx) {
    UploadedFile file = ctx.uploadedFile("image");

    if (file == null) {
      ErrorResponses.badRequest(ctx, "Missing image file payload.");
      return;
    }

    String contentLength = ctx.header("Content-Length");
    if (contentLength != null) {
      try {
        if (Long.parseLong(contentLength) > REQUEST_MAX_BYTES) {
          ErrorResponses.payloadTooLarge(ctx, "Image requests must be no larger than 20 MiB.");
          return;
        }
      } catch (NumberFormatException ignored) {
        // The bounded stream below remains the authoritative limit.
      }
    }

    try (InputStream is = file.content()) {
      byte[] imageBytes = readAtMost(is, IMAGE_MAX_BYTES);
      if (imageBytes.length > IMAGE_WARNING_BYTES) {
        ctx.header("X-Clams-Warning",
            "Image is larger than 5 MiB; consider compressing it when practical.");
      }

      try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
        ImageInfo info = Imaging.getImageInfo(bais, file.filename());
        ImageFormat format = info.getFormat();

        if (format != ImageFormats.JPEG) {
          ErrorResponses.unsupportedMediaType(ctx, "Unsupported format: only jpeg allowed.");
          return;
        }

        if (info.getWidth() > 2000 || info.getHeight() > 2000) {
          ErrorResponses.badRequest(ctx, "Image dimensions are too large: max allowed 2000x2000");
          return;
        }
      }

      // BEGIN LLM EDIT: Use one timestamp for both persisted metadata and the upload response.
      Instant uploadedAt = Instant.now();
      UUID generatedUuid = saveToStorage(
          imageBytes, file.filename(), file.contentType(), uploadedAt);
      // END LLM EDIT

      // BEGIN LLM EDIT: Return the public image route and stored metadata in one response.
      ctx.status(201).json(new ImageUploadResponse(
          generatedUuid,
          "/media/" + generatedUuid,
          file.filename(),
          file.contentType(),
          uploadedAt.toString()));
      // END LLM EDIT
    } catch (PayloadTooLargeException e) {
      ErrorResponses.payloadTooLarge(ctx, "Images must be no larger than 20 MiB.");
    } catch (Exception e) {
      ErrorResponses.badRequest(ctx, "Corrupted or invalid image data.");
    }
  }

  // BEGIN LLM EDIT: Bound image buffering so oversized bodies cannot consume unbounded memory.
  private static byte[] readAtMost(InputStream input, long maximumBytes) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    long total = 0;
    int read;
    while ((read = input.read(buffer)) != -1) {
      total += read;
      if (total > maximumBytes) {
        throw new PayloadTooLargeException();
      }
      output.write(buffer, 0, read);
    }
    return output.toByteArray();
  }

  private static final class PayloadTooLargeException extends IOException {
  }
  // END LLM EDIT

  private UUID saveToStorage(
      byte[] bytes, String originalFilename, String contentType, Instant uploadedAt)
      throws IOException {
    UUID uuid = UUID.randomUUID();
    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");
    Files.createDirectories(path.getParent());
    Files.write(path, bytes);

    try {
      mediaRepo.addImage(new ImageMetaData(uuid, originalFilename, contentType, uploadedAt));
      return uuid;
    } catch (Exception e) {
      Files.deleteIfExists(path);
      throw new IOException("Failed to add image record to database", e);
    }
  }

  public void handleGetMedia(Context ctx) {
    String uuidStr = ctx.pathParam("uuid");
    UUID uuid;

    try {
      uuid = UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      ErrorResponses.badRequest(ctx, "Invalid UUID format.");
      return;
    }

    if (mediaRepo.getImage(uuid).isEmpty()) {
      ErrorResponses.notFound(ctx, "Image not found.");
      return;
    }

    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");

    if (Files.exists(path)) {
      ctx.contentType("image/jpeg");

      try {
        ctx.result(Files.newInputStream(path));
      } catch (IOException e) {
        ErrorResponses.serverError(ctx, "Error reading image file.");
      }
    } else {
      ErrorResponses.notFound(ctx, "Image file missing from storage.");
    }
  }

  public record ImageResponse(UUID uuid, String originalFilename, String contentType,
                              String timeUploaded) {
  }


  public void handleGetMediaIndex(Context ctx) {
    List<ImageMetaData> metaDataList = mediaRepo.getAllImagesMetaData();

    List<ImageResponse> responseList = metaDataList.stream().map(
        meta -> new ImageResponse(meta.uuid(), meta.originalFilename(), meta.contentType(),
            meta.timeUploaded().toString())).toList();

    ctx.status(200).json(responseList);
  }

  public void handleDeleteMedia(Context ctx) {
    UUID uuid;
    try {
      uuid = UUID.fromString(ctx.pathParam("uuid"));
    } catch (IllegalArgumentException e) {
      ErrorResponses.badRequest(ctx, "Invalid UUID format.");
      return;
    }

    // BEGIN LLM EDIT: Refuse deletion when stored posts reference the public image URL.
    List<String> referencingPosts = postsRepo.findPostTitlesReferencing(uuid.toString());
    if (!referencingPosts.isEmpty()) {
      ErrorResponses.conflict(ctx, "Image is referenced by post(s): "
          + String.join(", ", referencingPosts) + ".");
      return;
    }
    // END LLM EDIT

    try {
      boolean deleted = mediaRepo.deleteImage(uuid);
      if (!deleted) {
        ErrorResponses.notFound(ctx, "Image not found.");
        return;
      }
    } catch (Exception e) {
      ErrorResponses.serverError(ctx, "Failed to delete image metadata from database.");
      return;
    }

    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      ErrorResponses.serverError(ctx, "Failed to delete image file from storage.");
      return;
    }

    ctx.status(204);
  }
}
