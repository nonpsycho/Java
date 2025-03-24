package com.gnomeland.foodlab.cache;

import com.gnomeland.foodlab.dto.RecipeDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        Objects.requireNonNull(key, "Cache key cannot be null");

        synchronized (cacheMap) {
            // 1. Санитизация ключа перед использованием
            String sanitizedKey = sanitizeKey(key);

            // 2. Создание безопасной версии значения для логов
            String safeLogValue = value != null
                    ? "List[size=" + value.size() + "]"
                    : "null";

            // 3. Безопасное добавление в кэш
            cacheMap.put(sanitizedKey, new CacheEntry(value, System.currentTimeMillis()));

            // 4. Безопасное логирование
            logger.info("Добавлено в кэш: ключ={}, значение={}",
                    sanitizedKey,
                    safeLogValue);
        }
    }

    /**
     * Санитизирует ключ кэша, удаляя потенциально опасные символы.
     */
    private String sanitizeKey(String key) {
        if (key == null) {
            return "";
        }
        // Удаляем специальные символы, которые могут быть использованы для инъекций
        return key.replaceAll("[^a-zA-Z0-9_:.-]", "_");
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