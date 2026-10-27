package com.rkamradt.groceryai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkamradt.groceryai.dto.ExtractedGroceryItem;
import com.rkamradt.groceryai.dto.ExtractedGroceryReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Calls the Anthropic Messages API with tool_use to extract structured data
 * from a grocery receipt image.  The model is forced to call the
 * extract_grocery_receipt tool so the response is always well-formed JSON.
 */
@Service
public class AnthropicService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private static final String PROMPT =
        "You are reading a grocery store receipt photo. The receipt may be wrinkled, " +
        "faded, skewed, or partially cut off — read as carefully as possible. " +
        "Extract every line item: the product name, unit price, quantity purchased, " +
        "the unit of measure (e.g. lbs, oz, count, fl oz), and the line total. " +
        "Never invent values: use null when a field is unreadable. " +
        "The total is the final amount paid (after tax and discounts). " +
        "Call the extract_grocery_receipt tool with everything you can read.";

    @Value("${groceryai.anthropic.api-key}")
    private String apiKey;

    @Value("${groceryai.anthropic.model}")
    private String model;

    private final RestClient restClient;

    public AnthropicService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);  // 10 s to establish connection
        factory.setReadTimeout(120_000);    // 2 min for Claude to respond
        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .build();
    }
    private final ObjectMapper mapper = new ObjectMapper();

    public ExtractedGroceryReceipt extract(byte[] jpegBytes) {
        String b64 = Base64.getEncoder().encodeToString(jpegBytes);

        Map<String, Object> request = Map.of(
            "model", model,
            "max_tokens", 2048,
            "tools", List.of(toolDefinition()),
            "tool_choice", Map.of("type", "tool", "name", "extract_grocery_receipt"),
            "messages", List.of(Map.of(
                "role", "user",
                "content", List.of(
                    Map.of(
                        "type", "image",
                        "source", Map.of(
                            "type", "base64",
                            "media_type", "image/jpeg",
                            "data", b64
                        )
                    ),
                    Map.of("type", "text", "text", PROMPT)
                )
            ))
        );

        log.info("Calling Anthropic API: model={} imageBytes={}", model, jpegBytes.length);
        String responseBody;
        try {
            responseBody = restClient.post()
                .uri(API_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(request)
                .retrieve()
                .body(String.class);
        } catch (Exception e) {
            log.error("Anthropic API call failed: {}", e.getMessage(), e);
            throw new RuntimeException("Anthropic API call failed: " + e.getMessage(), e);
        }
        log.info("Anthropic API responded: {} chars", responseBody == null ? 0 : responseBody.length());

        return parseToolUseResponse(responseBody);
    }

    private ExtractedGroceryReceipt parseToolUseResponse(String responseBody) {
        try {
            JsonNode root = mapper.readTree(responseBody);
            for (JsonNode block : root.path("content")) {
                if ("tool_use".equals(block.path("type").asText())
                        && "extract_grocery_receipt".equals(block.path("name").asText())) {
                    JsonNode input = block.path("input");
                    String storeName = input.path("store_name").isNull() ? null : input.path("store_name").asText(null);
                    String date     = input.path("date").isNull()       ? null : input.path("date").asText(null);
                    Double total    = input.path("total").isNull()      ? null : input.path("total").asDouble();
                    String notes    = input.path("confidence_notes").asText("");

                    List<ExtractedGroceryItem> items = new ArrayList<>();
                    for (JsonNode item : input.path("items")) {
                        items.add(new ExtractedGroceryItem(
                            item.path("name").asText(""),
                            item.path("unit_price").isNull()  ? null : item.path("unit_price").asDouble(),
                            item.path("quantity").isNull()    ? null : item.path("quantity").asDouble(),
                            item.path("unit").isNull()        ? null : item.path("unit").asText(null),
                            item.path("line_total").isNull()  ? null : item.path("line_total").asDouble()
                        ));
                    }
                    return new ExtractedGroceryReceipt(storeName, date, items, total, notes);
                }
            }
            log.warn("No tool_use block in Anthropic response");
            return new ExtractedGroceryReceipt(null, null, List.of(), null,
                "Extraction failed — no structured output returned");
        } catch (Exception e) {
            log.error("Failed to parse Anthropic response", e);
            throw new RuntimeException("Failed to parse extraction response", e);
        }
    }

    private Map<String, Object> toolDefinition() {
        return Map.of(
            "name", "extract_grocery_receipt",
            "description", "Extract structured data from a grocery store receipt image",
            "input_schema", Map.of(
                "type", "object",
                "properties", Map.of(
                    "store_name", Map.of(
                        "type", List.of("string", "null"),
                        "description", "Store name, or null if unreadable"
                    ),
                    "date", Map.of(
                        "type", List.of("string", "null"),
                        "description", "Date on the receipt in YYYY-MM-DD format, or null if unreadable"
                    ),
                    "items", Map.of(
                        "type", "array",
                        "description", "Line items on the receipt",
                        "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                "name",       Map.of("type", "string",              "description", "Product name as printed"),
                                "unit_price", Map.of("type", List.of("number", "null"), "description", "Price per unit in dollars"),
                                "quantity",   Map.of("type", List.of("number", "null"), "description", "Quantity purchased (numeric)"),
                                "unit",       Map.of("type", List.of("string", "null"), "description", "Unit of measure: lbs, oz, count, fl oz, etc."),
                                "line_total", Map.of("type", List.of("number", "null"), "description", "Line item total in dollars")
                            ),
                            "required", List.of("name", "unit_price", "quantity", "unit", "line_total")
                        )
                    ),
                    "total", Map.of(
                        "type", List.of("number", "null"),
                        "description", "Final amount paid (after tax and discounts), or null if unreadable"
                    ),
                    "confidence_notes", Map.of(
                        "type", "string",
                        "description", "Any caveats about readability or uncertain values"
                    )
                ),
                "required", List.of("store_name", "date", "items", "total", "confidence_notes")
            )
        );
    }
}
