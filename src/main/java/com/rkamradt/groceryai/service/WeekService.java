package com.rkamradt.groceryai.service;

import com.rkamradt.groceryai.dto.WeekCreateRequest;
import com.rkamradt.groceryai.dto.WeekDto;
import com.rkamradt.groceryai.model.Week;
import com.rkamradt.groceryai.repository.WeekRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class WeekService {

    private final WeekRepository weekRepository;

    public WeekService(WeekRepository weekRepository) {
        this.weekRepository = weekRepository;
    }

    public List<WeekDto> findAll() {
        return weekRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<WeekDto> findById(UUID id) {
        return weekRepository.findById(id).map(this::toDto);
    }

    public WeekDto create(WeekCreateRequest request) {
        Week week = new Week();
        week.setStartDate(request.startDate() != null ? request.startDate() : LocalDate.now());
        week.setPeople(request.people() != null && request.people() > 0 ? request.people() : 2);
        return toDto(weekRepository.save(week));
    }

    private WeekDto toDto(Week w) {
        return new WeekDto(w.getId(), w.getStartDate(), w.getPeople(), w.getCreatedAt());
    }
}
