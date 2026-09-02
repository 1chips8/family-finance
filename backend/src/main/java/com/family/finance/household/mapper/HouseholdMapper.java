package com.family.finance.household.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.finance.household.domain.Household;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HouseholdMapper extends BaseMapper<Household> {
    @Select("SELECT * FROM household WHERE invite_code = #{inviteCode} LIMIT 1")
    Household selectByInviteCode(String inviteCode);

    @Select("SELECT * FROM household WHERE id = #{id} FOR UPDATE")
    Household selectForUpdateById(Long id);
}
