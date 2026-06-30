package no.eliashaugsbakk.uploader.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import net.coobird.thumbnailator.Thumbnails;
import no.eliashaugsbakk.uploader.config.Config;
import no.eliashaugsbakk.uploader.exception.UploaderException;
import no.eliashaugsbakk.uploader.model.TextFile;
import no.eliashaugsbakk.utils.Image;

public class DataNormalizerService {
  private static final Set<String> SUPPORTED_IMAGE_EXTENSIONS =
      Set.of(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".wbmp");

  private TextFile textFile = null;
  private final List<Image> normalizedImageFiles;
  private final int maxSize;
  private final float quality;

  public DataNormalizerService(List<String> filePaths) throws IOException {
    this.maxSize = Config.getInstance().getMaxImageSize();
    this.quality = Config.getInstance().getImageQuality();
    List<Image> imageFiles = new ArrayList<>();
    normalizedImageFiles = new ArrayList<>();

    boolean markdownSeen = false;
    for (String s : filePaths) {
      Path path = Path.of(s);
      String fileName = path.getFileName().toString();
      String extension = getFileExtension(fileName);

      if (extension.equalsIgnoreCase(".md")) {
        if (markdownSeen) {
          throw new UploaderException(
              "Multiple .md files found. Only one markdown file is supported.");
        }
        markdownSeen = true;
        textFile = new TextFile(fileName, Files.readString(path));
      } else if (SUPPORTED_IMAGE_EXTENSIONS.contains(extension.toLowerCase())) {
        byte[] fileData = Files.readAllBytes(path);
        imageFiles.add(new Image(fileName, fileData));
      } else {
        throw new UploaderException(
            "Unsupported file extension: \"" + fileName + "\". Supported: " +
                SUPPORTED_IMAGE_EXTENSIONS + " and .md");
      }
    }

    if (!markdownSeen) {
      throw new UploaderException("No .md file found. Exactly one markdown file is required.");
    }

    // Normalize and collect all images
    for (Image image : imageFiles) {
      try {
        normalizedImageFiles.add(normalizeImage(image));
      } catch (IOException e) {
        throw new UploaderException(
            "Error processing image file '" + image.title() + "': " + e.getMessage(), e);
      }
    }

    // Verify all normalized image names are referenced in the markdown
    verifyImageNaming(normalizedImageFiles, textFile.body());
  }

  public TextFile getTextFile() {
    return textFile;
  }

  public List<Image> getImagesFiles() {
    return normalizedImageFiles;
  }

  /**
   * Extract file extension (lowercase, with leading dot).
   *
   * @param fileName the file name
   * @return the extension (e.g., ".jpg"), or empty string if no extension
   */
  private String getFileExtension(String fileName) {
    int lastDot = fileName.lastIndexOf('.');
    if (lastDot <= 0) {
      return "";
    }
    return fileName.substring(lastDot).toLowerCase(Locale.ROOT);
  }

  /**
   * Verify that each normalized image is referenced by name in the markdown. Uses word boundaries
   * to avoid partial matches.
   *
   * @param images          the normalized images
   * @param markdownContent the Markdown body text
   * @throws UploaderException if an image is not referenced
   */
  private void verifyImageNaming(List<Image> images, String markdownContent) {
    for (Image image : images) {
      String imageName = image.title();
      // Use word boundaries to match only whole filenames
      String regex = "(?<!\\S)" + Pattern.quote(imageName) + "(?!\\S)";
      boolean found = Pattern.compile(regex).matcher(markdownContent).find();
      if (!found) {
        throw new UploaderException(
            "Image '" + imageName + "' is not referenced in the markdown file.");
      }
    }
  }

  /**
   * Normalize an image: resize, convert format based on alpha channel, apply quality settings, and
   * update markdown references.
   *
   * @param imageToNormalize the image to normalize
   * @return a new Image with normalized data and updated filename
   * @throws IOException if image processing fails
   */
  private Image normalizeImage(Image imageToNormalize) throws IOException {
    BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageToNormalize.data()));
    if (originalImage == null) {
      throw new IOException("Failed to decode image: " + imageToNormalize.title());
    }

    boolean hasAlpha = originalImage.getColorModel().hasAlpha();
    String outputFormat = hasAlpha ? "png" : "jpg";

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Thumbnails.Builder<BufferedImage> builder =
        Thumbnails.of(originalImage).size(maxSize, maxSize).outputFormat(outputFormat);

    if (!hasAlpha) {
      builder.outputQuality(quality);
    }

    builder.toOutputStream(outputStream);

    String newImageName = getNewImageName(imageToNormalize.title());
    return new Image(newImageName, outputStream.toByteArray());
  }

  /**
   * Generate a normalized filename: convert all image extensions to .jpg (or keep .png if has
   * transparency). Updates markdown references.
   *
   * @param oldImageName the original filename
   * @return the new filename (e.g., "photo.bmp" -> "photo.jpg")
   */
  private String getNewImageName(String oldImageName) {
    // Strip any known image extension and replace with .jpg (or .png for transparency)
    String newImageName = oldImageName.replaceAll("(?i)\\.(jpg|jpeg|png|gif|bmp|wbmp)$", ".jpg");
    updateMarkdownLinks(oldImageName, newImageName);
    return newImageName;
  }

  /**
   * Update markdown to replace old image name with new image name. Uses regex with word boundaries
   * to avoid replacing partial matches.
   *
   * @param oldName the old filename
   * @param newName the new filename
   */
  private void updateMarkdownLinks(String oldName, String newName) {
    String regex = "(?<!\\S)" + Pattern.quote(oldName) + "(?!\\S)";
    String updatedBody = Pattern.compile(regex).matcher(textFile.body()).replaceAll(newName);
    textFile = new TextFile(textFile.title(), updatedBody);
  }
}
