package com.fivefy.domain.user.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(min = 2, max = 10, message = "이름은 2~10자여야 합니다")
        String name,
        @Valid
        PasswordChangeRequest passwordChange
) {
    public record PasswordChangeRequest(
            @NotBlank(message = "현재 비밀번호는 필수입니다")
            String currentPassword,
            @NotBlank(message = "새 비밀번호는 필수입니다")
            @Pattern(
                    regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()])[a-zA-Z\\d!@#$%^&*()]{8,20}$",
                    message = "비밀번호는 8~20자며, 영문, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
            )
            String newPassword
    ) {}
}

