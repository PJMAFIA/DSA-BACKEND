package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
@Data
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String category; // e.g., "Food", "Transport"
    private double amount;
    private LocalDate date;

    // Constructors, getters/setters via Lombok
}