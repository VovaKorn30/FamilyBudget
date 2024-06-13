package com.budget.planning;

import com.budget.planning.configuration.security.Role;
import com.budget.planning.model.BankHistory;
import com.budget.planning.model.BankAccount;
import com.budget.planning.model.User;
import com.budget.planning.repository.BankAccountRepository;
import com.budget.planning.repository.BankHistoryRepository;
import com.budget.planning.repository.UserRepository;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(locations = {"classpath:testApp.properties"}) //for tests, it is better to use H2 db
@Testcontainers
public class BudgetPlanningRepositoryTest {
    @Container
    private static final MySQLContainer<?> mysqlcontainer = new MySQLContainer<>("mysql:latest");

    @Autowired
    UserRepository userRepository;

    @Autowired
    BankAccountRepository bankAccountRepository;

    @Autowired
    BankHistoryRepository bankHistoryRepository;

    @Autowired
    TestEntityManager entityManager;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url", mysqlcontainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlcontainer::getUsername);
        registry.add("spring.datasource.password", mysqlcontainer::getPassword);
    }

    @Test
    @DisplayName("Test for save() method in UserRepository")
    void saveTest_UserRepository() {
        User expect = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .build();
        userRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<User> actual = userRepository.findById(expect.getUser_id());
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for findByEmail() method in UserRepository")
    @Sql(statements = {"INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
            "VALUES (1, 'vova', 'vova@gmail.com', '1234', 2, 100)"})
    void findByEmailTest_UserRepository() {
        User expect = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .build();

        Optional<User> actual = userRepository.findUserByEmail("vova@gmail.com");
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for findAllUsersByBankAccount() method in UserRepository")
    @Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1)",
            "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
                "VALUES (1, 'vova', 'vova@gmail.com', '1234', 2, 100, 1)," +
                    "(2, 'vova', 'vova2@gmail.com', '1234', 0, 100, null)"})
    void findAllUsersByBankAccountTest_UserRepository() {
        BankAccount bankAccount = BankAccount.builder().id(1L).balance(100).build();
        User expect = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(bankAccount)
                .build();

        List<User> actual = userRepository.findAllUsersByBankAccount(bankAccount);
        assertThat(actual)
                .hasOnlyElementsOfType(User.class)
                .hasSize(1)
                .contains(expect, Index.atIndex(0));
    }

    @Test
    @DisplayName("Test for save() method in BankAccountRepository")
    void saveTest_BankAccountRepository() {
        var expect = BankAccount.builder().balance(10).build();
        bankAccountRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<BankAccount> actual = bankAccountRepository.findById(expect.getId());
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for save() method in BankHistoryRepository")
    @Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1)",
            "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
                "VALUES (1, 'vova', 'vova@gmail.com', '1234', 2, 100, 1)"})
    void saveTest_BankHistoryRepository() {
        BankAccount bankAccount = BankAccount.builder().id(1L).balance(100).build();
        var expect = BankHistory.builder()
                .timestamp(LocalDateTime.now().withNano(0))
                .operation("replenish")
                .reason("payday")
                .amount(100)
                .bankAccount(bankAccount)
                .user(User.builder()
                        .user_id(1L)
                        .name("vova")
                        .email("vova@gmail.com")
                        .password("1234")
                        .role(Role.ADMIN)
                        .usage_limit(100)
                        .bankAccount(bankAccount)
                        .build())
                .build();
        bankHistoryRepository.save(expect);

        entityManager.clear(); //to clear cache

        Optional<BankHistory> actual = bankHistoryRepository.findById(expect.getId());
        assertThat(actual)
                .isPresent()
                .get()
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for findAllHistoriesByBankAccount() method in BankHistoryRepository")
    @Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1)",
            "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
                    "VALUES (1, 'vova', 'vova@gmail.com', '1234', 2, 100, 1)",
            "INSERT INTO bankhistory(id, timestamp, operation, reason, amount, user_id, account_id)" +
                    "VALUES (1, '2019-5-3T12:12:00', 'replenish', 'payday', 100, 1, 1)"
    })
    void findAllHistoriesByBankAccountTest_BankHistoryRepository() {
        BankAccount bankAccount = BankAccount.builder().id(1L).balance(100).build();
        var expect = BankHistory.builder()
                .id(1L)
                .timestamp(LocalDateTime.of(2019, 5, 3, 12, 12))
                .operation("replenish")
                .reason("payday")
                .amount(100)
                .bankAccount(bankAccount)
                .user(User.builder()
                        .user_id(1L)
                        .name("vova")
                        .email("vova@gmail.com")
                        .password("1234")
                        .role(Role.ADMIN)
                        .usage_limit(100)
                        .bankAccount(bankAccount)
                        .build())
                .build();

        List<BankHistory> actual = bankHistoryRepository.findAllHistoriesByBankAccount(bankAccount);
        assertThat(actual)
                .hasOnlyElementsOfType(BankHistory.class)
                .hasSize(1)
                .contains(expect, Index.atIndex(0));
    }

    @Test
    @DisplayName("Test for findAllHistoriesByBankAccountForLastMonth() method in BankHistoryRepository")
    @Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1), (200, 2)",
            "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
                    "VALUES (1, 'vova', 'vova@gmail.com', '1234', 2, 100, 1)",
            "INSERT INTO bankhistory(id, timestamp, operation, reason, amount, user_id, account_id)" +
                    "VALUES (1, '19-5-3T12:12:00', 'replenish', 'payday', 100, 1, 1)," +
                        " (2, '19-6-14T14:35:00', 'replenish', 'payday', 100, 1, 2)"
    })
    void findAllHistoriesByBankAccountForLastMonthTest_BankHistoryRepository() {
        BankAccount bankAccount = BankAccount.builder().id(1L).balance(100).build();

        List<BankHistory> actual = bankHistoryRepository.findAllHistoriesByBankAccountForLastMonth(bankAccount);
        assertThat(actual)
                .hasOnlyElementsOfType(BankHistory.class)
                .hasSize(0);
    }
}
