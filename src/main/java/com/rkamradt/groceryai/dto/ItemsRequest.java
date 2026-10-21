package com.rkamradt.groceryai.dto;

import java.util.List;
import java.util.UUID;

public record ItemsRequest(UUID weekId, List<GroceryItemRequest> items, Double receiptTotal) {}
