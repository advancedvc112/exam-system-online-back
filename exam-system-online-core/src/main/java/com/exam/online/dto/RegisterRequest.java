package com.exam.online.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    /**
     * 用户角色（1：学生 2：教师 3：管理员），不传默认学生
     */
    @Min(value = 1, message = "用户角色不合法")
    @Max(value = 3, message = "用户角色不合法")
    private Integer userRole;
}

