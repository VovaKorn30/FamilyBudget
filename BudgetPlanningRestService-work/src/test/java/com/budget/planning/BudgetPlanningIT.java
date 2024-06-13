package com.budget.planning;

import com.budget.planning.configuration.security.Role;
import com.budget.planning.dto.request.*;
import com.budget.planning.dto.response.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mockStatic;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(locations = {"classpath:testApp.properties"})
@Testcontainers
class BudgetPlanningIT {
	@Container
	private static final MySQLContainer<?> mysqlcontainer = new MySQLContainer<>("mysql:latest");

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper mapper;

	final RequestPostProcessor postProcessor = SecurityMockMvcRequestPostProcessors
			.httpBasic("vova@gmail.com", "1234");
	final String createParent = "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
			"VALUES (1, 'vova', 'vova@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 100, 1)";
	final String createChild =  "INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
			"VALUES (1, 'vova', 'vova@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 1, 100, 1)";
	final String createAdmin =  "INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
			"VALUES (1, 'vova', 'vova@gmail.com', '$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 2, 100)";

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry){
		registry.add("spring.datasource.url", mysqlcontainer::getJdbcUrl);
		registry.add("spring.datasource.username", mysqlcontainer::getUsername);
		registry.add("spring.datasource.password", mysqlcontainer::getPassword);
	}

	@Test
	@DisplayName("Test for POST /user/register endpoint")
	@Sql(statements = "INSERT INTO bankaccount(balance, id) VALUES (100, 1)")
	void testRegisterEndpoint() throws Exception {
		var registrationRequest = new UserRegistrationRequest(
				"vova",
				"vova@gmail.com",
				"1234",
				"parent",
				1L
		);

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

		var requestBuilder = post("/user/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(registrationRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Wrong role provided"));
	}

	@Test
	@DisplayName("Test for POST /user/register endpoint(such a user already registered)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1)", createParent})
	void testRegisterEndpoint_AlreadyRegistered() throws Exception {
		var registrationRequest = new UserRegistrationRequest(
				"vova",
				"vova@gmail.com",
				"1234",
				"parent",
				1L
		);

		var requestBuilder = post("/user/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(registrationRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Such a user already exists!"));
	}

	@Test
	@DisplayName("Test for POST /account/register endpoint")
	@Sql(statements = {"ALTER TABLE bankaccount AUTO_INCREMENT = 0", //reset primary
					"INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
						"VALUES (1, 'vova', 'vova@gmail.com', " +
							"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 1, 100)"})
	void testRegisterAccountEndpoint() throws Exception {
		var accountRequest = AccountRegistrationRequest.builder().balance(100).build();

		var expect = AccountUpdateDTO.builder().account_id(1L).balance(100).build();

		var requestBuilder = post("/account/register")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@DisplayName("Test for POST /account/replenish endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1)", createChild})
	void testReplenishAccountEndpoint() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(100).reason("payday").build();

		var expect = AccountUpdateDTO.builder().account_id(1L).balance(200).build();

		var requestBuilder = post("/account/replenish")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@DisplayName("Test for POST /account/replenish endpoint(no bank account)")
	@Sql(statements = "INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
			"VALUES (1, 'vova', 'vova@gmail.com', " +
			"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 100)")
	void testReplenishAccountEndpoint_NoBankAccount() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(100).reason("payday").build();

		var requestBuilder = post("/account/replenish")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You do not have a bank account!"));
	}

	@Test
	@DisplayName("Test for POST /account/withdraw endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)", createChild})
	void testWithdrawAccountEndpoint() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();

		var expect = AccountUpdateDTO.builder().account_id(1L).balance(100).build();

		var requestBuilder = post("/account/withdraw")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@DisplayName("Test for POST /account/withdraw endpoint(no bank account)")
	@Sql(statements = "INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
			"VALUES (1, 'vova', 'vova@gmail.com', " +
			"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 100)")
	void testWithdrawAccountEndpoint_NoBankAccount() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();

		var requestBuilder = post("/account/withdraw")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You do not have a bank account!"));
	}

	@Test
	@DisplayName("Test for POST /account/withdraw endpoint(limit reached)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)",
			"INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
					"VALUES (1, 'vova', 'vova@gmail.com', " +
					"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 1, 1)"})
	void testWithdrawAccountEndpoint_LimitReached() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(100).reason("notebook").build();

		var requestBuilder = post("/account/withdraw")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error")
						.value("Your usage limit does not allow you to perform this operation"));
	}

	@Test
	@DisplayName("Test for POST /account/withdraw endpoint(negative balance)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (20, 1)", createChild})
	void testWithdrawAccountEndpoint_NegativeBalance() throws Exception {
		var accountRequest = AccountUpdateRequest.builder().amount(30).reason("notebook").build();

		var requestBuilder = post("/account/withdraw")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(accountRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error")
						.value("Balance can not become less than zero after operation"));
	}

	@Test
	@DisplayName("Test for POST /user/limit endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)", createParent,
			"INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
					"VALUES (2, 'vova', 'vova2@gmail.com', " +
					"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 1, 100, 1)"})
	void testUserLimitEndpoint() throws Exception {
		var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);

		var expect = UserWithLimitDTO.builder()
				.name("vova")
				.email("vova2@gmail.com")
				.usage_limit(1)
				.build();

		var requestBuilder = post("/user/limit")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(limitRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string((mapper.writeValueAsString(expect))));
	}

	@Test
	@DisplayName("Test for POST /user/limit endpoint(no user)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)", createParent})
	void testUserLimitEndpointEndpoint_NoUser() throws Exception {
		var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);

		var requestBuilder = post("/user/limit")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(limitRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("No user with such username"));
	}

	@Test
	@DisplayName("Test for POST /user/limit endpoint(can not change limit)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1), (100, 2)", createParent,
			"INSERT INTO user(user_id, name, email, password, role, usage_limit, account_id) " +
					"VALUES (2, 'vova', 'vova2@gmail.com', " +
					"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 100, 2)"})
	void testUserLimitEndpoint_CanNotChangeLimit() throws Exception {
		var limitRequest = new LimitUpdateRequest("vova2@gmail.com", 1);

		var requestBuilder = post("/user/limit")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(limitRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You can't change limit of this user"));
	}

	@Test
	@DisplayName("Test for GET /account/history endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)", createParent,
			"INSERT INTO bankhistory(id, timestamp, operation, reason, amount, user_id, account_id) " +
					"VALUES (1, '2019-02-02 12:12:12.000000', 'withdraw', 'notebook', 10, 1, 1)"})
	void testAccountHistoryEndpoint() throws Exception {
		var time = LocalDateTime.of(2019, 2, 2, 12, 12, 12);
		var now = LocalDateTime.of(2019, 2, 22, 12, 12, 12);
		var expect = List.of(BankHistoryDTO.builder()
				.operation("withdraw").reason("notebook").amount(10)
				.user(UserWithLimitDTO.builder().name("vova").email("vova@gmail.com").usage_limit(100).build())
				.timestamp(time).build());

		try (var mock = mockStatic(LocalDateTime.class)) {
			Mockito.when(LocalDateTime.now()).thenReturn(now);
			Mockito.when(LocalDateTime.of(2019, 2, 2, 12, 12, 12, 0))
					.thenReturn(time);

			var requestBuilder = get("/account/history").with(postProcessor);
			mockMvc.perform(requestBuilder)
					.andExpect(status().isOk())
					.andExpect(content().string((mapper.writeValueAsString(expect))));
		}
	}

	@Test
	@DisplayName("Test for GET /account/history endpoint(no bank account)")
	@Sql(statements = "INSERT INTO user(user_id, name, email, password, role, usage_limit) " +
			"VALUES (1, 'vova', 'vova@gmail.com', " +
			"'$2a$10$Hzdg8upvCxY8wqZAyq79Ou1szV6sS6Xy55GmDyOqgz8ZKbMsklZ1C', 0, 100)")
	void testAccountHistoryEndpoint_NoBankAccount() throws Exception {
		var requestBuilder = get("/account/history").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("You do not have a bank account!"));
	}

	@Test
	@DisplayName("Test for GET /account/history endpoint(no transactions)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)", createParent})
	void testAccountHistoryEndpoint_NoTransactions() throws Exception {
		var requestBuilder = get("/account/history").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error")
						.value("No transactions have been performed for this account"));
	}

	@Test
	@DisplayName("Test for POST /user/account endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)" , createAdmin})
	void testUserAccountEndpoint() throws Exception {
		var userRequest = new UpdateUserRequest("vova@gmail.com", 1L);

		var expect = UserDTO.builder()
				.user_id(1L)
				.name("vova")
				.email("vova@gmail.com")
				.role(Role.ADMIN.toString())
				.usage_limit(100)
				.bankAccount(BankAccountDTO.builder().id(1L).balance(200).build())
				.build();

		var requestBuilder = post("/user/account")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(userRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string(mapper.writeValueAsString(expect)));
	}

	@Test
	@DisplayName("Test for POST /user/account endpoint(no user)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)" , createAdmin})
	void testUserAccountEndpoint_NoUser() throws Exception {
		var userRequest = new UpdateUserRequest("vova2@gmail.com", 1L);

		var requestBuilder = post("/user/account")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(userRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("There are no users with that username"));
	}

	@Test
	@DisplayName("est for POST /user/account endpoint(wrong id)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)" , createAdmin})
	void testUserAccountEndpoint_WrongId() throws Exception {
		var userRequest = new UpdateUserRequest("vova@gmail.com", 2L);

		var requestBuilder = post("/user/account")
				.with(postProcessor)
				.contentType(MediaType.APPLICATION_JSON)
				.content(mapper.writeValueAsString(userRequest));
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("There are no bank account with that id"));
	}

	@Test
	@DisplayName("Test for GET /account/all endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (100, 1), (2200, 2)" , createAdmin})
	void testAccountAllEndpoint() throws Exception {
		var expect = List.of(BankAccountDTO.builder().id(1L).balance(100).build(),
				BankAccountDTO.builder().id(2L).balance(2200).build());

		var requestBuilder = get("/account/all").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string(mapper.writeValueAsString(expect)));
	}

	@Test
	@DisplayName("Test for DELETE /account/delete endpoint")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 1)" , createAdmin})
	void testDeleteAccountEndpoint() throws Exception {
		var requestBuilder = delete("/account/delete?id=1").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isOk())
				.andExpect(content().string("Account deleted"));
	}

	@Test
	@DisplayName("Test for DELETE /account/delete endpoint(wrong id)")
	@Sql(statements = {"INSERT INTO bankaccount(balance, id) VALUES (200, 2)" , createAdmin})
	void testDeleteAccountEndpoint_WrongId() throws Exception {
		var requestBuilder = delete("/account/delete?id=1").with(postProcessor);
		mockMvc.perform(requestBuilder)
				.andExpect(status().isBadRequest())
				.andExpect(content().string("Wrong id"));
	}
}
