package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends BaseMapper<SysRole> {

    @Select("""
        SELECT *
        FROM sys_role
        WHERE role_code = #{roleCode}
          AND is_deleted = 0
        LIMIT 1
        """)
    SysRole selectByRoleCode(@Param("roleCode") String roleCode);
}
