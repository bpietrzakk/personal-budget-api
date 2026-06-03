package com.bpietrzak.budget.controller;

import com.bpietrzak.budget.dto.AccountCreateRequest;
import com.bpietrzak.budget.dto.AccountResponse;
import com.bpietrzak.budget.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Manage bank accounts")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(summary = "List all accounts")
    public List<AccountResponse> getAll() {
        return accountService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public AccountResponse create(@RequestBody @Valid AccountCreateRequest request) {
        return accountService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by ID with current balance")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public AccountResponse getById(@PathVariable UUID id) {
        return accountService.findById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete account (only if no transactions exist)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Account deleted"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "409", description = "Account has existing transactions")
    })
    public void delete(@PathVariable UUID id) {
        accountService.delete(id);
    }

    @GetMapping("/{id}/transactions/export")
    @Operation(summary = "Export account transactions as CSV")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CSV file"),
            @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public void exportCsv(@PathVariable UUID id, HttpServletResponse response) throws IOException {
        accountService.findById(id);
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition",
                "attachment; filename=transactions_" + id + ".csv");
        accountService.writeCsv(id, response.getWriter());
    }
}
