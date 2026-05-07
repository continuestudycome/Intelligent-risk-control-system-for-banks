package com.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.system.domain.TxnTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface TxnTransactionMapper extends BaseMapper<TxnTransaction> {

    @Select("""
        SELECT COALESCE(SUM(amount), 0)
        FROM txn_transaction
        WHERE customer_id = #{customerId}
          AND transaction_time >= CURDATE()
          AND transaction_time < DATE_ADD(CURDATE(), INTERVAL 1 DAY)
          AND handle_result = 1
        """)
    BigDecimal selectTodayTotalAmount(@Param("customerId") Long customerId);

    /**
     * 近一段时间内小额交易笔数（用于试探易欺诈识别）
     */
    @Select("""
        SELECT COUNT(*)
        FROM txn_transaction
        WHERE customer_id = #{customerId}
          AND create_time >= DATE_SUB(NOW(), INTERVAL #{hours} HOUR)
          AND amount < #{smallAmount}
          AND handle_result = 1
        """)
    Long countSmallAmountTransactions(
            @Param("customerId") Long customerId,
            @Param("smallAmount") BigDecimal smallAmount,
            @Param("hours") int hours
    );

    @Select("""
        SELECT COUNT(*)
        FROM txn_transaction
        WHERE customer_id = #{customerId}
          AND handle_result = 1
          AND transaction_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
        """)
    Long countSuccessfulSinceDays(@Param("customerId") Long customerId, @Param("days") int days);

    @Select("""
        SELECT COALESCE(SUM(amount), 0)
        FROM txn_transaction
        WHERE customer_id = #{customerId}
          AND handle_result = 1
          AND transaction_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY)
        """)
    BigDecimal sumSuccessfulAmountSinceDays(@Param("customerId") Long customerId, @Param("days") int days);
}
