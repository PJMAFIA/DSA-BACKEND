package com.example.demo.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "savings_goals")
@Data
public class SavingsGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String name;
    private double targetAmount;
    private double currentAmount = 0.0;
    private LocalDate createdDate = LocalDate.now();
}