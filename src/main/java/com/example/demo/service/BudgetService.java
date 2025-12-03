
package com.example.demo.service;

import com.example.demo.model.Budget;
import com.example.demo.model.User;
import com.example.demo.repository.BudgetRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;

    @Autowired
    private UserBudgetService userBudgetService;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository, UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.userRepository = userRepository;
    }

    public double getMonthlyBudget() {
        return userBudgetService.getOrCreateBudget().getMonthlyBudget();
    }

    public Budget addExpense(Budget expense) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        expense.setUser(user);
        return budgetRepository.save(expense);
    }

    // YE PURA METHOD REPLACE KAR DE — AB SORTED RETURN KAREGA (LOW TO HIGH)
    public List<Budget> getExpenses() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Budget> expenses = budgetRepository.findByUserIdOrderByDateDesc(user.getId());

        // DSA: Insertion Sort → Low to High by amount
        return insertionSortByAmount(new ArrayList<>(expenses));
    }

    // YE NAYA METHOD ADD KAR DE
    private List<Budget> insertionSortByAmount(List<Budget> list) {
        for (int i = 1; i < list.size(); i++) {
            Budget current = list.get(i);
            int j = i - 1;

            while (j >= 0 && list.get(j).getAmount() > current.getAmount()) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, current);
        }
        return list; // Sorted: ₹100, ₹500, ₹2000, ₹50000...
    }

    public void deleteExpense(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }
        budgetRepository.deleteById(id);
    }
{


}
    public List<java.util.Map<String, Object>> getTopSpendingCategories() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return budgetRepository.findTopSpendingCategories(user.getId());
    }
}
