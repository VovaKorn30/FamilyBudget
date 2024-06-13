package com.budget.planning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Builder
@Getter
public class BankAccountDTO {
    private Long id;
    private Integer balance;
}
