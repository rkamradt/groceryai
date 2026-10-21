package com.rkamradt.groceryai.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.rkamradt.groceryai.dto.NutritionReportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a weekly nutrition + cost report to Google Sheets.
 * Each export creates a new tab named by week start date.
 */
@Service
public class GoogleSheetsService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSheetsService.class);
    private static final String APP_NAME = "groceryai.kamradtfamily.net";

    @Value("${groceryai.google.sheet-id}")
    private String sheetId;

    @Value("${groceryai.google.sheet-tab}")
    private String baseTab;

    public void exportReport(NutritionReportDto report, LocalDate weekStart,
                             String accessToken)
            throws IOException, GeneralSecurityException {

        HttpRequestInitializer creds = credentialsAdapter(accessToken);
        Sheets sheets = new Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            creds)
            .setApplicationName(APP_NAME)
            .build();

        String tab = baseTab + " " + weekStart;
        ensureTab(sheets, tab);

        List<List<Object>> rows = new ArrayList<>();

        // Header row
        rows.add(List.of(
            "Item", "Category", "Price ($)", "Serving (g)", "Servings/Wk",
            "Calories", "Protein (g)", "Fat (g)", "Carbs (g)", "Fiber (g)", "Sodium (mg)"
        ));

        // Item rows
        for (var item : report.items()) {
            rows.add(List.of(
                item.name(),
                item.category(),
                item.price() != null ? item.price() : "",
                item.servingSizeG(),
                item.servingsPerWeek(),
                item.weeklyCalories(),
                item.weeklyProtein(),
                item.weeklyFat(),
                item.weeklyCarbs(),
                item.weeklyFiber(),
                item.weeklySodium()
            ));
        }

        // Blank separator
        rows.add(List.of());

        // Weekly totals summary
        var wt = report.weeklyTotal();
        rows.add(List.of("WEEKLY TOTAL", "", report.totalCost() != null ? report.totalCost() : "",
            "", "", wt.calories(), wt.protein(), wt.fat(), wt.carbs(), wt.fiber(), wt.sodium()));

        var ppd = report.perPersonPerDay();
        rows.add(List.of(
            "Per person/day (" + report.people() + " people)", "",
            report.perPersonCost() != null ? report.perPersonCost() : "",
            "", "", ppd.calories(), ppd.protein(), ppd.fat(), ppd.carbs(), ppd.fiber(), ppd.sodium()
        ));

        ValueRange body = new ValueRange().setValues(rows);
        sheets.spreadsheets().values()
            .update(sheetId, tab + "!A1", body)
            .setValueInputOption("USER_ENTERED")
            .execute();

        log.info("Exported nutrition report to sheet {} tab '{}'", sheetId, tab);
    }

    private void ensureTab(Sheets sheets, String tabName) throws IOException, GeneralSecurityException {
        // Try to clear/reuse if exists; if not, it will be created on first write via USER_ENTERED mode.
        // Sheets API auto-creates named ranges on update only if the sheet tab already exists;
        // so we use batchUpdate to add the sheet if needed.
        try {
            var spreadsheet = sheets.spreadsheets().get(sheetId).execute();
            boolean exists = spreadsheet.getSheets().stream()
                .anyMatch(s -> tabName.equals(s.getProperties().getTitle()));
            if (!exists) {
                var addSheet = new com.google.api.services.sheets.v4.model.AddSheetRequest();
                addSheet.setProperties(new com.google.api.services.sheets.v4.model.SheetProperties()
                    .setTitle(tabName));
                var req = new com.google.api.services.sheets.v4.model.Request()
                    .setAddSheet(addSheet);
                sheets.spreadsheets().batchUpdate(sheetId,
                    new com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest()
                        .setRequests(List.of(req)))
                    .execute();
                log.info("Created sheet tab '{}'", tabName);
            }
        } catch (Exception e) {
            log.warn("Could not ensure tab '{}': {}", tabName, e.getMessage());
        }
    }

    private HttpRequestInitializer credentialsAdapter(String token) {
        GoogleCredentials creds = GoogleCredentials.create(new AccessToken(token, null));
        return new HttpCredentialsAdapter(creds);
    }
}
