package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<SysUser> {

    @Select("""
        SELECT *
        FROM sys_user
        WHERE username = #{username}
          AND is_deleted = 0
        LIMIT 1
        """)
    SysUser selectByUsername(@Param("username") String username);

    @Select("""
        SELECT r.role_code
        FROM sys_role r
        INNER JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
          AND r.status = 1
          AND r.is_deleted = 0
        ORDER BY r.id
        LIMIT 1
        """)
    String selectPrimaryRoleCode(@Param("userId") Long userId);

    /** 是否拥有指定角色（解决多角色时 LIMIT 1 不稳定的问题） */
    @Select(
            """
            SELECT COUNT(*)
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.is_deleted = 0
            WHERE ur.user_id = #{userId} AND UPPER(r.role_code) = UPPER(#{roleCode})
            """)
    long countUserRole(@Param("userId") Long userId, @Param("roleCode") String roleCode);

    @Select(
            """
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId} AND r.status = 1 AND r.is_deleted = 0
            ORDER BY r.id
            """)
    List<String> selectRoleCodes(@Param("userId") Long userId);
}
