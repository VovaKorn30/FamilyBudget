package com.budget.planning;

import com.budget.planning.configuration.security.UserAdapter;
import com.budget.planning.dto.request.UserRegistrationRequest;
import com.budget.planning.model.BankAccount;
import com.budget.planning.model.User;
import com.budget.planning.repository.BankAccountRepository;
import com.budget.planning.repository.UserRepository;
import com.budget.planning.service.UserDetailsServiceImp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceTest {
    @Mock
    UserRepository userRepository;
    @Mock
    BankAccountRepository bankAccountRepository;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    UserDetailsServiceImp userDetailsService;

    @Test
    @DisplayName("Test for registration")
    void testRegistration() {
        ResponseEntity<String> expect = new ResponseEntity<>("Successfully registered, your email is your username",
                HttpStatus.OK);
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "PARENT",
                1L
        );

        when(userRepository.findUserByEmail(userRegistrationRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(bankAccountRepository.findById(1L))
                .thenReturn(Optional.ofNullable(BankAccount.builder().id(1L).balance(1).build()));

        assertThat(userDetailsService.register(userRegistrationRequest)).isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for registration, no bank account")
    void testRegistration_NoBankAccount() {
        ResponseEntity<String> expect = new ResponseEntity<>("Successfully registered, your email is your username." +
                "But bank account not found, ask admin for help or create a new one", HttpStatus.OK);
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "PARENT",
                0L
        );

        when(userRepository.findUserByEmail(userRegistrationRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThat(userDetailsService.register(userRegistrationRequest)).isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for registration, already registered")
    void testRegistration_AlreadyRegistered() {
        ResponseEntity<String> expect = new ResponseEntity<>("Such a user already exists!",
                HttpStatus.BAD_REQUEST);
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "PARENT",
                0L
        );

        when(userRepository.findUserByEmail(userRegistrationRequest.getEmail()))
                .thenReturn(Optional.of(new User()));

        assertThat(userDetailsService.register(userRegistrationRequest)).isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for registration, wrong role")
    void testRegistration_WrongRole() {
        ResponseEntity<String> expect = new ResponseEntity<>("Wrong role provided",
                HttpStatus.BAD_REQUEST);
        UserRegistrationRequest userRegistrationRequest = new UserRegistrationRequest(
                "vova",
                "vova@gmail.com",
                "1234",
                "megauser",
                0L
        );

        when(userRepository.findUserByEmail(userRegistrationRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThat(userDetailsService.register(userRegistrationRequest)).isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for authentication")
    void testAuthentication() {
        String email = "email@gmail.com";
        User user = new User();
        UserAdapter expect = new UserAdapter(user);

        when(userRepository.findUserByEmail(email))
                .thenReturn(Optional.of(user));

        assertThat(userDetailsService.loadUserByUsername(email)).isEqualTo(expect);
    }

    @Test
    @DisplayName("Test for authentication, user not found")
    void testAuthentication_NoUser() {
        String email = "email@gmail.com";

        when(userRepository.findUserByEmail(email))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Not found!");
    }
}
