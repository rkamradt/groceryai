package com.rkamradt.groceryai.dto;

import java.time.LocalDate;

public record WeekCreateRequest(LocalDate startDate, Integer people) {}
