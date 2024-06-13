package com.budget.planning.service;

import com.budget.planning.configuration.security.Role;
import com.budget.planning.configuration.security.UserAdapter;
import com.budget.planning.dto.request.UserRegistrationRequest;
import com.budget.planning.model.User;
import com.budget.planning.repository.BankAccountRepository;
import com.budget.planning.repository.UserRepository;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@AllArgsConstructor
public class UserDetailsServiceImp implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public ResponseEntity<String> register(UserRegistrationRequest userRegistrationRequest) {
        if (userRepository.findUserByEmail(userRegistrationRequest.getEmail()).isPresent()) {
            return new ResponseEntity<>("Such a user already exists!",
                    HttpStatus.BAD_REQUEST);
        }

        if (Arrays.stream(Role.values()).noneMatch(x -> x.name().equals(userRegistrationRequest.getRole().toUpperCase()))){
            return new ResponseEntity<>("Wrong role provided",
                    HttpStatus.BAD_REQUEST);
        }
        Role role = Role.valueOf((userRegistrationRequest.getRole().toUpperCase()));

        User user = User.builder()
                .name(userRegistrationRequest.getName())
                .email(userRegistrationRequest.getEmail())
                .password(passwordEncoder.encode(userRegistrationRequest.getPassword()))
                .role(role)
                .usage_limit(role.equals(Role.CHILD) ? 100 : Integer.MAX_VALUE)
                .bankAccount(bankAccountRepository.findById(userRegistrationRequest.getAccount_id())
                        .orElse(null))
                .build();
        userRepository.save(user);

        if (user.getBankAccount() == null) {
            return new ResponseEntity<>("Successfully registered, your email is your username." +
                    "But bank account not found, ask admin for help or create a new one", HttpStatus.OK);
        }
        return new ResponseEntity<>("Successfully registered, your email is your username", HttpStatus.OK);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findUserByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found!"));

        return new UserAdapter(user);
    }
}
