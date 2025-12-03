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
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
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

    // ------------------------------------------------------------
    // SEND MESSAGE
    // ------------------------------------------------------------
    public Message sendMessage(String content) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Message userMessage = new Message();
        userMessage.setUser(user);
        userMessage.setRole("user");
        userMessage.setContent(content);
        userMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(userMessage);

        String aiResponse = getOllamaResponse(content);

        Message aiMessage = new Message();
        aiMessage.setUser(user);
        aiMessage.setRole("assistant");
        aiMessage.setContent(aiResponse);
        aiMessage.setTimestamp(LocalDateTime.now());
        messageRepository.save(aiMessage);

        return aiMessage;
    }

 public List<String> getChatHistoryStrings(Long userId) {
    List<Message> messages = messageRepository.findByUserIdOrderByTimestampAsc(userId);
    List<String> history = new ArrayList<>();
    for (Message m : messages) {
        String prefix = m.getRole().equals("user") ? "You: " : "AI: ";
        history.add(prefix + m.getContent());
    }
    return history;
}

public List<Message> getChatHistory(Long userId) {
    return messageRepository.findByUserIdOrderByTimestampAsc(userId);
}


    // ------------------------------------------------------------
    // 🔥 MAIN FINANCIAL + SAVINGS + DSA ANALYSIS AI ENGINE
    // ------------------------------------------------------------
    private String getOllamaResponse(String userMessage) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        double monthlyBudget = userBudgetService.getOrCreateBudget().getMonthlyBudget();
        List<Budget> expenses = budgetRepository.findByUserIdOrderByDateDesc(user.getId());
        List<SavingsGoal> goals = savingsGoalRepository.findByUserId(user.getId());

        // Build expense text
        StringBuilder expenseText = new StringBuilder();
        List<Double> amountList = new ArrayList<>();
        double totalSpent = 0;

        for (Budget b : expenses) {
            expenseText.append("- ")
                    .append(b.getCategory())
                    .append(" — ")
                    .append(b.getAmount())
                    .append(" on ")
                    .append(b.getDate())
                    .append("\n");

            totalSpent += b.getAmount();
            amountList.add(b.getAmount());
        }

        double remaining = monthlyBudget - totalSpent;

        // ------------------------------------------------------------
        // 🔥 DSA OUTPUTS START
        // ------------------------------------------------------------

        // 1️⃣ QuickSort by amount
        List<Budget> sortedExpenses = quickSortBudgets(new ArrayList<>(expenses));

        StringBuilder sortedText = new StringBuilder();
        for (Budget b : sortedExpenses) {
            sortedText.append(b.getCategory())
                      .append(": ")
                      .append(b.getAmount())
                      .append("\n");
        }

        // 2️⃣ HashMap totals
        Map<String, Double> categoryTotals = getCategoryTotals(expenses);

        StringBuilder categoryText = new StringBuilder();
        for (String key : categoryTotals.keySet()) {
            categoryText.append(key)
                        .append(": ")
                        .append(categoryTotals.get(key))
                        .append("\n");
        }

        // 3️⃣ Recursive spending prediction
        double predicted = amountList.isEmpty()
                ? 0
                : recursiveAverage(amountList, amountList.size() - 1);

        // 4️⃣ Sliding window: max spend in last 7
        double maxWeekly = getMaxWeeklySpending(amountList);

        // 5️⃣ Binary search example
        Budget found = binarySearchBudget(sortedExpenses, 200); // example search

        String binarySearchResult = (found == null)
                ? "No item found at amount 200"
                : "Found category: " + found.getCategory();

        // ------------------------------------------------------------
        // 🔥 Savings Goals Info
        // ------------------------------------------------------------
        StringBuilder goalText = new StringBuilder();
        double totalGoalsTarget = 0;
        double totalGoalsProgress = 0;

        for (SavingsGoal g : goals) {
            goalText.append("- ")
                    .append(g.getName())
                    .append(": saved ")
                    .append(g.getCurrentAmount())
                    .append(" / ")
                    .append(g.getTargetAmount())
                    .append("\n");

            totalGoalsTarget += g.getTargetAmount();
            totalGoalsProgress += g.getCurrentAmount();
        }

        double totalRemainingGoal = totalGoalsTarget - totalGoalsProgress;

        // ------------------------------------------------------------
        // Combine all DSA + Goals insights
        // ------------------------------------------------------------
        String insights =
                "DSA ANALYSIS:\n" +
                "-------------------\n" +
                "Sorted Expenses (QuickSort):\n" + sortedText + "\n" +
                "Category Totals (HashMap):\n" + categoryText + "\n" +
                "Predicted Next Expense (Recursion): " + predicted + "\n" +
                "Max Weekly Spending (Sliding Window): " + maxWeekly + "\n" +
                "Binary Search Result: " + binarySearchResult + "\n\n" +
                "SAVINGS GOALS:\n" +
                "-------------------\n" +
                "Total Goals Target: " + totalGoalsTarget + "\n" +
                "Total Saved: " + totalGoalsProgress + "\n" +
                "Remaining to Save: " + totalRemainingGoal + "\n" +
                goalText + "\n";

        // ------------------------------------------------------------
        // 🔥 FINAL PROMPT TO AI
        // ------------------------------------------------------------
        String financialContext =
                "USER FINANCIAL DATA:\n" +
                        "Monthly Budget: " + monthlyBudget + "\n" +
                        "Total Spent: " + totalSpent + "\n" +
                        "Remaining: " + remaining + "\n\n" +
                        "Expense Breakdown:\n" + expenseText +
                        "\n" + insights +
                        "Act like a professional financial advisor. Use above DSA + savings goals info to give smarter advice.\n\n" +
                        "User says: " + userMessage;

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

            if (!response.isSuccessful()) {
                return "Finance AI is warming up… ask again!";
            }

            String responseBody = response.body().string();
            JsonNode root = mapper.readTree(responseBody);

            if (root.has("response")) {
                return root.get("response").asText().trim();
            }

            return "Your CFO bot is ready!";

        } catch (Exception e) {
            return "Please wait… loading financial brain.";
        }
    }

    // ------------------------------------------------------------
    // ✅ DSA MODULES
    // ------------------------------------------------------------
    public List<Budget> quickSortBudgets(List<Budget> budgets) {
        if (budgets == null || budgets.size() < 2) return budgets;

        Budget pivot = budgets.get(0);
        List<Budget> left = new ArrayList<>();
        List<Budget> right = new ArrayList<>();

        for (int i = 1; i < budgets.size(); i++) {
            if (budgets.get(i).getAmount() <= pivot.getAmount())
                left.add(budgets.get(i));
            else
                right.add(budgets.get(i));
        }

        List<Budget> sorted = new ArrayList<>();
        sorted.addAll(quickSortBudgets(left));
        sorted.add(pivot);
        sorted.addAll(quickSortBudgets(right));

        return sorted;
    }

    public Map<String, Double> getCategoryTotals(List<Budget> budgets) {
        Map<String, Double> map = new HashMap<>();

        for (Budget b : budgets) {
            map.put(b.getCategory(), map.getOrDefault(b.getCategory(), 0.0) + b.getAmount());
        }
        return map;
    }

    public double recursiveAverage(List<Double> amounts, int index) {
        if (index == 0) return amounts.get(0);
        return (amounts.get(index) + recursiveAverage(amounts, index - 1)) / 2;
    }

    public double getMaxWeeklySpending(List<Double> amounts) {
        double maxSum = 0;
        double window = 0;

        for (int i = 0; i < amounts.size(); i++) {
            window += amounts.get(i);
            if (i >= 7) window -= amounts.get(i - 7);
            maxSum = Math.max(maxSum, window);
        }
        return maxSum;
    }

    public Budget binarySearchBudget(List<Budget> sortedList, double targetAmount) {
        int left = 0, right = sortedList.size() - 1;

        while (left <= right) {
            int mid = (left + right) / 2;
            double midVal = sortedList.get(mid).getAmount();

            if (midVal == targetAmount)
                return sortedList.get(mid);
            else if (midVal < targetAmount)
                left = mid + 1;
            else
                right = mid - 1;
        }
        return null;
    }
}