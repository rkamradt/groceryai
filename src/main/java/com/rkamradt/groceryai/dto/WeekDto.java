package com.rkamradt.groceryai.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record WeekDto(UUID id, LocalDate startDate, int people, LocalDateTime createdAt) {}
