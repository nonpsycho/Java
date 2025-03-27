package com.gnomeland.foodlab.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryCache {

    private static final int MAX_CACHE_SIZE = 3;
    private final Map<String, Object> cacheMap;
    private static final Logger logger = LoggerFactory.getLogger(InMemoryCache.class);

    public InMemoryCache() {
        this.cacheMap = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                boolean shouldRemove = size() > MAX_CACHE_SIZE;
                if (shouldRemove) {
                    logger.info("Удаление старейшей записи из кэша: ключ={}, значение={}",
                            eldest.getKey(), eldest.getValue());
                }
                return shouldRemove;
            }
        };
    }

    public void put(String key, Object value) {
        synchronized (cacheMap) {
            cacheMap.put(key, value);
            logger.info("Добавлено в кэш: ключ={}, значение={}\n", key, value);
        }
    }

    public Optional<Object> get(String key) {
        synchronized (cacheMap) {
            Object value = cacheMap.get(key);
            if (value != null) {
                logger.info("Попадание в кэш: ключ={}, значение={}", key, value);
            } else {
                logger.info("Промах кэша: ключ={}", key);
            }
            return Optional.ofNullable(value);
        }
    }

    public void remove(String key) {
        synchronized (cacheMap) {
            Object removedValue = cacheMap.remove(key);
            if (removedValue != null) {
                logger.info("Удалено из кэша: ключ={}, значение={}\n", key, removedValue);
            } else {
                logger.info("Попытка удалить несуществующий ключ из кэша: ключ={}\n", key);
            }
        }
    }

    public void removeAll() {
        synchronized (cacheMap) {
            cacheMap.clear();
            logger.info("Кеш был очищен");
        }
    }

    public int size() {
        synchronized (cacheMap) {
            int size = cacheMap.size();
            logger.info("Текущий размер кэша: {}", size);
            return size;
        }
    }

    public boolean contains(String key) {
        synchronized (cacheMap) {
            return cacheMap.containsKey(key);
        }
    }
}

