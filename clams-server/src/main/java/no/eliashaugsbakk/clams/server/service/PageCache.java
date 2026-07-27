package no.eliashaugsbakk.clams.server.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PageCache {
  private final Map<String, String> cache;

  /**
   * Least Recently Used Cache
   */
  public PageCache(int capacity) {

    LinkedHashMap<String, String> map = new LinkedHashMap<>(capacity, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
        return size() > capacity;
      }
    };

    // Wrap with synchronizedMap to make thread-safe
    this.cache = Collections.synchronizedMap(map);
  }

  public String get(String url) {
    return cache.get(url);
  }

  public void put(String url, String html) {
    cache.put(url, html);
  }

  public void clear() {
    cache.clear();
  }
}
