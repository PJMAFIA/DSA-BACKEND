package com.example.demo.service;

import com.example.demo.model.SavingsGoal;
import com.example.demo.model.User;
import com.example.demo.repository.SavingsGoalRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SavingsGoalService {
    private final SavingsGoalRepository goalRepository;
    private final UserRepository userRepository;

    @Autowired
    public SavingsGoalService(SavingsGoalRepository goalRepository, UserRepository userRepository) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email).orElseThrow();
    }

    public SavingsGoal createGoal(SavingsGoal goal) {
        goal.setUser(getCurrentUser());
        return goalRepository.save(goal);
    }
    public List<SavingsGoal> getGoalsByUser(User user) {
        return goalRepository.findByUserId(user.getId());
    }
    public List<SavingsGoal> getGoals() {
        return goalRepository.findByUserId(getCurrentUser().getId());
    }

    public SavingsGoal addProgress(Long id, double amount) {
        SavingsGoal goal = goalRepository.findById(id).orElseThrow();
        if (!goal.getUser().getId().equals(getCurrentUser().getId())) throw new RuntimeException("Unauthorized");
        goal.setCurrentAmount(goal.getCurrentAmount() + amount);
        return goalRepository.save(goal);
    }

    public void deleteGoal(Long id) {
        SavingsGoal goal = goalRepository.findById(id).orElseThrow();
        if (!goal.getUser().getId().equals(getCurrentUser().getId())) throw new RuntimeException("Unauthorized");
        goalRepository.deleteById(id);
    }
}