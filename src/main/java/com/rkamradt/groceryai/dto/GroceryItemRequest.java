package com.rkamradt.groceryai.dto;

public record GroceryItemRequest(String name, String category, double servingSizeG, int servingsPerWeek, Double price) {}
