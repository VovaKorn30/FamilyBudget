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
public class BankHistoryDTO {
    private String operation;
    private String reason;
    private LocalDateTime timestamp;
    private Integer amount;
    private UserWithLimitDTO user;
}
