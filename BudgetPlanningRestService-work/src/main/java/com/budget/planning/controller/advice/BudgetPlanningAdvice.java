package com.budget.planning.controller.advice;

import com.budget.planning.exception.AccountUpdateException;
import com.budget.planning.exception.BankHistoryException;
import com.budget.planning.exception.LimitUpdateException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.Optional;

@RestControllerAdvice
public class BudgetPlanningAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleArgumentNotValid(MethodArgumentNotValidException exception) {
        Optional<String> message = Optional.ofNullable(
                exception.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
        return Map.of("error", message.orElse(exception.getMessage()));
    }

    @ExceptionHandler(AccountUpdateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleAccountUpdateException(AccountUpdateException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(LimitUpdateException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleLimitUpdateException(LimitUpdateException exception) {
        return Map.of("error", exception.getMessage());
    }

    @ExceptionHandler(BankHistoryException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBankHistoryException(BankHistoryException exception) {
        return Map.of("error", exception.getMessage());
    }
}
