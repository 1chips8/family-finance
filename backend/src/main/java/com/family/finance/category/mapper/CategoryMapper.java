package com.family.finance.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.finance.category.domain.FinanceCategory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<FinanceCategory> {}
