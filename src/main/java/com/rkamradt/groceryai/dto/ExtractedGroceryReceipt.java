package com.rkamradt.groceryai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedGroceryReceipt(
        String storeName,
        String date,
        List<ExtractedGroceryItem> items,
        Double total,
        String confidenceNotes
) {}
