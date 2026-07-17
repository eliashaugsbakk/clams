package no.eliashaugsbakk.clams.server.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import no.eliashaugsbakk.clams.server.model.ImageMetaData;

public interface MediaRepo {
  void addImage(ImageMetaData image);
  boolean deleteImage(UUID uuid);
  Optional<ImageMetaData> getImage(UUID uuid);
  List<ImageMetaData> getAllImagesMetaData();
}
