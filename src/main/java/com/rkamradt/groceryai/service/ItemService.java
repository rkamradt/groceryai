package com.rkamradt.groceryai.service;

import com.rkamradt.groceryai.dto.GroceryItemRequest;
import com.rkamradt.groceryai.dto.ItemsRequest;
import com.rkamradt.groceryai.model.GroceryItem;
import com.rkamradt.groceryai.model.Week;
import com.rkamradt.groceryai.repository.GroceryItemRepository;
import com.rkamradt.groceryai.repository.WeekRepository;
import com.rkamradt.groceryai.service.UsdaFoodService.NutritionPer100g;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ItemService {

    private final GroceryItemRepository itemRepository;
    private final WeekRepository weekRepository;
    private final UsdaFoodService usdaFoodService;

    public ItemService(
            GroceryItemRepository itemRepository,
            WeekRepository weekRepository,
            UsdaFoodService usdaFoodService
    ) {
        this.itemRepository = itemRepository;
        this.weekRepository = weekRepository;
        this.usdaFoodService = usdaFoodService;
    }

    @Transactional
    public List<GroceryItem> saveItems(ItemsRequest request) {
        Week week = weekRepository.findById(request.weekId())
                .orElseThrow(() -> new NoSuchElementException("Week not found: " + request.weekId()));

        // Replace existing items for this week
        itemRepository.deleteByWeekId(week.getId());

        // Store receipt total on the week if provided
        if (request.receiptTotal() != null) {
            week.setReceiptTotal(request.receiptTotal());
            weekRepository.save(week);
        }

        List<GroceryItem> saved = request.items().stream()
                .map(req -> buildItem(req, week))
                .map(itemRepository::save)
                .toList();

        return saved;
    }

    private GroceryItem buildItem(GroceryItemRequest req, Week week) {
        NutritionPer100g nutrition = usdaFoodService.lookupNutrition(req.name());

        GroceryItem item = new GroceryItem();
        item.setWeek(week);
        item.setName(req.name());
        item.setCategory(req.category());
        item.setServingSizeG(req.servingSizeG());
        item.setServingsPerWeek(req.servingsPerWeek());
        item.setPrice(req.price());
        item.setCaloriesPer100g(nutrition.calories());
        item.setProteinPer100g(nutrition.protein());
        item.setFatPer100g(nutrition.fat());
        item.setCarbsPer100g(nutrition.carbs());
        item.setFiberPer100g(nutrition.fiber());
        item.setSodiumPer100g(nutrition.sodium());
        return item;
    }
}
