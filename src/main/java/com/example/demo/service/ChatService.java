package com.example.demo.service;

import com.example.demo.model.Budget;
import com.example.demo.model.Message;
import com.example.demo.model.SavingsGoal;
import com.example.demo.model.User;
import com.example.demo.service.UserBudgetService;
import com.example.demo.repository.MessageRepository;
import com.example.demo.repository.BudgetRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.SavingsGoalRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BudgetRepository budgetRepository;
    private final UserBudgetService userBudgetService;
    private final SavingsGoalRepository savingsGoalRepository;

    @Value("${alpha.vantage.api.key}")
    private String alphaVantageApiKey;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public ChatService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            BudgetRepository budgetRepository,
            UserBudgetService userBudgetService,
            SavingsGoalRepository savingsGoalRepository
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.budgetRepository = budgetRepository;
        this.userBudgetService = userBudgetService;
        this.savingsGoalRepository = savingsGoalRepository;
    }

    public Message sendMessage(String content) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save user message
        Message userMessage = new Message();
        userMessage.setUser(user);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(userMessage);

        // 🔥 Async response for low-end laptop speed
        String aiResponse = getOllamaResponse(content, user);

        Message aiMessage = new Message();
        aiMessage.setUser(user);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiResponse);
        aiMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(aiMessage);

        return aiMessage;
    }
    public List<Message> getChatHistory(Long userId) {
    return messageRepository.findByUserIdOrderByTimestampAsc(userId);
}
    // ------------------------------------------------------------
    private String getOllamaResponse(String userMessage, User user) {
        // fetch all budgets & goals at once
        double monthlyBudget = userBudgetService.getOrCreateBudget().getMonthlyBudget();
        List<Budget> expenses = budgetRepository.findByUserIdOrderByDateDesc(user.getId());
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(user.getId());

        double totalSpent = 0;
        List<Double> amountList = new ArrayList<>();
        StringBuilder expenseText = new StringBuilder();
        for (Budget b : expenses) {
            expenseText.append("- ").append(b.getCategory())
                       .append(" — ").append(b.getAmount())
                       .append(" on ").append(b.getDate()).append("\n");
            totalSpent += b.getAmount();
            amountList.add(b.getAmount());
        }
        double remaining = monthlyBudget - totalSpent;

        // Use built-in sort (faster than recursive quicksort)
        expenses.sort(Comparator.comparingDouble(Budget::getAmount));
        StringBuilder sortedText = new StringBuilder();
        for (Budget b : expenses) {
            sortedText.append(b.getCategory()).append(": ").append(b.getAmount()).append("\n");
        }

        // Category totals
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Budget b : expenses)
            categoryTotals.put(b.getCategory(), categoryTotals.getOrDefault(b.getCategory(), 0.0) + b.getAmount());

        StringBuilder categoryText = new StringBuilder();
        categoryTotals.forEach((k, v) -> categoryText.append(k).append(": ").append(v).append("\n"));

        // Iterative average instead of recursion
        double predicted = 0;
        if (!amountList.isEmpty()) {
            double sum = 0;
            for (double a : amountList) sum += a;
            predicted = sum / amountList.size();
        }

        // Max weekly spending (sliding window)
        double maxWeekly = 0, window = 0;
        for (int i = 0; i < amountList.size(); i++) {
            window += amountList.get(i);
            if (i >= 7) window -= amountList.get(i - 7);
            maxWeekly = Math.max(maxWeekly, window);
        }

        // Binary search example
        Budget found = binarySearchBudget(expenses, 200);
        String binarySearchResult = (found == null) ? "No item found at amount 200"
                                                    : "Found category: " + found.getCategory();

        // Savings goals
        double totalGoalsTarget = 0, totalGoalsProgress = 0;
        StringBuilder goalText = new StringBuilder();
        for (SavingsGoal g : goals) {
            goalText.append("- ").append(g.getName())
                    .append(": saved ").append(g.getCurrentAmount())
                    .append(" / ").append(g.getTargetAmount()).append("\n");
            totalGoalsTarget += g.getTargetAmount();
            totalGoalsProgress += g.getCurrentAmount();
        }
        double totalRemainingGoal = totalGoalsTarget - totalGoalsProgress;
        
        // 🔥 Compose final prompt
        String financialContext =
                "USER FINANCIAL DATA:\n" +
                "Monthly Budget: " + monthlyBudget + "\n" +
                "Total Spent: " + totalSpent + "\n" +
                "Remaining: " + remaining + "\n\n" +
                "Expense Breakdown:\n" + expenseText +
                "\nDSA ANALYSIS:\n" +
                "Sorted Expenses:\n" + sortedText +
                "Category Totals:\n" + categoryText +
                "Predicted Next Expense: " + predicted + "\n" +
                "Max Weekly Spending: " + maxWeekly + "\n" +
                "Binary Search Result: " + binarySearchResult + "\n\n" +
                "SAVINGS GOALS:\n" +
                "Total Goals Target: " + totalGoalsTarget + "\n" +
                "Total Saved: " + totalGoalsProgress + "\n" +
                "Remaining to Save: " + totalRemainingGoal + "\n" +
                goalText + "\n" +
                "Act like a professional financial advisor.\nUser says: " + userMessage;

        String jsonInput = "{\"model\":\"llama3.2\",\"prompt\":\"" +
                financialContext.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") +
                "\",\"stream\":false,\"options\":{\"temperature\":0.7,\"num_predict\":1000}}";

        RequestBody body = RequestBody.create(jsonInput,
                MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url("http://localhost:11434/api/generate")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) return "Finance AI is warming up… ask again!";
            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);
            return root.has("response") ? root.get("response").asText().trim() : "Your CFO bot is ready!";
        } catch (Exception e) {
            return "Please wait… loading financial brain.";
        }
    }

    // Binary search same as before
    public Budget binarySearchBudget(List<Budget> sortedList, double targetAmount) {
        int left = 0, right = sortedList.size() - 1;
        while (left <= right) {
            int mid = (left + right) / 2;
            double midVal = sortedList.get(mid).getAmount();
            if (midVal == targetAmount) return sortedList.get(mid);
            else if (midVal < targetAmount) left = mid + 1;
            else right = mid - 1;
        }
        return null;
    }
}
