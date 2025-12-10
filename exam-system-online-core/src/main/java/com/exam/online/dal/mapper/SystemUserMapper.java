package com.exam.online.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.online.dal.dataobject.SystemUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SystemUserMapper extends BaseMapper<SystemUserDO> {
}

