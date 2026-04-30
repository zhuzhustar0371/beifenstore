package com.zhixi.backend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShippingRecordMapper {
  @Insert("INSERT INTO shipping_records(order_id, company_name, tracking_no, ship_time, created_at, updated_at) " +
      "VALUES(#{orderId}, '待补充', #{trackingNo}, NOW(), NOW(), NOW())")
  int insert(@Param("orderId") Long orderId, @Param("trackingNo") String trackingNo);

  @Update("UPDATE shipping_records SET tracking_no = #{trackingNo}, ship_time = NOW(), updated_at = NOW() WHERE order_id = #{orderId}")
  int updateByOrderId(@Param("orderId") Long orderId, @Param("trackingNo") String trackingNo);
}
