package no.eliashaugsbakk.clams.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.eliashaugsbakk.clams.server.model.ImageMetaData;

public class MediaRepoSqlite implements MediaRepo {
  private final SqliteManager manager;

  public MediaRepoSqlite(SqliteManager sqliteManager) {
    this.manager = sqliteManager;
  }

  @Override
  public void addImage(ImageMetaData image) {
    String sql = """
        INSERT INTO images (uuid, original_filename, extension, time_uploaded)
        VALUES (?, ?, ?, ?)
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, image.uuid().toString());
      stmt.setString(2, image.originalFilename());
      stmt.setString(3, image.contentType());
      stmt.setString(4, image.timeUploaded().toString());

      stmt.executeUpdate();

    } catch (SQLException e) {
      throw new RepoException("Error adding image: " + image.uuid(), e);
    }
  }

  @Override
  public boolean deleteImage(UUID uuid) {
    String sql = """
        DELETE FROM images
        WHERE uuid = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, uuid.toString());

      int rowsAffected = stmt.executeUpdate();
      return rowsAffected > 0;

    } catch (SQLException e) {
      throw new RepoException("Error deleting image: " + uuid, e);
    }
  }

  @Override
  public Optional<ImageMetaData> getImage(UUID uuid) {
    String sql = """
        SELECT uuid, original_filename, extension, time_uploaded
        FROM images
        WHERE uuid = ?
        """;

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

      stmt.setString(1, uuid.toString());

      try (ResultSet resultSet = stmt.executeQuery()) {
        if (resultSet.next()) {
          String originalFilename = resultSet.getString("original_filename");
          String extension = resultSet.getString("extension");
          Instant timeUploaded = Instant.parse(resultSet.getString("time_uploaded"));

          return Optional.of(new ImageMetaData(uuid, originalFilename, extension, timeUploaded));
        }
        return Optional.empty();
      }

    } catch (SQLException e) {
      throw new RepoException("Error fetching image: " + uuid, e);
    }
  }

  @Override
  public List<ImageMetaData> getAllImagesMetaData() {
    String sql = """
        SELECT uuid, original_filename, extension, time_uploaded
        FROM images
        """;

    List<ImageMetaData> images = new ArrayList<>();

    try (Connection conn = manager.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet resultSet = stmt.executeQuery()) {

      while (resultSet.next()) {
        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
        String originalFilename = resultSet.getString("original_filename");
        String extension = resultSet.getString("extension");
        Instant timeUploaded = Instant.parse(resultSet.getString("time_uploaded"));

        images.add(new ImageMetaData(uuid, originalFilename, extension, timeUploaded));
      }

      return images;

    } catch (SQLException e) {
      throw new RepoException("Error fetching all images metadata", e);
    }
  }
}
