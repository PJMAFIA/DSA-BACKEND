package com.example.demo.controller;

import com.example.demo.model.Budget;
import com.example.demo.model.UserBudget;
import com.example.demo.service.BudgetService;
import com.example.demo.service.UserBudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {
    private final BudgetService budgetService;

    @Autowired
    private UserBudgetService userBudgetService;  // ← ADD THIS

    @Autowired
    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    public UserBudgetService getUserBudgetService() {  // ← ADD THIS
        return userBudgetService;
    }

    @PostMapping
    public ResponseEntity<Budget> addExpense(@RequestBody Budget expense) {
        return ResponseEntity.ok(budgetService.addExpense(expense));
    }

    @GetMapping
    public ResponseEntity<List<Budget>> getExpenses() {
        return ResponseEntity.ok(budgetService.getExpenses());
    }

    @GetMapping("/top-categories")
    public ResponseEntity<List<Map<String, Object>>> getTopSpendingCategories() {
        return ResponseEntity.ok(budgetService.getTopSpendingCategories());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        budgetService.deleteExpense(id);
        return ResponseEntity.ok().build();
    }

    // ← PASTE THESE TWO BELOW
    @GetMapping("/monthly-budget")
    public ResponseEntity<Double> getMonthlyBudget() {
        return ResponseEntity.ok(budgetService.getMonthlyBudget());
    }

    @PutMapping("/monthly-budget")
    public ResponseEntity<UserBudget> updateMonthlyBudget(@RequestBody Map<String, Double> body) {
        double newBudget = body.get("monthlyBudget");
        UserBudget updated = userBudgetService.updateMonthlyBudget(newBudget);
        return ResponseEntity.ok(updated);
    }
}