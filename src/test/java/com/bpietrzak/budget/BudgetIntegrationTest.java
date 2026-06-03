package com.bpietrzak.budget;

import com.bpietrzak.budget.dto.AccountCreateRequest;
import com.bpietrzak.budget.dto.TransactionCreateRequest;
import com.bpietrzak.budget.model.enums.TransactionType;
import com.bpietrzak.budget.repository.AccountRepository;
import com.bpietrzak.budget.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class BudgetIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void happyPath_balanceAndSummary() throws Exception {
        String accountId = createAccount("Main Account");

        postTransaction(accountId, TransactionType.INCOME, "3000.00", "Salary");
        postTransaction(accountId, TransactionType.INCOME, "500.00", "Bonus");
        postTransaction(accountId, TransactionType.EXPENSE, "200.00", "Food");

        // balance = 3000 + 500 - 200 = 3300
        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(3300.00));

        mockMvc.perform(get("/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").value(3500.00))
                .andExpect(jsonPath("$.totalExpenses").value(200.00))
                .andExpect(jsonPath("$.expensesByCategory.Food").value(200.00));
    }

    @Test
    void deleteTransaction_revertsBalance() throws Exception {
        String accountId = createAccount("Test Account");

        postTransaction(accountId, TransactionType.INCOME, "1000.00", "Salary");
        String transactionId = postTransaction(accountId, TransactionType.EXPENSE, "400.00", "Rent");

        // balance = 1000 - 400 = 600
        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(jsonPath("$.balance").value(600.00));

        // delete expense → balance reverts to 1000
        mockMvc.perform(delete("/transactions/" + transactionId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/" + accountId))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }

    @Test
    void deleteAccount_withTransactions_returns409() throws Exception {
        String accountId = createAccount("Test Account");
        postTransaction(accountId, TransactionType.INCOME, "100.00", "Test");

        mockMvc.perform(delete("/accounts/" + accountId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void getAccount_notFound_returns404WithErrorBody() throws Exception {
        mockMvc.perform(get("/accounts/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/accounts/00000000-0000-0000-0000-000000000000"));
    }

    @Test
    void createAccount_blankName_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("name: Name is required"));
    }

    private String createAccount(String name) throws Exception {
        String json = objectMapper.writeValueAsString(new AccountCreateRequest(name));
        MvcResult result = mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String postTransaction(String accountId, TransactionType type, String amount, String category) throws Exception {
        TransactionCreateRequest request = new TransactionCreateRequest(
                type, new BigDecimal(amount), category, null, null, UUID.fromString(accountId));
        MvcResult result = mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
