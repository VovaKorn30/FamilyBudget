package com.budget.planning.repository;

import com.budget.planning.model.BankAccount;
import com.budget.planning.model.BankHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BankHistoryRepository extends JpaRepository<BankHistory,Long> {
    default List<BankHistory> findAllHistoriesByBankAccount(BankAccount bankAccount) {
        return findAll().stream()
                .filter(b -> bankAccount.equals(b.getBankAccount()))
                .toList();
    }

    default List<BankHistory> findAllHistoriesByBankAccountForLastMonth(BankAccount bankAccount) {
        LocalDateTime now = LocalDateTime.now().minusMonths(1L);
        return findAll().stream()
                .filter(b -> bankAccount.equals(b.getBankAccount())
                        && b.getTimestamp().isAfter(now))
                .toList();
    }
}
