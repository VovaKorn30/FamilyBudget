package com.budget.planning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

@Data
@AllArgsConstructor
@Builder
@Getter
public class AccountUpdateDTO {
    private Long account_id;
    private Integer balance;
}
