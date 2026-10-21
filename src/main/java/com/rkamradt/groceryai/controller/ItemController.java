package com.rkamradt.groceryai.controller;

import com.rkamradt.groceryai.dto.ApiResponse;
import com.rkamradt.groceryai.dto.ItemsRequest;
import com.rkamradt.groceryai.model.GroceryItem;
import com.rkamradt.groceryai.service.ItemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @PostMapping
    public ApiResponse<List<String>> saveItems(@RequestBody ItemsRequest request) {
        List<GroceryItem> saved = itemService.saveItems(request);
        List<String> names = saved.stream().map(GroceryItem::getName).toList();
        return ApiResponse.ok(names);
    }
}
