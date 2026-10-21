package com.rkamradt.groceryai.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "grocery_items")
public class GroceryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "week_id", nullable = false)
    private Week week;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    /** Grams per serving */
    @Column(nullable = false)
    private double servingSizeG;

    @Column(nullable = false)
    private int servingsPerWeek;

    // Nutrition values per 100 g (from USDA FoodData Central)
    private Double caloriesPer100g;
    private Double proteinPer100g;
    private Double fatPer100g;
    private Double carbsPer100g;
    private Double fiberPer100g;
    /** Sodium in mg per 100 g */
    private Double sodiumPer100g;

    /** Price paid on the receipt (dollars), null if not from a receipt */
    private Double price;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Week getWeek() { return week; }
    public void setWeek(Week week) { this.week = week; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getServingSizeG() { return servingSizeG; }
    public void setServingSizeG(double servingSizeG) { this.servingSizeG = servingSizeG; }

    public int getServingsPerWeek() { return servingsPerWeek; }
    public void setServingsPerWeek(int servingsPerWeek) { this.servingsPerWeek = servingsPerWeek; }

    public Double getCaloriesPer100g() { return caloriesPer100g; }
    public void setCaloriesPer100g(Double caloriesPer100g) { this.caloriesPer100g = caloriesPer100g; }

    public Double getProteinPer100g() { return proteinPer100g; }
    public void setProteinPer100g(Double proteinPer100g) { this.proteinPer100g = proteinPer100g; }

    public Double getFatPer100g() { return fatPer100g; }
    public void setFatPer100g(Double fatPer100g) { this.fatPer100g = fatPer100g; }

    public Double getCarbsPer100g() { return carbsPer100g; }
    public void setCarbsPer100g(Double carbsPer100g) { this.carbsPer100g = carbsPer100g; }

    public Double getFiberPer100g() { return fiberPer100g; }
    public void setFiberPer100g(Double fiberPer100g) { this.fiberPer100g = fiberPer100g; }

    public Double getSodiumPer100g() { return sodiumPer100g; }
    public void setSodiumPer100g(Double sodiumPer100g) { this.sodiumPer100g = sodiumPer100g; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
