package com.family.finance.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.family.finance.auth.domain.AppUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {
    @Select("SELECT * FROM app_user WHERE username = #{username} LIMIT 1")
    AppUser selectByUsername(String username);

    @Select("SELECT * FROM app_user WHERE id = #{id} FOR UPDATE")
    AppUser selectForUpdateById(Long id);

    @Select("SELECT COALESCE(MAX(CAST(SUBSTRING(member_no, 2) AS UNSIGNED)), 0) FROM app_user WHERE household_id = #{householdId} FOR UPDATE")
    Integer maxMemberSequenceForUpdate(Long householdId);

    @Select("SELECT COUNT(*) FROM app_user WHERE household_id = #{householdId} AND role = 'PARENT' AND status = 'ACTIVE'")
    long countActiveParents(Long householdId);
}
