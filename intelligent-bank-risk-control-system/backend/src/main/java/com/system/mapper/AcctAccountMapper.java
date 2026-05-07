package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.AcctAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AcctAccountMapper extends BaseMapper<AcctAccount> {

    @Select("""
        SELECT *
        FROM acct_account
        WHERE customer_id = #{customerId}
          AND is_deleted = 0
        ORDER BY id DESC
        """)
    List<AcctAccount> selectByCustomerId(@Param("customerId") Long customerId);

    @Select("""
        SELECT *
        FROM acct_account
        WHERE account_no = #{accountNo}
          AND is_deleted = 0
        LIMIT 1
        """)
    AcctAccount selectByAccountNo(@Param("accountNo") String accountNo);

    @Update("""
        UPDATE acct_account
        SET balance = balance - #{amount},
            update_time = NOW()
        WHERE id = #{accountId}
          AND is_deleted = 0
          AND status = 1
          AND balance >= #{amount}
        """)
    int debitWithCheck(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);

    @Update("""
        UPDATE acct_account
        SET balance = balance + #{amount},
            update_time = NOW()
        WHERE id = #{accountId}
          AND is_deleted = 0
          AND status = 1
        """)
    int credit(@Param("accountId") Long accountId, @Param("amount") BigDecimal amount);
}
