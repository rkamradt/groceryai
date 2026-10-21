package com.rkamradt.groceryai.dto;

import java.util.List;

public record NutritionReportDto(
        NutritionSummaryDto weeklyTotal,
        NutritionSummaryDto perPersonPerDay,
        List<CategoryCaloriesDto> categoryCalories,
        List<ItemNutritionDto> items,
        int people,
        Double totalCost,
        Double perPersonCost
) {}
