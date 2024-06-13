package com.budget.planning.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AccountUpdateRequest {
    @NotNull(message = "Write down with how much money you want to update your account!")
    @Min(value = 1, message = "Write down with how much money you want to update your account!")
    private Integer amount;
    @Schema(example = "Buy candy")
    @NotBlank(message = "Write down purpose of the operation!")
    private String reason;
}
