package com.fivefy.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserSignupRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 2, max = 10, message = "이름은 2~10자여야 합니다")
        String name,
        @NotBlank(message = "이메일은 필수입니다")
        @Email(message = "이메일 형식이어야 합니다")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()])[a-zA-Z\\d!@#$%^&*()]{8,20}$",
                message = "비밀번호는 8~20자며, 영문, 숫자, 특수문자를 각각 최소 1개씩 포함해야 합니다"
        )
        String password
) {}
