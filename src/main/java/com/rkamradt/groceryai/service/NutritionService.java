package com.rkamradt.groceryai.service;

import com.rkamradt.groceryai.dto.CategoryCaloriesDto;
import com.rkamradt.groceryai.dto.ItemNutritionDto;
import com.rkamradt.groceryai.dto.NutritionReportDto;
import com.rkamradt.groceryai.dto.NutritionSummaryDto;
import com.rkamradt.groceryai.model.GroceryItem;
import com.rkamradt.groceryai.model.Week;
import com.rkamradt.groceryai.repository.GroceryItemRepository;
import com.rkamradt.groceryai.repository.WeekRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NutritionService {

    private final GroceryItemRepository itemRepository;
    private final WeekRepository weekRepository;

    public NutritionService(GroceryItemRepository itemRepository, WeekRepository weekRepository) {
        this.itemRepository = itemRepository;
        this.weekRepository = weekRepository;
    }

    public NutritionReportDto buildReport(UUID weekId) {
        Week week = weekRepository.findById(weekId)
                .orElseThrow(() -> new NoSuchElementException("Week not found: " + weekId));

        List<GroceryItem> items = itemRepository.findByWeekId(weekId);

        List<ItemNutritionDto> itemDtos = items.stream()
                .map(this::toItemDto)
                .toList();

        NutritionSummaryDto weeklyTotal = sum(itemDtos);
        NutritionSummaryDto perPersonPerDay = divide(weeklyTotal, week.getPeople() * 7.0);

        List<CategoryCaloriesDto> categoryCalories = itemDtos.stream()
                .collect(Collectors.groupingBy(ItemNutritionDto::category,
                        Collectors.summingDouble(ItemNutritionDto::weeklyCalories)))
                .entrySet().stream()
                .map(e -> new CategoryCaloriesDto(e.getKey(), round(e.getValue())))
                .sorted(Comparator.comparingDouble(CategoryCaloriesDto::calories).reversed())
                .toList();

        Double totalCost = week.getReceiptTotal();
        Double perPersonCost = (totalCost != null && week.getPeople() > 0)
                ? Math.round(totalCost / week.getPeople() * 100.0) / 100.0
                : null;

        return new NutritionReportDto(weeklyTotal, perPersonPerDay, categoryCalories, itemDtos,
                week.getPeople(), totalCost, perPersonCost);
    }

    private ItemNutritionDto toItemDto(GroceryItem item) {
        double factor = item.getServingSizeG() * item.getServingsPerWeek() / 100.0;
        return new ItemNutritionDto(
                item.getName(),
                item.getCategory(),
                item.getServingSizeG(),
                item.getServingsPerWeek(),
                round(val(item.getCaloriesPer100g()) * factor),
                round(val(item.getProteinPer100g())  * factor),
                round(val(item.getFatPer100g())       * factor),
                round(val(item.getCarbsPer100g())     * factor),
                round(val(item.getFiberPer100g())     * factor),
                round(val(item.getSodiumPer100g())    * factor),
                item.getPrice()
        );
    }

    private NutritionSummaryDto sum(List<ItemNutritionDto> items) {
        return new NutritionSummaryDto(
                round(items.stream().mapToDouble(ItemNutritionDto::weeklyCalories).sum()),
                round(items.stream().mapToDouble(ItemNutritionDto::weeklyProtein).sum()),
                round(items.stream().mapToDouble(ItemNutritionDto::weeklyFat).sum()),
                round(items.stream().mapToDouble(ItemNutritionDto::weeklyCarbs).sum()),
                round(items.stream().mapToDouble(ItemNutritionDto::weeklyFiber).sum()),
                round(items.stream().mapToDouble(ItemNutritionDto::weeklySodium).sum())
        );
    }

    private NutritionSummaryDto divide(NutritionSummaryDto total, double divisor) {
        if (divisor == 0) return total;
        return new NutritionSummaryDto(
                round(total.calories() / divisor),
                round(total.protein()  / divisor),
                round(total.fat()      / divisor),
                round(total.carbs()    / divisor),
                round(total.fiber()    / divisor),
                round(total.sodium()   / divisor)
        );
    }

    private double val(Double d) {
        return d != null ? d : 0.0;
    }

    private double round(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
