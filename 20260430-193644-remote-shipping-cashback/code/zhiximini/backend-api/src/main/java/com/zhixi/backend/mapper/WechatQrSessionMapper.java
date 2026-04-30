package com.zhixi.backend.mapper;

import com.zhixi.backend.model.WechatQrSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface WechatQrSessionMapper {
  @Insert("INSERT INTO wechat_qr_sessions(scene, status, expires_at) VALUES(#{scene}, #{status}, #{expiresAt})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(WechatQrSession session);

  @Select("SELECT * FROM wechat_qr_sessions WHERE scene = #{scene}")
  WechatQrSession findByScene(String scene);

  @Update("UPDATE wechat_qr_sessions SET status = #{status}, openid = #{openid}, nickname = #{nickname}, confirmed_at = CURRENT_TIMESTAMP WHERE scene = #{scene}")
  int markScanned(
      @Param("scene") String scene,
      @Param("status") String status,
      @Param("openid") String openid,
      @Param("nickname") String nickname
  );

  @Update("UPDATE wechat_qr_sessions SET status = 'AUTHORIZED', user_id = #{userId}, token = #{token}, confirmed_at = CURRENT_TIMESTAMP WHERE scene = #{scene}")
  int markAuthorized(@Param("scene") String scene, @Param("userId") Long userId, @Param("token") String token);
}
