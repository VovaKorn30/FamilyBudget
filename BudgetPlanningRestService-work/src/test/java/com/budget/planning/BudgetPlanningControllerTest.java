package com.budget.planning;

import com.budget.planning.configuration.security.Role;
import com.budget.planning.configuration.security.SecurityConfig;
import com.budget.planning.configuration.security.UserAdapter;
import com.budget.planning.controller.BudgetPlanningController;
import com.budget.planning.dto.request.*;
import com.budget.planning.dto.response.*;
import com.budget.planning.exception.AccountUpdateException;
import com.budget.planning.exception.BankHistoryException;
import com.budget.planning.exception.LimitUpdateException;
import com.budget.planning.model.BankAccount;
import com.budget.planning.model.User;
import com.budget.planning.service.BudgetPlanningService;
import com.budget.planning.service.UserDetailsServiceImp;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BudgetPlanningController.class)
@Import(SecurityConfig.class)
public class BudgetPlanningControllerTest {
    @Autowired
    MockMvc mockMvc;

    @MockBean
    BudgetPlanningService budgetPlanningService;
    @MockBean
    UserDetailsServiceImp userDetailsService;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("Test for POST /user/register endpoint")
    void testRegisterEndpoint() throws Exception {
        var registrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "parent",
                1L
        );
        var responseEntity = new ResponseEntity<>("Successfully registered, your email is your username", HttpStatus.OK);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully registered, your email is your username"));
    }

    @Test
    @DisplayName("Test for POST /user/register endpoint(no bank account)")
    void testRegisterEndpoint_NoBankAccount() throws Exception {
        var registrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "parent",
                0L
        );
        var responseEntity = new ResponseEntity<>("Successfully registered, your email is your username." +
                "But bank account not found, ask admin for help or create a new one", HttpStatus.OK);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("Successfully registered, your email is your username." +
                                        "But bank account not found, ask admin for help or create a new one"));
    }

    @Test
    @DisplayName("Test for POST /user/register endpoint(wrong role provided)")
    void testRegisterEndpoint_WrongRole() throws Exception {
        var registrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "user",
                1L
        );
        var responseEntity = new ResponseEntity<>("Wrong role provided",
                HttpStatus.BAD_REQUEST);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wrong role provided"));
    }

    @Test
    @DisplayName("Test for POST /user/register endpoint(such a user already registered)")
    void testRegisterEndpoint_AlreadyRegistered() throws Exception {
        var registrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "parent",
                1L
        );
        var responseEntity = new ResponseEntity<>("Such a user already exists!",
                HttpStatus.BAD_REQUEST);

        when(userDetailsService.register(registrationRequest))
                .thenReturn(responseEntity);

        var requestBuilder = post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(registrationRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Such a user already exists!"));
    }

    @Test
    @DisplayName("Test for POST /account/register endpoint")
    void testRegisterAccountEndpoint() throws Exception {
        var accountRequest = AccountRegistrationRequest.builder().balance(100).build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(100).build();

        when(budgetPlanningService.registerAccount(accountRequest, user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @DisplayName("Test for POST /account/replenish endpoint")
    void testReplenishAccountEndpoint() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(100).reason("payday").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(100)
                .bankAccount(BankAccount.builder().id(1L).balance(100).build())
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(200).build();

        when(budgetPlanningService.replenishAccount(accountRequest, user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/replenish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @DisplayName("Test for POST /account/replenish endpoint(no bank account)")
    void testReplenishAccountEndpoint_NoBankAccount() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(100).reason("payday").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(100)
                .bankAccount(null)
                .build();

        when(budgetPlanningService.replenishAccount(accountRequest, user))
                .thenThrow(new AccountUpdateException("You do not have a bank account!"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/replenish")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not have a bank account!"));
    }

    @Test
    @DisplayName("Test for POST /account/withdraw endpoint")
    void testWithdrawAccountEndpoint() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(1000)
                .bankAccount(BankAccount.builder().id(1L).balance(200).build())
                .build();

        var expect = AccountUpdateDTO.builder().account_id(1L).balance(100).build();

        when(budgetPlanningService.withdrawAccount(accountRequest, user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @DisplayName("Test for POST /account/withdraw endpoint(no bank account)")
    void testWithdrawAccountEndpoint_NoBankAccount() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(1000)
                .bankAccount(null)
                .build();

        when(budgetPlanningService.withdrawAccount(accountRequest, user))
                .thenThrow(new AccountUpdateException("You do not have a bank account!"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not have a bank account!"));
    }

    @Test
    @DisplayName("Test for POST /account/withdraw endpoint(limit reached)")
    void testWithdrawAccountEndpoint_LimitReached() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(1)
                .bankAccount(BankAccount.builder().id(1L).balance(1000).build())
                .build();

        when(budgetPlanningService.withdrawAccount(accountRequest, user))
                .thenThrow(new AccountUpdateException("Your usage limit does not allow you to perform this operation"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Your usage limit does not allow you to perform this operation"));
    }

    @Test
    @DisplayName("Test for POST /account/withdraw endpoint(negative balance)")
    void testWithdrawAccountEndpoint_NegativeBalance() throws Exception {
        var accountRequest = AccountUpdateRequest.builder().amount(1000).reason("notebook").build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.CHILD)
                .usage_limit(1000)
                .bankAccount(BankAccount.builder().id(1L).balance(100).build())
                .build();

        when(budgetPlanningService.withdrawAccount(accountRequest, user))
                .thenThrow(new AccountUpdateException("Balance can not become less than zero after operation"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/account/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(accountRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Balance can not become less than zero after operation"));
    }

    @Test
    @DisplayName("Test for POST /user/limit endpoint")
    void testUserLimitEndpoint() throws Exception {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);
        var bankAccount = BankAccount.builder().id(1L).balance(200).build();
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1000)
                .bankAccount(bankAccount)
                .build();

        var expect = UserWithLimitDTO.builder()
                .name("vova")
                .email("vova2@gmail.com")
                .usage_limit(1)
                .build();

        when(budgetPlanningService.updateLimit(limitRequest, user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/user/limit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(limitRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @DisplayName("Test for POST /user/limit endpoint(no user)")
    void testUserLimitEndpointEndpoint_NoUser() throws Exception {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1000)
                .bankAccount(null)
                .build();

        when(budgetPlanningService.updateLimit(limitRequest, user))
                .thenThrow(new LimitUpdateException("No user with such username"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/user/limit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(limitRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No user with such username"));
    }

    @Test
    @DisplayName("Test for POST /user/limit endpoint(can not change limit)")
    void testUserLimitEndpoint_CanNotChangeLimit() throws Exception {
        var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1)
                .bankAccount(BankAccount.builder().id(1L).balance(1000).build())
                .build();

        when(budgetPlanningService.updateLimit(limitRequest, user))
                .thenThrow(new LimitUpdateException("You can't change limit of this user"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = post("/user/limit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(limitRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You can't change limit of this user"));
    }

    @Test
    @DisplayName("Test for GET /account/history endpoint")
    void testAccountHistoryEndpoint() throws Exception {
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1)
                .bankAccount(BankAccount.builder().id(1L).balance(1000).build())
                .build();

        var expect = List.of(BankHistoryDTO.builder()
                .operation("withdraw").reason("notebook").amount(10)
                .user(UserWithLimitDTO.builder().name("vova").email("vova@gmail.com").usage_limit(100).build())
                .timestamp(LocalDateTime.of(2019, 1, 1, 1, 1))
                .build());

        when(budgetPlanningService.getAccountHistory(user)).thenReturn(expect);
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = get("/account/history");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string((mapper.writeValueAsString(expect))));
    }

    @Test
    @DisplayName("Test for GET /account/history endpoint(no bank account)")
    void testAccountHistoryEndpoint_NoBankAccount() throws Exception {
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.PARENT)
                .usage_limit(1)
                .bankAccount(null)
                .build();

        when(budgetPlanningService.getAccountHistory(user))
                .thenThrow(new BankHistoryException("You do not have a bank account!"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = get("/account/history");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("You do not have a bank account!"));
    }

    @Test
    @DisplayName("Test for GET /account/history endpoint(no transactions)")
    void testAccountHistoryEndpoint_NoTransactions() throws Exception {
        var user = User.builder()
                .name("vova")
                .email("vova@gmail.com")
                .password("1234")
                .role(Role.ADMIN)
                .usage_limit(1)
                .bankAccount(BankAccount.builder().id(1L).balance(1000).build())
                .build();

        when(budgetPlanningService.getAccountHistory(user))
                .thenThrow(new BankHistoryException("No transactions have been performed for this account"));
        SecurityContextHolder.getContext().setAuthentication(new PreAuthenticatedAuthenticationToken(
                new UserAdapter(user), null, List.of(new SimpleGrantedAuthority(user.getRole().toString()))
        ));

        var requestBuilder = get("/account/history");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("No transactions have been performed for this account"));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Test for POST /user/account endpoint")
    void testUserAccountEndpoint() throws Exception {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);

        var expect = UserDTO.builder()
                .user_id(1L)
                .name("vova")
                .email("vova@gmail.com")
                .role(Role.ADMIN.toString())
                .usage_limit(100)
                .bankAccount(BankAccountDTO.builder().id(1L).balance(100).build())
                .build();

        when(budgetPlanningService.updateBankAccount(userRequest))
                .thenReturn(expect);

        var requestBuilder = post("/user/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(userRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(expect)));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Test for POST /user/account endpoint(no user)")
    void testUserAccountEndpoint_NoUser() throws Exception {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);

        when(budgetPlanningService.updateBankAccount(userRequest))
                .thenThrow(new AccountUpdateException("There are no users with that username"));

        var requestBuilder = post("/user/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(userRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("There are no users with that username"));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("est for POST /user/account endpoint(wrong id)")
    void testUserAccountEndpoint_WrondId() throws Exception {
        var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);

        when(budgetPlanningService.updateBankAccount(userRequest))
                .thenThrow(new AccountUpdateException("There are no bank account with that id"));

        var requestBuilder = post("/user/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(userRequest));
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("There are no bank account with that id"));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Test for GET /account/all endpoint")
    void testAccountAllEndpoint() throws Exception {
        var expect = List.of(BankAccountDTO.builder().id(1L).balance(100).build(),
                BankAccountDTO.builder().id(2L).balance(2200).build());

        when(budgetPlanningService.getAllAccounts())
                .thenReturn(expect);

        var requestBuilder = get("/account/all");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(mapper.writeValueAsString(expect)));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Test for DELETE /account/delete endpoint")
    void testDeleteAccountEndpoint() throws Exception {
        when(budgetPlanningService.deleteAccount(1L)).thenReturn(true);

        var requestBuilder = delete("/account/delete?id=1");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string("Account deleted"));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Test for DELETE /account/delete endpoint(wrond if)")
    void testDeleteAccountEndpoint_WrongId() throws Exception {
        when(budgetPlanningService.deleteAccount(1L)).thenReturn(false);

        var requestBuilder = delete("/account/delete?id=1");
        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Wrong id"));
    }

    @Test
    @WithMockUser(username = "vova@gmail.com", password = "1234", authorities = "ADMIN")
    @DisplayName("Validation test")
    void testValidation() throws Exception {
        var requestNoAccountId = post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"vova\",\"email\":\"vova@gmail.com\"," +
                        "\"password\":\"1234\",\"role\":\"parent\"}");
        mockMvc.perform(requestNoAccountId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Write down your bank account id!"));

        var requestNoUserName = post("/user/account")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"account_id\":25}");
        mockMvc.perform(requestNoUserName)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Write down username!"));

        var requestWrongAccountId = post("/user/limit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"vova@gmail.com\",\"limit\":-25}");
        mockMvc.perform(requestWrongAccountId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Write down new limit!"));
    }
}
