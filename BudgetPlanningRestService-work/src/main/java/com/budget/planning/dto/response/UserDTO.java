package com.budget.planning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private Long user_id;
    private String name;
    private String email;
    private String role;
    private Integer usage_limit;
    private BankAccountDTO bankAccount;
}
