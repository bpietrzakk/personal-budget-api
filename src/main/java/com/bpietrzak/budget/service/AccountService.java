package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.AccountCreateRequest;
import com.bpietrzak.budget.dto.AccountResponse;
import com.bpietrzak.budget.exception.ConflictException;
import com.bpietrzak.budget.exception.ResourceNotFoundException;
import com.bpietrzak.budget.model.Account;
import com.bpietrzak.budget.repository.AccountRepository;
import com.bpietrzak.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountResponse create(AccountCreateRequest request) {
        Account account = Account.builder()
                .name(request.getName())
                .balance(BigDecimal.ZERO)
                .build();
        return toResponse(accountRepository.save(account));
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AccountResponse findById(UUID id) {
        return accountRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    public void delete(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        if (transactionRepository.existsByAccountId(id)) {
            throw new ConflictException("Cannot delete account with existing transactions");
        }
        accountRepository.delete(account);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .name(account.getName())
                .balance(account.getBalance())
                .createdAt(account.getCreatedAt())
                .build();
    }
}