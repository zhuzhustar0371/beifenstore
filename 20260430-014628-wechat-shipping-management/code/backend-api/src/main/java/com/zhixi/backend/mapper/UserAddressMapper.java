package com.zhixi.backend.mapper;

import com.zhixi.backend.model.UserAddress;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserAddressMapper {
  @Insert("INSERT INTO user_addresses(" +
      "user_id,recipient_name,recipient_phone,province,city,district,detail_address,is_default,created_at,updated_at" +
      ") VALUES (" +
      "#{userId},#{recipientName},#{recipientPhone},#{province},#{city},#{district},#{detailAddress},#{isDefault},NOW(),NOW()" +
      ")")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UserAddress address);

  @Select("SELECT id,user_id AS userId,recipient_name AS recipientName,recipient_phone AS recipientPhone," +
      "province,city,district,detail_address AS detailAddress,is_default AS isDefault," +
      "created_at AS createdAt,updated_at AS updatedAt " +
      "FROM user_addresses WHERE id = #{id} LIMIT 1")
  UserAddress findById(Long id);

  @Select("SELECT id,user_id AS userId,recipient_name AS recipientName,recipient_phone AS recipientPhone," +
      "province,city,district,detail_address AS detailAddress,is_default AS isDefault," +
      "created_at AS createdAt,updated_at AS updatedAt " +
      "FROM user_addresses WHERE user_id = #{userId} ORDER BY is_default DESC, id DESC")
  List<UserAddress> findByUserId(Long userId);

  @Select("SELECT id,user_id AS userId,recipient_name AS recipientName,recipient_phone AS recipientPhone," +
      "province,city,district,detail_address AS detailAddress,is_default AS isDefault," +
      "created_at AS createdAt,updated_at AS updatedAt " +
      "FROM user_addresses WHERE user_id = #{userId} ORDER BY is_default DESC, id DESC LIMIT 1")
  UserAddress findPreferredByUserId(Long userId);

  @Select("SELECT COUNT(1) FROM user_addresses WHERE user_id = #{userId}")
  long countByUserId(Long userId);

  @Update("UPDATE user_addresses SET is_default = FALSE, updated_at = NOW() " +
      "WHERE user_id = #{userId} AND is_default = TRUE")
  int clearDefaultByUserId(Long userId);

  @Update("UPDATE user_addresses SET is_default = FALSE, updated_at = NOW() " +
      "WHERE user_id = #{userId} AND id <> #{id} AND is_default = TRUE")
  int clearDefaultByUserIdExclude(@Param("userId") Long userId, @Param("id") Long id);

  @Update("UPDATE user_addresses SET " +
      "recipient_name = #{recipientName},recipient_phone = #{recipientPhone},province = #{province},city = #{city}," +
      "district = #{district},detail_address = #{detailAddress},is_default = #{isDefault},updated_at = NOW() " +
      "WHERE id = #{id} AND user_id = #{userId}")
  int updateByIdAndUserId(UserAddress address);

  @Update("UPDATE user_addresses SET is_default = TRUE, updated_at = NOW() WHERE id = #{id} AND user_id = #{userId}")
  int markDefaultByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

  @Delete("DELETE FROM user_addresses WHERE id = #{id} AND user_id = #{userId}")
  int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
