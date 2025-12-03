package com.example.demo.controller;

import com.example.demo.model.SavingsGoal;
import com.example.demo.service.SavingsGoalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {
    private final SavingsGoalService goalService;

    @Autowired
    public SavingsGoalController(SavingsGoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    public ResponseEntity<SavingsGoal> create(@RequestBody SavingsGoal goal) {
        return ResponseEntity.ok(goalService.createGoal(goal));
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getAll() {
        return ResponseEntity.ok(goalService.getGoals());
    }

    @PutMapping("/{id}/progress")
    public ResponseEntity<SavingsGoal> addProgress(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        return ResponseEntity.ok(goalService.addProgress(id, body.get("amount")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goalService.deleteGoal(id);
        return ResponseEntity.ok().build();
    }
}