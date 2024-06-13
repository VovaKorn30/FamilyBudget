package com.budget.planning.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserWithLimitDTO {
    private String name;
    private String email;
    private Integer usage_limit;
}
