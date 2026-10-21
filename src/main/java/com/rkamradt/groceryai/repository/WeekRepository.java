package com.rkamradt.groceryai.repository;

import com.rkamradt.groceryai.model.Week;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WeekRepository extends JpaRepository<Week, UUID> {
    List<Week> findAllByOrderByStartDateDesc();
}
