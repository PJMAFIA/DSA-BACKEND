package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.model.UserBudget;
import com.example.demo.repository.UserBudgetRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserBudgetService {
    private final UserBudgetRepository userBudgetRepository;
    private final UserRepository userRepository;

    @Autowired
    public UserBudgetService(UserBudgetRepository userBudgetRepository, UserRepository userRepository) {
        this.userBudgetRepository = userBudgetRepository;
        this.userRepository = userRepository;
    }

    public UserBudget getOrCreateBudget() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username).orElseThrow();

        return userBudgetRepository.findByUserId(user.getId())
            .orElseGet(() -> {
                UserBudget budget = new UserBudget();
                budget.setUser(user);
                budget.setMonthlyBudget(5000.0);
                return userBudgetRepository.save(budget);
            });
    }

    public UserBudget updateMonthlyBudget(double newBudget) {
        UserBudget budget = getOrCreateBudget();
        budget.setMonthlyBudget(newBudget);
        return userBudgetRepository.save(budget);
    }
}