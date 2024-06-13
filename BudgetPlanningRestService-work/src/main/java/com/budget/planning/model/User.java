package com.budget.planning.model;

import com.budget.planning.configuration.security.Role;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long user_id;
    private String name;
    private String email;
    private String password;
    private Role role;
    @ManyToOne
    @JoinColumn(name = "account_id")
    private BankAccount bankAccount;
    private Integer usage_limit;
}
