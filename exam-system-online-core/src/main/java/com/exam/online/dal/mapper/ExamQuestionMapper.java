package com.exam.online.dal.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.online.dal.dataobject.ExamQuestionDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestionDO> {
}

