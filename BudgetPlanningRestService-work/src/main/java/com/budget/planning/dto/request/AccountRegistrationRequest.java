package com.budget.planning.dto.request;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AccountRegistrationRequest {
    @NotNull(message = "Write down initial balance of the account!")
    @Min(value = 0, message = "Write down initial balance of the account!")
    private Integer balance;
}
