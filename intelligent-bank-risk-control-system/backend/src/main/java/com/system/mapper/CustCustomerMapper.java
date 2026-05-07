package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.CustCustomer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustCustomerMapper extends BaseMapper<CustCustomer> {

    @Select("""
        SELECT *
        FROM cust_customer
        WHERE user_id = #{userId}
          AND is_deleted = 0
        LIMIT 1
        """)
    CustCustomer selectByUserId(@Param("userId") Long userId);

    /**
     * 风控信用查询：最近客户。仅查基础列（兼容未执行增量 DDL 的库：不包含 credit_level）。
     */
    @Select("""
            SELECT id, real_name, phone, customer_no
            FROM cust_customer
            WHERE is_deleted = 0
            ORDER BY id DESC
            LIMIT #{pageSize}
            """)
    List<CustCustomer> listCreditBriefRecent(@Param("pageSize") int pageSize);

    /** 关键词检索（姓名/手机号），不含 ID 精确匹配 */
    @Select("""
            SELECT id, real_name, phone, customer_no
            FROM cust_customer
            WHERE is_deleted = 0
              AND (
                  real_name LIKE CONCAT('%', #{kw}, '%')
                  OR IFNULL(phone, '') LIKE CONCAT('%', #{kw}, '%')
              )
            ORDER BY id DESC
            LIMIT #{pageSize}
            """)
    List<CustCustomer> listCreditBriefByKeywordNoId(@Param("kw") String kw, @Param("pageSize") int pageSize);

    /** 关键词检索 + 纯数字关键词时按客户主键 ID 匹配 */
    @Select("""
            SELECT id, real_name, phone, customer_no
            FROM cust_customer
            WHERE is_deleted = 0
              AND (
                  real_name LIKE CONCAT('%', #{kw}, '%')
                  OR IFNULL(phone, '') LIKE CONCAT('%', #{kw}, '%')
                  OR id = #{idExact}
              )
            ORDER BY id DESC
            LIMIT #{pageSize}
            """)
    List<CustCustomer> listCreditBriefByKeywordWithId(
            @Param("kw") String kw, @Param("idExact") Long idExact, @Param("pageSize") int pageSize);
}
