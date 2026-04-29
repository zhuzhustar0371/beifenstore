package com.zhixi.backend.mapper;

import com.zhixi.backend.model.SmsLoginCode;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SmsLoginCodeMapper {
  @Insert("INSERT INTO sms_login_codes(phone, code, scene, used, expires_at) VALUES(#{phone}, #{code}, #{scene}, #{used}, #{expiresAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(SmsLoginCode code);

  @Select("SELECT * FROM sms_login_codes WHERE phone = #{phone} AND code = #{code} AND scene = #{scene} ORDER BY id DESC LIMIT 1")
  SmsLoginCode findLatest(@Param("phone") String phone, @Param("code") String code, @Param("scene") String scene);

  @Update("UPDATE sms_login_codes SET used = TRUE WHERE id = #{id}")
  int markUsed(Long id);
}
