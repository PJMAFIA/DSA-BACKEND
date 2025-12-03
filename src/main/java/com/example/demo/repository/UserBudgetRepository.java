package com.example.demo.repository;

import com.example.demo.model.UserBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserBudgetRepository extends JpaRepository<UserBudget, Long> {
    Optional<UserBudget> findByUserId(Long userId);
}