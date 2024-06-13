package com.budget.planning;

import com.budget.planning.configuration.security.Role;
import com.budget.planning.dto.request.AccountRegistrationRequest;
import com.budget.planning.dto.request.AccountUpdateRequest;
import com.budget.planning.dto.request.LimitUpdateRequest;
import com.budget.planning.dto.request.UpdateUserRequest;
import com.budget.planning.dto.response.*;
import com.budget.planning.exception.AccountUpdateException;
import com.budget.planning.exception.BankHistoryException;
import com.budget.planning.exception.LimitUpdateException;
import com.budget.planning.model.BankAccount;
import com.budget.planning.model.BankHistory;
import com.budget.planning.model.User;
import com.budget.planning.repository.BankAccountRepository;
import com.budget.planning.repository.BankHistoryRepository;
import com.budget.planning.repository.UserRepository;
import com.budget.planning.service.BudgetPlanningService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BudgetPlanningServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    BankHistoryRepository bankHistoryRepository;
    @Mock
    BankAccountRepository bankAccountRepository;

    @InjectMocks
    BudgetPlanningService budgetPlanningService;

    @Test
    @DisplayName("Test for registerAccount() method")
    void testRegisterAccount() {
        var registerRequest = AccountRegistrationRequest.builder().balance(0).build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(0).build();

        when(bankAccountRepository.save(BankAccount.builder().balance(0).build()))
                .thenReturn(BankAccount.builder().id(1L).balance(0).build());

        assertThat(budgetPlanningService.registerAccount(registerRequest, user))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for replenishAccount() method")
    void testReplenishAccount() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("payday").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(20).build();

        assertThat(budgetPlanningService.replenishAccount(updateRequest, user))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for replenishAccount() method(no bank account)")
    void testReplenishAccount_NoBankAccount() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("payday").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        assertThatThrownBy(() -> budgetPlanningService.replenishAccount(updateRequest, user))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("You do not have a bank account!");
    }

    @Test
    @DisplayName("Test for withdrawAccount() method")
    void testWithdrawAccount() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("notebook").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(0).build();

        assertThat(budgetPlanningService.withdrawAccount(updateRequest, user))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for withdrawAccount() method(no bank account)")
    void testWithdrawAccount_NoBankAccount() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("notebook").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        assertThatThrownBy(() -> budgetPlanningService.withdrawAccount(updateRequest, user))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("You do not have a bank account!");
    }

    @Test
    @DisplayName("Test for withdrawAccount() method(limit reached)")
    void testWithdrawAccount_LimitReached() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("notebook").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        assertThatThrownBy(() -> budgetPlanningService.withdrawAccount(updateRequest, user))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("Your usage limit does not allow you to perform this operation");
    }

    @Test
    @DisplayName("Test for withdrawAccount() method(negative balance)")
    void testWithdrawAccount_NegativeBalance() {
        var updateRequest = AccountUpdateRequest.builder().amount(10).reason("notebook").build();
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(5).build())
                .build();

        assertThatThrownBy(() -> budgetPlanningService.withdrawAccount(updateRequest, user))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("Balance can not become less than zero after operation");
    }

    @Test
    @DisplayName("Test for updateLimit() method")
    void testUpdateLimit() {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 10);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();
        var child = User.builder()
                .user_id(2L)
                .name("vova")
                .email("vova2@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(10000)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        var expect = UserWithLimitDTO.builder()
                .name("vova")
                .email("vova2@gmail.com")
                .usage_limit(10)
                .build();

        when(userRepository.findUserByEmail("vova2@gmail.com"))
                .thenReturn(Optional.ofNullable(child));

        assertThat(budgetPlanningService.updateLimit(limitRequest, user))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for updateLimit() method(no user)")
    void testUpdateLimit_NoUser() {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 10);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        when(userRepository.findUserByEmail("vova2@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetPlanningService.updateLimit(limitRequest, user))
                .isInstanceOf(LimitUpdateException.class)
                .hasMessage("No user with such username");
    }

    @Test
    @DisplayName("Test for updateLimit() method(can not change limit)")
    void testUpdateLimit_CanNotChangeLimit() {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 10);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1)
                .bankAccount(null)
                .build();

        var child = User.builder()
                .user_id(2L)
                .name("vova")
                .email("vova2@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(10000)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        when(userRepository.findUserByEmail("vova2@gmail.com"))
                .thenReturn(Optional.ofNullable(child));

        assertThatThrownBy(() -> budgetPlanningService.updateLimit(limitRequest, user))
                .isInstanceOf(LimitUpdateException.class)
                .hasMessage("You can't change limit of this user");
    }

    @Test
    @DisplayName("Test for getAccountHistory() method")
    void testGetAccountHistory() {
        var bankAccount = BankAccount.builder().id(1L).balance(10).build();
        var dateTime = LocalDateTime.of(2019, 1, 1, 1, 1);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(bankAccount)
                .build();

        var expect = List.of(BankHistoryDTO.builder()
                .operation("withdraw").reason("notebook").amount(10).timestamp(dateTime)
                .user(UserWithLimitDTO.builder().name("vova").email("vova@gmail.com").usage_limit(100).build())
                .build());

        when(bankHistoryRepository.findAllHistoriesByBankAccountForLastMonth(user.getBankAccount()))
                .thenReturn(List.of(BankHistory.builder()
                        .operation("withdraw").reason("notebook")
                        .amount(10).bankAccount(bankAccount)
                        .timestamp(dateTime).user(user).build()));

        assertThat(budgetPlanningService.getAccountHistory(user))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for getAccountHistory() method(no bank account)")
    void testGetAccountHistory_NoBankAccount() {
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        assertThatThrownBy(() -> budgetPlanningService.getAccountHistory(user))
                .isInstanceOf(BankHistoryException.class)
                .hasMessage("You do not have a bank account!");
    }

    @Test
    @DisplayName("Test for getAccountHistory() method(no transactions)")
    void testGetAccountHistory_NoTransactions() {
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(10).build())
                .build();

        when(bankHistoryRepository.findAllHistoriesByBankAccountForLastMonth(user.getBankAccount()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> budgetPlanningService.getAccountHistory(user))
                .isInstanceOf(BankHistoryException.class)
                .hasMessage("No transactions have been performed for this account");
    }

    @Test
    @DisplayName("Test for updateBankAccount() method")
    void testUpdateBankAccount() {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        var expect = UserDTO.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .role(Role.ADMIN.toString())
                .usage_limit(100)
                .bankAccount(BankAccountDTO.builder().id(1L).balance(100).build())
                .build();

        when(userRepository.findUserByEmail("vova@gmail.com"))
                .thenReturn(Optional.ofNullable(user));
        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.ofNullable(BankAccount.builder().id(1L).balance(100).build()));

        assertThat(budgetPlanningService.updateBankAccount(userRequest))
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for updateBankAccount() method(no user)")
    void testUpdateBankAccount_NoUser() {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);

        when(userRepository.findUserByEmail("vova@gmail.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetPlanningService.updateBankAccount(userRequest))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("There are no users with that username");
    }

    @Test
    @DisplayName("Test for updateBankAccount() method(wrong id)")
    void testUpdateBankAccount_WrondId() {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);
        var user = User.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        when(userRepository.findUserByEmail("vova@gmail.com"))
                .thenReturn(Optional.ofNullable(user));
        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetPlanningService.updateBankAccount(userRequest))
                .isInstanceOf(AccountUpdateException.class)
                .hasMessage("There are no bank account with that id");
    }

    @Test
    @DisplayName("Test for getAllAccounts() method")
    void testGetAllAccounts() {
        var expect = List.of(BankAccountDTO.builder().id(1L).balance(100).build(),
                BankAccountDTO.builder().id(2L).balance(2200).build());

        when(bankAccountRepository.findAll())
                .thenReturn(List.of(BankAccount.builder().id(1L).balance(100).build(),
                        BankAccount.builder().id(2L).balance(2200).build()));

        assertThat(budgetPlanningService.getAllAccounts())
                .isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for deleteAccount() method")
    void testDeleteAccount() {
        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.ofNullable(BankAccount.builder().id(1L).balance(100).build()));

        assertThat(budgetPlanningService.deleteAccount(1L))
                .isEqualTo(true);
    }

    @Test
    @DisplayName("Test for deleteAccount() method(wrong id)")
    void testDeleteAccount_WrongId() {
        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThat(budgetPlanningService.deleteAccount(1L))
                .isEqualTo(false);
    }
}
