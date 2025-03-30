package com.gnomeland.foodlab.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/logs")
@Tag(name = "Log Controller", description = "Позволяет получать логи на введенную дату")
public class LogController {

    private static final String LOG_FILE_PATH = "logs/application.log"; // основной лог файл

    @Operation(summary = "Получение информации о логах",
            description = "Возвращает информацию о логах по дате")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Логи найдены"),
        @ApiResponse(responseCode = "404", description = "Логи за предложенную дату не найдены")
    })
    @GetMapping("/{date}")
    public ResponseEntity<Resource> getLogsByDate(@PathVariable String date) throws IOException {
        List<String> logs = new ArrayList<>();

        if (isToday(date)) {
            File logFile = new File(LOG_FILE_PATH);
            if (logFile.exists()) {
                logs.addAll(readLogsFromFile(logFile));
            }
        }

        File archiveLogFile = getArchiveLogFile(date);
        if (archiveLogFile.exists()) {
            logs.addAll(readLogsFromGzip(archiveLogFile));
        }

        List<String> filteredLogs = filterLogsByDate(logs, date);

        if (filteredLogs.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        File tempLogFile = createTempLogFile(filteredLogs, date);

        return createFileResponse(tempLogFile);
    }

    private boolean isToday(String date) {
        String today = LocalDate.now().toString();
        return today.equals(date);
    }

    private List<String> readLogsFromFile(File logFile) throws IOException {
        List<String> logs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        }
        return logs;
    }

    private List<String> readLogsFromGzip(File gzipFile) throws IOException {
        List<String> logs = new ArrayList<>();
        try (GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzipFile));
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     gzipInputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        }
        return logs;
    }

    private List<String> filterLogsByDate(List<String> logs, String date) {
        List<String> filteredLogs = new ArrayList<>();
        for (String log : logs) {
            if (log.startsWith(date) && !log.contains("INFO - Вход в метод контроллера")) {
                filteredLogs.add(log);
            }
        }
        return filteredLogs;
    }

    private File getArchiveLogFile(String date) {
        String archiveLogFilePath = LOG_FILE_PATH + "." + date + ".0.gz";
        return new File(archiveLogFilePath);
    }

    private File createTempLogFile(List<String> logs, String date) throws IOException {
        String safeDate = sanitizeFilename(date);

        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize();

        if (!Files.isDirectory(tempDir) || Files.isSymbolicLink(tempDir)) {
            throw new IOException("Unsafe temporary directory: " + tempDir);
        }

        Path tempFile = Files.createTempFile(tempDir, "logs-" + safeDate + "-", ".log");

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8,
                StandardOpenOption.WRITE)) {
            for (String log : logs) {
                writer.write(log);
                writer.newLine();
            }
        }

        tempFile.toFile().deleteOnExit();

        return tempFile.toFile();
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unknown";
        }

        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private ResponseEntity<Resource> createFileResponse(File file) throws IOException {
        Path path = Paths.get(file.getAbsolutePath());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
