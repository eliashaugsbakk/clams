package no.eliashaugsbakk.webserver.db;

import no.eliashaugsbakk.webserver.model.Page;
import no.eliashaugsbakk.webserver.model.PageMetadata;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PageRepository {
    Optional<Page> findPageBySlug(String slug);
    List<PageMetadata> getAllPageMetadata();
    Optional<PageMetadata> findPreviousPageMetadata(String slug);
    Optional<PageMetadata> findNextPageMetadata(String slug);
    boolean addPage(Page page);
    List<PageMetadata> searchMetadataInTitle(String query);
    Set<String> getAllSlugs();
    boolean slugExists(String slug);
}
