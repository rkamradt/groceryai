package com.rkamradt.groceryai.dto;

public record NutritionSummaryDto(
        double calories,
        double protein,
        double fat,
        double carbs,
        double fiber,
        double sodium
) {}
