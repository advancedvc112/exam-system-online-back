package com.exam.online.dal.dataobject;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("system_users")
public class SystemUserDO {
    @TableId
    private Long id;
    private String username;
    private String password;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime registerTime;
    private LocalDateTime updatedTime;
    private Integer userRole;
    private Integer isDeleted;
}

