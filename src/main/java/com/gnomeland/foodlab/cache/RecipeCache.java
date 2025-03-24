package com.gnomeland.foodlab.cache;

import com.gnomeland.foodlab.dto.RecipeDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RecipeCache {
    private static final int MAX_CACHE_SIZE = 5; // Максимальный размер кэша
    private static final long TTL = TimeUnit.MINUTES.toMillis(10); // Время жизни записи (10 минут)
    private static final Logger logger = LoggerFactory.getLogger(RecipeCache.class);

    private final Map<String, CacheEntry> cacheMap;


    public RecipeCache() {
        this.cacheMap = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                boolean shouldRemove = size() > MAX_CACHE_SIZE || isExpired(eldest.getValue());
                if (shouldRemove) {
                    logger.info("Удаление записи из кэша: ключ={}, значение={}, причина={}",
                            eldest.getKey(), eldest.getValue(),
                            size() > MAX_CACHE_SIZE
                                    ? "превышение размера" : "истекшее время жизни");
                }
                return shouldRemove;
            }
        };
    }

    /**
     * Добавляет запись в кэш.
     *
     * @param key   Ключ записи.
     * @param value Значение записи.
     */
    public void put(String key, List<RecipeDto> value) {
        synchronized (cacheMap) {
            cacheMap.put(key, new CacheEntry(value, System.currentTimeMillis()));
            logger.info("Добавлено в кэш: ключ={}, значение={}", key, value);
        }
    }

    /**
     * Получает запись из кэша по ключу.
     *
     * @param key Ключ записи.
     * @return Значение записи, если оно есть и не истекло.
     */
    public Optional<List<RecipeDto>> get(String key) {
        synchronized (cacheMap) {
            CacheEntry entry = cacheMap.get(key);
            if (entry != null) {
                if (isExpired(entry)) {
                    cacheMap.remove(key);
                    logger.info("Запись удалена из кэша: ключ={},"
                            + " причина=истекшее время жизни", key);
                    return Optional.empty();
                }
                logger.info("Попадание в кэш: ключ={}, значение={}", key, entry.value());
                return Optional.of(entry.value());
            } else {
                logger.info("Промах кэша: ключ={}", key);
                return Optional.empty();
            }
        }
    }

    /**
     * Удаляет запись из кэша по ключу.
     *
     * @param key Ключ записи.
     */
    public void remove(String key) {
        synchronized (cacheMap) {
            CacheEntry removedEntry = cacheMap.remove(key);
            if (removedEntry != null) {
                logger.info("Удалено из кэша: ключ={}, значение={}", key, removedEntry.value());
            } else {
                logger.info("Попытка удалить несуществующий ключ из кэша: ключ={}", key);
            }
        }
    }

    /**
     * Очищает кэш.
     */
    public void clear() {
        synchronized (cacheMap) {
            cacheMap.clear();
            logger.info("Кэш очищен");
        }
    }

    /**
     * Возвращает текущий размер кэша.
     *
     * @return Размер кэша.
     */
    public int size() {
        synchronized (cacheMap) {
            int size = cacheMap.size();
            logger.info("Текущий размер кэша: {}", size);
            return size;
        }
    }

    /**
     * Проверяет, содержит ли кэш запись с указанным ключом.
     *
     * @param key Ключ записи.
     * @return true, если запись есть и она не истекла, иначе false.
     */
    public boolean contains(String key) {
        synchronized (cacheMap) {
            CacheEntry entry = cacheMap.get(key);
            if (entry != null && isExpired(entry)) {
                cacheMap.remove(key);
                return false;
            }
            return cacheMap.containsKey(key);
        }
    }

    /**
     * Проверяет, истекло ли время жизни записи.
     *
     * @param entry Запись кэша.
     * @return true, если время жизни истекло, иначе false.
     */
    private boolean isExpired(CacheEntry entry) {
        return System.currentTimeMillis() - entry.timestamp() > TTL;
    }

    /**
     * Внутренний класс для хранения записи кэша.
     */
    private record CacheEntry(List<RecipeDto> value, long timestamp) {
        @Override
        public String toString() {
            return "CacheEntry{"
                    + "value=" + value
                    + ", timestamp=" + timestamp
                    + '}';
        }
    }

    public void removeByPrefix(String prefix) {
        synchronized (cacheMap) {
            cacheMap.keySet().removeIf(key -> key.startsWith(prefix));
            logger.info("Удалены записи из кэша с префиксом: {}", prefix);
        }
    }
}