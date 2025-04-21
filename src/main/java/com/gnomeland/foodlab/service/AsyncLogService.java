package com.gnomeland.foodlab.service;

import com.gnomeland.foodlab.exception.LogNotReadyException;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class AsyncLogService {
    private static final long TASK_TTL_MINUTES = 5;
    private final Map<String, TaskWrapper> tasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor =
            Executors.newSingleThreadScheduledExecutor();

    @Value("${logging.file.name}")
    private String logFilePath;

    public AsyncLogService() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredTasks, 1, 1, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdownExecutor() {
        cleanupExecutor.shutdown();
    }

    public String createLogTask(String date) {
        String taskId = UUID.randomUUID().toString();
        CompletableFuture<LogFileResult> future =
                CompletableFuture.supplyAsync(() -> processLogs(date));
        tasks.put(taskId, new TaskWrapper(future));
        return taskId;
    }

    public Map<String, Object> getTaskStatus(String taskId) {
        TaskWrapper wrapper = tasks.get(taskId.trim());
        if (wrapper == null || wrapper.isExpired()) {
            throw new NoSuchElementException("Task completed or not found");
        }
        return wrapper.getStatus();
    }

    public LogFileResult getLogFile(String taskId) {
        TaskWrapper wrapper = tasks.get(taskId.trim());
        if (wrapper == null || wrapper.isExpired()) {
            throw new NoSuchElementException("The issue was not found or outdated");
        }
        if (!wrapper.future.isDone()) {
            throw new LogNotReadyException("Wait, the file has not been created yet.");
        }
        return wrapper.getResult();
    }


    private LogFileResult processLogs(String date) {
        try {
            Thread.sleep(30_000);
            List<String> logs = readLogsByDate(date);
            String content = String.join("\n", logs);
            ByteArrayResource resource = new ByteArrayResource(content
                    .getBytes(StandardCharsets.UTF_8)) {
                @Override
                public String getFilename() {
                    return "logs_" + date + ".log";
                }
            };
            return new LogFileResult(resource, logs.isEmpty());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("The processing of the log file was "
                    + "interrupted on the date: " + date, e);
        } catch (Exception e) {
            throw new CompletionException("Error processing the log file on the date: " + date, e);
        }
    }


    private List<String> readLogsByDate(String date) throws IOException {
        List<String> logs = new ArrayList<>();
        LocalDate targetDate = LocalDate.parse(date);

        File archiveLog = new File(logFilePath + "." + date + ".0.gz");
        if (archiveLog.exists()) {
            logs.addAll(readFromGzip(archiveLog));
        }

        if (targetDate.equals(LocalDate.now())) {
            File currentLog = new File(logFilePath);
            if (currentLog.exists()) {
                logs.addAll(readFromFile(currentLog));
            }
        }

        return logs.stream()
                .filter(line -> line.startsWith(date)
                        && !line.contains("INFO - Input to the controller method"))
                .toList();
    }

    private List<String> readFromGzip(File file) throws IOException {
        try (GZIPInputStream is = new GZIPInputStream(new FileInputStream(file));
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(is, StandardCharsets.UTF_8))) {
            List<String> logs = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
            return logs;
        }
    }

    private List<String> readFromFile(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String> logs = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
            return logs;
        }
    }

    private void cleanupExpiredTasks() {
        tasks.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public record LogFileResult(ByteArrayResource resource, boolean empty) {
        public ByteArrayResource getResource() {
            if (empty) {
                throw new IllegalStateException("The log is empty");
            }
            return resource;
        }

        public long getContentLength() {
            return empty ? 0 : resource.contentLength();
        }

        public String getFilename() {
            return resource.getFilename();
        }
    }

    private static class TaskWrapper {
        private final CompletableFuture<LogFileResult> future;
        private volatile long expiration = Long.MAX_VALUE;
        private boolean completed = false;

        TaskWrapper(CompletableFuture<LogFileResult> future) {
            this.future = future;
            future.whenComplete((res, ex) -> {
                this.completed = true;
                this.expiration = System.currentTimeMillis()
                        + TimeUnit.MINUTES.toMillis(TASK_TTL_MINUTES);
            });
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiration;
        }

        LogFileResult getResult() {
            try {
                return future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Task aborted", e);
            } catch (ExecutionException e) {
                throw new IllegalStateException("Issue error", e.getCause());
            }
        }

        Map<String, Object> getStatus() {
            Map<String, Object> map = new HashMap<>();
            map.put("isCompleted", future.isDone());
            if (completed) {
                map.put("expiresIn", Math.max(0, (expiration - System.currentTimeMillis()) / 1000));
            } else {
                map.put("expiresIn", "will be define");
            }
            return map;
        }
    }
}
