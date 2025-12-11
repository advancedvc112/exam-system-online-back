package com.exam.online.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.online.dal.dataobject.SystemUserDO;
import com.exam.online.dal.mapper.SystemUserMapper;
import com.exam.online.dto.LoginRequest;
import com.exam.online.dto.LoginResponse;
import com.exam.online.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SystemUserMapper systemUserMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LoginResponse login(LoginRequest request) {
        SystemUserDO user = getByUsername(request.getUsername());
        if (user == null) {
            user = createUser(request.getUsername(), request.getUserpassword(), null);
        } else if (!passwordEncoder.matches(request.getUserpassword(), user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LocalDateTime now = LocalDateTime.now();
        user.setLastLoginTime(now);
        systemUserMapper.updateById(user);

        return new LoginResponse(user.getId(), user.getUsername());
    }

    public LoginResponse register(RegisterRequest request) {
        SystemUserDO exists = getByUsername(request.getUsername());
        if (exists != null) {
            throw new IllegalArgumentException("用户名已存在");
        }
        SystemUserDO user = createUser(request.getUsername(), request.getUserpassword(), request.getUserRole());
        return new LoginResponse(user.getId(), user.getUsername());
    }

    private SystemUserDO getByUsername(String username) {
        return systemUserMapper.selectOne(new LambdaQueryWrapper<SystemUserDO>()
                .eq(SystemUserDO::getUsername, username)
                .eq(SystemUserDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    private SystemUserDO createUser(String username, String rawPassword, Integer userRole) {
        LocalDateTime now = LocalDateTime.now();

        SystemUserDO user = new SystemUserDO();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        user.setLastLoginTime(now);
        user.setLastLoginIp(null);
        user.setRegisterTime(now);
        user.setUpdatedTime(now);
        // 默认学生角色；允许调用方传入教师/管理员
        int role = (userRole == null || userRole < 1 || userRole > 3) ? 1 : userRole;
        user.setUserRole(role);
        user.setIsDeleted(0);

        systemUserMapper.insert(user);
        return user;
    }
}

