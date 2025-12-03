package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final UserRepository userRepository;

    @Autowired
    private BudgetService budgetService;  // YE ADD KIYA

    public DashboardController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<User> getUserData() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(ResponseEntity::ok)
                   .orElseGet(() -> ResponseEntity.status(404).build());
    }

    // YE NAYA ENDPOINT — DSA WALA
    
}