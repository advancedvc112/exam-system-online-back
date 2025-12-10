package com.exam.online.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "username不能为空")
    @Size(max = 50, message = "username长度不能超过50")
    private String username;

    @NotBlank(message = "userpassword不能为空")
    @Size(min = 6, max = 100, message = "密码长度需在6-100之间")
    private String userpassword;
}

