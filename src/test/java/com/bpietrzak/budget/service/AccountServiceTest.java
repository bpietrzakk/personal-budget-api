package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.AccountCreateRequest;
import com.bpietrzak.budget.dto.AccountResponse;
import com.bpietrzak.budget.exception.ConflictException;
import com.bpietrzak.budget.exception.ResourceNotFoundException;
import com.bpietrzak.budget.model.Account;
import com.bpietrzak.budget.repository.AccountRepository;
import com.bpietrzak.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private AccountService accountService;

    @Test
    void create_savesAccountWithZeroBalance() {
        AccountCreateRequest request = new AccountCreateRequest("My Account");
        Account saved = Account.builder()
                .id(UUID.randomUUID()).name("My Account")
                .balance(BigDecimal.ZERO).createdAt(LocalDateTime.now()).build();
        when(accountRepository.save(any())).thenReturn(saved);

        AccountResponse response = accountService.create(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(captor.getValue().getName()).isEqualTo("My Account");
        assertThat(response.getId()).isEqualTo(saved.getId());
    }

    @Test
    void findById_throwsResourceNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_deletesAccount_whenNoTransactions() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).name("Test").balance(BigDecimal.ZERO).build();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByAccountId(id)).thenReturn(false);

        accountService.delete(id);

        verify(accountRepository).delete(account);
    }

    @Test
    void delete_throwsConflictException_whenHasTransactions() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(id).name("Test").balance(BigDecimal.ZERO).build();
        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByAccountId(id)).thenReturn(true);

        assertThatThrownBy(() -> accountService.delete(id))
                .isInstanceOf(ConflictException.class);
        verify(accountRepository, never()).delete(any());
    }

    @Test
    void delete_throwsResourceNotFoundException_whenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
