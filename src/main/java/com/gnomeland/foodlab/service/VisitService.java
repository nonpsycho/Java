package com.gnomeland.foodlab.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

@Service
public class VisitService {
    private final ConcurrentHashMap<String, AtomicLong> urlCounters = new ConcurrentHashMap<>();

    public void recordVisit(String url) {
        urlCounters.computeIfAbsent(url, k -> new AtomicLong(0)).incrementAndGet();
    }

    public long getVisitCount(String url) {
        return urlCounters.getOrDefault(url, new AtomicLong(0)).get();
    }

    public Map<String, Long> getAllStats() {
        return urlCounters.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().get()));
    }
}