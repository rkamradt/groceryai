package com.rkamradt.groceryai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fetches nutrition data (per 100 g) from USDA FoodData Central.
 *
 * <p>USDA nutrient IDs used:
 * <ul>
 *   <li>1008 – Energy (kcal)</li>
 *   <li>1003 – Protein (g)</li>
 *   <li>1004 – Total lipid / fat (g)</li>
 *   <li>1005 – Carbohydrate by difference (g)</li>
 *   <li>1079 – Fiber, total dietary (g)</li>
 *   <li>1093 – Sodium (mg)</li>
 * </ul>
 */
@Service
public class UsdaFoodService {

    private static final Logger log = LoggerFactory.getLogger(UsdaFoodService.class);

    private static final int NUTRIENT_ENERGY   = 1008;
    private static final int NUTRIENT_PROTEIN  = 1003;
    private static final int NUTRIENT_FAT      = 1004;
    private static final int NUTRIENT_CARBS    = 1005;
    private static final int NUTRIENT_FIBER    = 1079;
    private static final int NUTRIENT_SODIUM   = 1093;

    private final RestClient restClient;
    private final String apiKey;

    public UsdaFoodService(
            RestClient.Builder restClientBuilder,
            @Value("${usda.api.base-url}") String baseUrl,
            @Value("${usda.api.key}") String apiKey
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
    }

    public record NutritionPer100g(
            Double calories,
            Double protein,
            Double fat,
            Double carbs,
            Double fiber,
            Double sodium
    ) {
        public static NutritionPer100g empty() {
            return new NutritionPer100g(null, null, null, null, null, null);
        }
    }

    // ── USDA response POJOs ──────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsdaSearchResponse(List<UsdaFood> foods) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsdaFood(long fdcId, String description, List<UsdaNutrient> foodNutrients) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record UsdaNutrient(int nutrientId, Double value) {}

    // ── Public API ────────────────────────────────────────────────────────────

    public NutritionPer100g lookupNutrition(String foodName) {
        try {
            String uri = UriComponentsBuilder.fromPath("/foods/search")
                    .queryParam("query", foodName)
                    .queryParam("api_key", apiKey)
                    .queryParam("dataType", "Foundation,SR Legacy")
                    .queryParam("pageSize", 1)
                    .build()
                    .toUriString();

            UsdaSearchResponse response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(UsdaSearchResponse.class);

            if (response == null || response.foods() == null || response.foods().isEmpty()) {
                log.warn("No USDA result for: {}", foodName);
                return NutritionPer100g.empty();
            }

            UsdaFood food = response.foods().get(0);
            log.debug("USDA match for '{}': {}", foodName, food.description());

            Map<Integer, Double> byId = buildNutrientMap(food.foodNutrients());
            return new NutritionPer100g(
                    byId.get(NUTRIENT_ENERGY),
                    byId.get(NUTRIENT_PROTEIN),
                    byId.get(NUTRIENT_FAT),
                    byId.get(NUTRIENT_CARBS),
                    byId.get(NUTRIENT_FIBER),
                    byId.get(NUTRIENT_SODIUM)
            );
        } catch (Exception ex) {
            log.error("USDA lookup failed for '{}': {}", foodName, ex.getMessage());
            return NutritionPer100g.empty();
        }
    }

    private Map<Integer, Double> buildNutrientMap(List<UsdaNutrient> nutrients) {
        if (nutrients == null) return Map.of();
        var map = new java.util.HashMap<Integer, Double>();
        for (UsdaNutrient n : nutrients) {
            if (n.value() != null) {
                map.put(n.nutrientId(), n.value());
            }
        }
        return map;
    }
}
