package com.budget.planning.repository;

import com.budget.planning.model.BankAccount;
import com.budget.planning.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findUserByEmail(String email);

    default List<User> findAllUsersByBankAccount(BankAccount bankAccount) {
        return findAll().stream()
                .filter(u -> bankAccount.equals(u.getBankAccount()))
                .toList();
    }
}
