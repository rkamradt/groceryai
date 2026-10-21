package com.rkamradt.groceryai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractedGroceryItem(
        String name,
        Double unitPrice,
        Double quantity,
        String unit,
        Double lineTotal
) {}
