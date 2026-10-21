package com.rkamradt.groceryai.dto;

public record ItemNutritionDto(
        String name,
        String category,
        double servingSizeG,
        int servingsPerWeek,
        double weeklyCalories,
        double weeklyProtein,
        double weeklyFat,
        double weeklyCarbs,
        double weeklyFiber,
        double weeklySodium,
        Double price
) {}
