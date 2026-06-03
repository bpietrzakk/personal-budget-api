package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.TransactionCreateRequest;
import com.bpietrzak.budget.exception.ResourceNotFoundException;
import com.bpietrzak.budget.model.Account;
import com.bpietrzak.budget.model.Transaction;
import com.bpietrzak.budget.model.enums.TransactionType;
import com.bpietrzak.budget.repository.AccountRepository;
import com.bpietrzak.budget.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @InjectMocks private TransactionService transactionService;

    @Test
    void create_income_addsAmountToBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).name("Test").balance(new BigDecimal("1000.00")).build();
        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.INCOME, new BigDecimal("500.00"), "Salary", null, null, accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> buildSavedTransaction(inv.getArgument(0), account));

        transactionService.create(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("1500.00");
    }

    @Test
    void create_expense_subtractsAmountFromBalance() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).name("Test").balance(new BigDecimal("1000.00")).build();
        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.EXPENSE, new BigDecimal("300.00"), "Food", null, null, accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> buildSavedTransaction(inv.getArgument(0), account));

        transactionService.create(request);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("700.00");
    }

    @Test
    void create_nullTransactionDate_defaultsToToday() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.builder().id(accountId).name("Test").balance(BigDecimal.ZERO).build();
        TransactionCreateRequest request = new TransactionCreateRequest(
                TransactionType.INCOME, new BigDecimal("100.00"), "Other", null, null, accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(transactionRepository.save(any())).thenAnswer(inv -> buildSavedTransaction(inv.getArgument(0), account));

        transactionService.create(request);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getTransactionDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void delete_income_subtractsAmountFromBalance() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(UUID.randomUUID()).name("Test").balance(new BigDecimal("1500.00")).build();
        Transaction transaction = Transaction.builder()
                .id(id).type(TransactionType.INCOME).amount(new BigDecimal("500.00"))
                .category("Salary").transactionDate(LocalDate.now()).account(account).build();

        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(accountRepository.save(any())).thenReturn(account);

        transactionService.delete(id);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void delete_expense_addsAmountToBalance() {
        UUID id = UUID.randomUUID();
        Account account = Account.builder().id(UUID.randomUUID()).name("Test").balance(new BigDecimal("700.00")).build();
        Transaction transaction = Transaction.builder()
                .id(id).type(TransactionType.EXPENSE).amount(new BigDecimal("300.00"))
                .category("Food").transactionDate(LocalDate.now()).account(account).build();

        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(accountRepository.save(any())).thenReturn(account);

        transactionService.delete(id);

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualByComparingTo("1000.00");
    }

    @Test
    void delete_throwsResourceNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(transactionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Transaction buildSavedTransaction(Transaction t, Account account) {
        return Transaction.builder()
                .id(UUID.randomUUID()).type(t.getType()).amount(t.getAmount())
                .category(t.getCategory()).transactionDate(t.getTransactionDate())
                .account(account).build();
    }
}
