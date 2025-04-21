package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.service.AsyncLogService;
import com.gnomeland.foodlab.service.AsyncLogService.LogFileResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/logs")
@Tag(name = "Log Controller Async", description = "Allows you to work with logs asynchronously")
public class AsyncLogController {

    private final AsyncLogService asyncLogService;

    public AsyncLogController(AsyncLogService asyncLogService) {
        this.asyncLogService = asyncLogService;
    }

    @Operation(summary = "Creating a log file",
            description = "Starts the asynchronous process of creating a log file.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "The process is running"),
    })
    @PostMapping("/async")
    public ResponseEntity<Map<String, String>> requestLogsAsync(
            @RequestParam
            @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Date must be in format yyyy-MM-dd")
            String date) {
        String taskId = asyncLogService.createLogTask(date);
        return ResponseEntity.accepted().body(Map.of("taskId", taskId));
    }

    @Operation(summary = "Getting a status",
            description = "Displays the status of the current process,"
                    + " if it has completed, displays the time until it is deleted.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Process status output"),
        @ApiResponse(responseCode = "404", description = "The process was not found"),
    })
    @GetMapping("/async/status/{taskId}")
    public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
        try {
            return ResponseEntity.ok(asyncLogService.getTaskStatus(taskId));
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Getting the log file",
            description = "Returns the log file")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "202", description = "The process is running"),
        @ApiResponse(responseCode = "204",
                description = "Logs for the proposed date were not found"),
        @ApiResponse(responseCode = "404", description = "There is no such process."),
    })
    @GetMapping("/async/file/{taskId}")
    public ResponseEntity<ByteArrayResource> getLogFile(@PathVariable String taskId) {
        try {
            LogFileResult result = asyncLogService.getLogFile(taskId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + result.getFilename())
                    .contentType(MediaType.TEXT_PLAIN)
                    .contentLength(result.getContentLength())
                    .body(result.getResource());
        } catch (NoSuchElementException ex) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
    }
}
