package com.zhixi.backend.mapper;

import com.zhixi.backend.model.CashbackDebt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface CashbackDebtMapper {

  @Insert("INSERT INTO cashback_debts(user_id,order_id,cashback_id,amount,reason,status,created_at) " +
      "VALUES(#{userId},#{orderId},#{cashbackId},#{amount},#{reason},#{status},NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(CashbackDebt debt);

  @Select("SELECT id,user_id AS userId,order_id AS orderId,cashback_id AS cashbackId,amount,reason,status,created_at AS createdAt " +
      "FROM cashback_debts WHERE user_id = #{userId} AND status = 'PENDING' ORDER BY id ASC")
  List<CashbackDebt> findPendingByUserId(Long userId);

  @Select("SELECT COALESCE(SUM(amount),0) FROM cashback_debts WHERE user_id = #{userId} AND status = 'PENDING'")
  BigDecimal sumPendingByUserId(Long userId);

  @Update("UPDATE cashback_debts SET status = #{status} WHERE id = #{id}")
  int updateStatus(@Param("id") Long id, @Param("status") String status);

  @Update("UPDATE cashback_debts SET status = 'DEDUCTED' WHERE user_id = #{userId} AND status = 'PENDING'")
  int markAllDeducted(@Param("userId") Long userId);
}
