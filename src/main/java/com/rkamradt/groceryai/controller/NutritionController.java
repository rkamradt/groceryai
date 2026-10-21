package com.rkamradt.groceryai.controller;

import com.rkamradt.groceryai.dto.ApiResponse;
import com.rkamradt.groceryai.dto.NutritionReportDto;
import com.rkamradt.groceryai.service.GoogleSheetsService;
import com.rkamradt.groceryai.service.NutritionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/nutrition")
public class NutritionController {

    private static final Logger log = LoggerFactory.getLogger(NutritionController.class);

    private final NutritionService nutritionService;
    private final GoogleSheetsService sheetsService;
    private final OAuth2AuthorizedClientService clientService;

    public NutritionController(NutritionService nutritionService,
                               GoogleSheetsService sheetsService,
                               OAuth2AuthorizedClientService clientService) {
        this.nutritionService = nutritionService;
        this.sheetsService = sheetsService;
        this.clientService = clientService;
    }

    @GetMapping("/{weekId}")
    public ResponseEntity<ApiResponse<NutritionReportDto>> report(@PathVariable UUID weekId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(nutritionService.buildReport(weekId)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{weekId}/export")
    public ResponseEntity<ApiResponse<String>> export(
            @PathVariable UUID weekId,
            @RequestParam(value = "weekStart", required = false) String weekStartParam,
            Authentication auth) {
        try {
            NutritionReportDto report = nutritionService.buildReport(weekId);
            LocalDate weekStart = weekStartParam != null
                ? LocalDate.parse(weekStartParam)
                : LocalDate.now();

            String accessToken = getGoogleAccessToken(auth);
            sheetsService.exportReport(report, weekStart, accessToken);

            return ResponseEntity.ok(ApiResponse.ok("Report exported to Google Sheets."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to export report for week {}", weekId, e);
            return ResponseEntity.internalServerError()
                .body(new ApiResponse<>(null, "error", "Export failed: " + e.getMessage()));
        }
    }

    private String getGoogleAccessToken(Authentication auth) {
        OAuth2AuthorizedClient client =
            clientService.loadAuthorizedClient("google", auth.getName());
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Google access token not available — please sign in again");
        }
        var expiry = client.getAccessToken().getExpiresAt();
        if (expiry != null && expiry.isBefore(java.time.Instant.now())) {
            throw new IllegalStateException("Google access token expired — please sign in again");
        }
        return client.getAccessToken().getTokenValue();
    }
}
