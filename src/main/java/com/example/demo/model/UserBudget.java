package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user_budgets")
@Data
public class UserBudget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private double monthlyBudget = 5000.0; // Default 5000
}