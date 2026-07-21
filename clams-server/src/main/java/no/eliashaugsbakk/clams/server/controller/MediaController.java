package no.eliashaugsbakk.clams.server.controller;

import io.javalin.http.Context;
import io.javalin.http.UploadedFile;
import java.io.ByteArrayInputStream;
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
import org.apache.commons.imaging.ImageFormat;
import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageInfo;
import org.apache.commons.imaging.Imaging;

public class MediaController {
  private final MediaRepo mediaRepo;
  private final AppConfig appConfig;

  public MediaController(MediaRepo mediaRepo, AppConfig appConfig) {
    this.mediaRepo = mediaRepo;
    this.appConfig = appConfig;
  }

  public void handlePostMedia(Context ctx) {
    UploadedFile file = ctx.uploadedFile("image");

    if (file == null) {
      ctx.status(400).result("Missing image file payload.");
      return;
    }

    try (InputStream is = file.content()) {
      byte[] imageBytes = is.readAllBytes();

      try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
        ImageInfo info = Imaging.getImageInfo(bais, file.filename());
        ImageFormat format = info.getFormat();

        if (format != ImageFormats.JPEG) {
          ctx.status(415).result("Unsupported format: only jpeg allowed.");
          return;
        }

        if (info.getWidth() > 2000 || info.getHeight() > 2000) {
          ctx.status(400).result("Image dimensions are too large: max allowed 2000x2000");
          return;
        }
      }

      UUID generatedUuid = saveToStorage(imageBytes, file.filename(), file.contentType());

      ctx.status(201).json(Map.of("uuid", generatedUuid, "url", "/api/media/" + generatedUuid));
    } catch (Exception e) {
      ctx.status(400).result("Corrupted or invalid image data.");
    }
  }

  private UUID saveToStorage(byte[] bytes, String originalFilename, String contentType)
      throws IOException {
    UUID uuid = UUID.randomUUID();
    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");
    Files.createDirectories(path.getParent());
    Files.write(path, bytes);

    try {
      mediaRepo.addImage(new ImageMetaData(uuid, originalFilename, contentType, Instant.now()));
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
      ctx.status(400).result("Invalid UUID format.");
      return;
    }

    if (mediaRepo.getImage(uuid).isEmpty()) {
      ctx.status(404).result("Image not found.");
      return;
    }

    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");

    if (Files.exists(path)) {
      ctx.contentType("image/jpeg");

      try {
        ctx.result(Files.newInputStream(path));
      } catch (IOException e) {
        ctx.status(500).result("Error reading image file.");
      }
    } else {
      ctx.status(404).result("Image file missing from storage.");
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
      ctx.status(400).result("Invalid UUID format.");
      return;
    }

    try {
      boolean deleted = mediaRepo.deleteImage(uuid);
      if (!deleted) {
        ctx.status(404).result("Image not found.");
        return;
      }
    } catch (Exception e) {
      ctx.status(500).result("Failed to delete image metadata from database.");
      return;
    }

    Path path = Path.of(appConfig.getStorageLocation(), "images", uuid + ".jpeg");
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      ctx.status(500).result("Failed to delete image file from storage.");
      return;
    }

    ctx.status(204);
  }
}
