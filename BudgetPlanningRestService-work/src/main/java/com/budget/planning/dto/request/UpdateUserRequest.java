package com.budget.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUserRequest {
    @Schema(example = "vova@gmail.com")
    @NotBlank(message = "Write down username!")
    private String username;
    @NotNull(message = "Write down bank account id!")
    @Min(value = 1, message = "Write down correct bank account id!")
    private Long account_id;
}
