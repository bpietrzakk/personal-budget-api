package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.TransactionCreateRequest;
import com.bpietrzak.budget.dto.TransactionResponse;
import com.bpietrzak.budget.exception.ResourceNotFoundException;
import com.bpietrzak.budget.model.Account;
import com.bpietrzak.budget.model.Transaction;
import com.bpietrzak.budget.model.enums.TransactionType;
import com.bpietrzak.budget.repository.AccountRepository;
import com.bpietrzak.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionResponse create(TransactionCreateRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountId()));

        BigDecimal newBalance = request.getType() == TransactionType.INCOME
                ? account.getBalance().add(request.getAmount())
                : account.getBalance().subtract(request.getAmount());
        account.setBalance(newBalance);

        Transaction transaction = Transaction.builder()
                .type(request.getType())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate() != null ? request.getTransactionDate() : LocalDate.now())
                .account(account)
                .build();

        accountRepository.save(account);
        return toResponse(transactionRepository.save(transaction));
    }

    public List<TransactionResponse> findAll(LocalDate from, LocalDate to, String category) {
        return transactionRepository.findWithFilters(from, to, category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));

        Account account = transaction.getAccount();
        BigDecimal reversedBalance = transaction.getType() == TransactionType.INCOME
                ? account.getBalance().subtract(transaction.getAmount())
                : account.getBalance().add(transaction.getAmount());
        account.setBalance(reversedBalance);

        accountRepository.save(account);
        transactionRepository.delete(transaction);
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .amount(t.getAmount())
                .category(t.getCategory())
                .description(t.getDescription())
                .transactionDate(t.getTransactionDate())
                .createdAt(t.getCreatedAt())
                .accountId(t.getAccount().getId())
                .build();
    }
}
