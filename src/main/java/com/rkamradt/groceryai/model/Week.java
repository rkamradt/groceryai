package com.rkamradt.groceryai.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weeks")
public class Week {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private int people = 2;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Receipt total from the most recent upload (dollars) */
    private Double receiptTotal;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public int getPeople() { return people; }
    public void setPeople(int people) { this.people = people; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getReceiptTotal() { return receiptTotal; }
    public void setReceiptTotal(Double receiptTotal) { this.receiptTotal = receiptTotal; }
}
