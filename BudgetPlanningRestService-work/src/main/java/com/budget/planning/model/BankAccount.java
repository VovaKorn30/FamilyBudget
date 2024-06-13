package com.budget.planning.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bankaccount")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer balance;
}
