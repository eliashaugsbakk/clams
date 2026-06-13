package no.eliashaugsbakk.webserver.service;

import no.eliashaugsbakk.utils.Image;
import no.eliashaugsbakk.webserver.db.DataAccessException;
import no.eliashaugsbakk.webserver.db.PageRepository;
import no.eliashaugsbakk.webserver.model.Page;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;

public class PostStorageService {
    private final String imageDir = "/app/data/images";
    private final PageRepository pageRepo;

    public PostStorageService(PageRepository pageRepo) {
        this.pageRepo = pageRepo;
    }

    public boolean createPage(String title, String html, List<Image> images) {
        if (title.endsWith(".md")) {
            title = title.replace(".md", "");
        }
        try {
            String slug = generateSlug(title);


            html = storeImages(images, slug, html);

            Instant createdAt = Instant.now();

            return pageRepo.addPage(new Page(
                    slug,
                    title,
                    createdAt,
                    html
            ));

        } catch (DataAccessException e) {
            System.err.println("Error while trying to save page: " + e.getMessage());
            return false;
        }
    }

    private String storeImages(List<Image> images, String slug, String html) {
        File directory = new File(imageDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            if (images.isEmpty()) return html;

            String newHtml = html;
            for (Image image : images) {
                String safeName = slug + "-" + image.title();

                newHtml = newHtml.replace(image.title(), "/static/images/" + safeName);

                Path destination = Paths.get(imageDir,  safeName);
                Files.write(destination, image.data());
            }
            return newHtml;
        } catch (Exception e) {
            System.err.println("Error while trying to save image: " + e.getMessage());
            return null;
        }
    }


    private String generateSlug(String title) {
        String base = title.toLowerCase()
                .replaceAll("æ", "ae")
                .replaceAll("ø", "oe")
                .replaceAll("å", "aa")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String slug = base;
        int counter = 2;
        while (pageRepo.slugExists(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

}
