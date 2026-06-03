package com.bpietrzak.budget.repository;

import com.bpietrzak.budget.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
}

