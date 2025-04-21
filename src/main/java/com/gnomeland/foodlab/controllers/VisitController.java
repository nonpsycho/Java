package com.gnomeland.foodlab.controllers;

import com.gnomeland.foodlab.service.VisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits")
@Tag(name = "Visit Controller",
        description = "API for tracking and retrieving website visit statistics")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @Operation(summary = "Get total visits count",
            description = "Returns total visits for all endpoints")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    })
    @GetMapping("/all")
    public Map<String, Long> getAllStats() {
        return visitService.getAllStats();
    }

    @Operation(summary = "Get visits by endpoint",
            description = "Returns visits count for specific endpoint")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Endpoint not tracked")
    })
    @GetMapping("/count")
    public long getVisitCount(@RequestParam String url) {
        return visitService.getVisitCount(url);
    }
}