package com.rkamradt.groceryai.controller;

import com.rkamradt.groceryai.dto.ApiResponse;
import com.rkamradt.groceryai.dto.WeekCreateRequest;
import com.rkamradt.groceryai.dto.WeekDto;
import com.rkamradt.groceryai.service.WeekService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/weeks")
public class WeekController {

    private final WeekService weekService;

    public WeekController(WeekService weekService) {
        this.weekService = weekService;
    }

    @GetMapping
    public ApiResponse<List<WeekDto>> list() {
        return ApiResponse.ok(weekService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeekDto>> get(@PathVariable UUID id) {
        return weekService.findById(id)
                .<ResponseEntity<ApiResponse<WeekDto>>>map(dto -> ResponseEntity.ok(ApiResponse.ok(dto)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ApiResponse<WeekDto> create(@RequestBody WeekCreateRequest request) {
        return ApiResponse.ok(weekService.create(request));
    }
}
