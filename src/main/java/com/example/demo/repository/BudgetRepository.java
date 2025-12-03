package com.example.demo.repository;

import com.example.demo.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserIdOrderByDateDesc(Long userId);

    // DSA: Query for top spending categories (grouped sum)
    @Query(value = "SELECT b.category, SUM(b.amount) as total FROM Budget b WHERE b.user_id = :userId GROUP BY b.category ORDER BY total DESC", nativeQuery = true)
    List<Map<String, Object>> findTopSpendingCategories(Long userId);
    
}