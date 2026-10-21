package com.rkamradt.groceryai.repository;

import com.rkamradt.groceryai.model.GroceryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GroceryItemRepository extends JpaRepository<GroceryItem, UUID> {
    List<GroceryItem> findByWeekId(UUID weekId);
    void deleteByWeekId(UUID weekId);
}
