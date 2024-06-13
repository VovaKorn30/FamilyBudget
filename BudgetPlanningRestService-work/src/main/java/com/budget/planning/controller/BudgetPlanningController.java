package com.budget.planning.controller;

import com.budget.planning.configuration.security.UserAdapter;
import com.budget.planning.dto.request.*;
import com.budget.planning.dto.response.*;
import com.budget.planning.service.BudgetPlanningService;
import com.budget.planning.service.UserDetailsServiceImp;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
public class BudgetPlanningController {
    private final BudgetPlanningService budgetPlanningService;
    private final UserDetailsServiceImp userDetailsService;

    @Operation(summary = "Register new user")
    @ApiResponse(responseCode = "200", description = "User registered", content = @Content)
    @ApiResponse(responseCode = "400", description = "Wrong role or user already registered", content = @Content)

    @PostMapping("/user/register")
    public ResponseEntity<String> register(@Valid @RequestBody UserRegistrationRequest userRegistrationRequest) {
        return userDetailsService.register(userRegistrationRequest);
    }

    @Operation(summary = "Register new bank account, authorization required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Created bank account",
            content = @Content(
                schema = @Schema(implementation = AccountUpdateDTO.class),
                examples = @ExampleObject(value = "{\"account_id\":2,\"balance\":10000}")))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)

    @PostMapping("/account/register")
    public AccountUpdateDTO register(@Valid @RequestBody AccountRegistrationRequest accountRequest,
                                     @AuthenticationPrincipal UserAdapter user) {
        return budgetPlanningService.registerAccount(accountRequest, user.getUser());
    }

    @Operation(summary = "Replenish a bank account, authorization required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Updated bank account",
            content = @Content(
                    schema = @Schema(implementation = AccountUpdateDTO.class),
                    examples = @ExampleObject(value = "{\"account_id\":2,\"balance\":10000}")))
    @ApiResponse(responseCode = "400", description = "You do not have a bank account", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)

    @PostMapping("/account/replenish")
    public AccountUpdateDTO replenishAccount(@Valid @RequestBody AccountUpdateRequest accountRequest,
                                             @AuthenticationPrincipal UserAdapter user) {
        return budgetPlanningService.replenishAccount(accountRequest, user.getUser());
    }

    @Operation(summary = "Withdraw money from bank account, authorization required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Updated bank account",
            content = @Content(
                    schema = @Schema(implementation = AccountUpdateDTO.class),
                    examples = @ExampleObject(value = "{\"account_id\":2,\"balance\":10000}")))
    @ApiResponse(responseCode = "400", description = "You do not have a bank account, you can't withdraw that much money or" +
            "balance will become zero after operation", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)

    @PostMapping("/account/withdraw")
    public AccountUpdateDTO withdrawAccount(@Valid @RequestBody AccountUpdateRequest accountRequest,
                                            @AuthenticationPrincipal UserAdapter user) {
        return budgetPlanningService.withdrawAccount(accountRequest, user.getUser());
    }

    @Operation(summary = "Set new usage limit for a child, Parent or Admin role required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Updated user",
            content = @Content(
                    schema = @Schema(implementation = UserWithLimitDTO.class),
                    examples = @ExampleObject(value = "{\"name\":\"vova\",\"email\":\"vova@gmail.com\",\"usage_limit\":10}")))
    @ApiResponse(responseCode = "400", description = "No user with such username, or " +
            "you can't change limit of this user", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Wrong role", content = @Content)

    @PostMapping("/user/limit")
    public UserWithLimitDTO updateLimit(@Valid @RequestBody LimitUpdateRequest limitRequest,
                                        @AuthenticationPrincipal UserAdapter user) {
        return budgetPlanningService.updateLimit(limitRequest, user.getUser());
    }

    @Operation(summary = "Get your bank account history for the last month, Parent or Admin role required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "List of account transactions",
            content = @Content(
                    schema = @Schema(implementation = BankHistoryDTO.class),
                    examples = @ExampleObject(value = "[{\"operation\":\"replenish\",\"reason\":\"payday\"," +
                            "\"timestamp\":\"2024-05-19T09:01:06\",\"amount\":1100,\"user\":" +
                            "{\"name\":\"vova\",\"email\":\"vova@gmail.com\",\"usage_limit\":100}}]")))
    @ApiResponse(responseCode = "400", description = "You do not have a bank account, or " +
            "no transactions have been performed for this account", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Wrong role", content = @Content)

    @GetMapping("/account/history")
    public List<BankHistoryDTO> getAccountHistory(@AuthenticationPrincipal UserAdapter user) {
        return budgetPlanningService.getAccountHistory(user.getUser());
    }

    @Operation(summary = "Update the user's bank account, Admin role required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "Updated user",
            content = @Content(
                    schema = @Schema(implementation = UserDTO.class),
                    examples = @ExampleObject(value = "{\"id\":1,\"name\":1000,\"email\":\"vova@gmail.com\"," +
                            "\"role\":\"PARENT\",\"usage_limit\":100,\"bankAccount\":" +
                            "{\"id\":1,\"balance\":1000}}")))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Wrong role", content = @Content)

    @PostMapping("/user/account")
    public UserDTO updateBankAccount(@Valid @RequestBody UpdateUserRequest userRequest) {
        return budgetPlanningService.updateBankAccount(userRequest);
    }

    @Operation(summary = "Get all bank accounts, Admin role required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "List of all accounts",
            content = @Content(
                    schema = @Schema(implementation = BankAccountDTO.class),
                    examples = @ExampleObject(value = "[{\"id\":1,\"balance\":1000},{\"id\":2,\"balance\":22000}]")))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Wrong role", content = @Content)

    @GetMapping("/account/all")
    public List<BankAccountDTO> getAllAccounts() {
        return budgetPlanningService.getAllAccounts();
    }

    @Operation(summary = "Delete bank account and all its history, Admin role required",
            security = @SecurityRequirement(name = "basicAuth"))
    @ApiResponse(responseCode = "200", description = "The account was deleted", content = @Content)
    @ApiResponse(responseCode = "400", description = "Wrong id", content = @Content)
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    @ApiResponse(responseCode = "403", description = "Wrong role", content = @Content)

    @DeleteMapping("/account/delete") //link looks like/account/delete?id=1
    public ResponseEntity<String> deleteAccount(@Parameter(description = "Book ID for return")
                                                  @RequestParam Long id) {
        if (budgetPlanningService.deleteAccount(id)) {
            return new ResponseEntity<>("Account deleted", HttpStatus.OK);
        }

        return new ResponseEntity<>("Wrong id", HttpStatus.BAD_REQUEST);
    }


}
