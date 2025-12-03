package com.example.demo.controller;

import com.example.demo.model.Message;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.ChatService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @Autowired
    public ChatController(ChatService chatService, UserRepository userRepository) {
        this.chatService = chatService;
        this.userRepository = userRepository;
    }

    // ---------------- SEND MESSAGE ----------------
    @PostMapping
    public ResponseEntity<Message> sendMessage(@RequestBody ChatRequest request) {
        Message aiMessage = chatService.sendMessage(request.content);
        return ResponseEntity.ok(aiMessage);
    }

    // ---------------- GET CHAT HISTORY ----------------
    @GetMapping("/history")
    public ResponseEntity<List<Message>> getChatHistory() {
        // Logged-in user ko fetch karo
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Chat history fetch karo from service
        List<Message> history = chatService.getChatHistory(user.getId());
        return ResponseEntity.ok(history);
    }
    
    // ---------------- REQUEST BODY RECORD ----------------
    public record ChatRequest(String content) {}
}
