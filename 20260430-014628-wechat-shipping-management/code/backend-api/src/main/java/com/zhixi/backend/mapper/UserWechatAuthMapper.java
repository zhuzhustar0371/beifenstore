package com.zhixi.backend.mapper;

import com.zhixi.backend.model.UserWechatAuth;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserWechatAuthMapper {
  @Select("SELECT * FROM user_wechat_auth WHERE openid = #{openid}")
  UserWechatAuth findByOpenid(String openid);

  @Select("SELECT * FROM user_wechat_auth WHERE source_type = #{sourceType} AND openid = #{openid}")
  UserWechatAuth findBySourceAndOpenid(@Param("sourceType") String sourceType, @Param("openid") String openid);

  @Select("SELECT * FROM user_wechat_auth WHERE unionid = #{unionid} ORDER BY id DESC LIMIT 1")
  UserWechatAuth findByUnionid(String unionid);

  @Select("SELECT * FROM user_wechat_auth WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
  UserWechatAuth findByUserId(Long userId);

  @Select("SELECT * FROM user_wechat_auth WHERE user_id = #{userId} AND source_type = #{sourceType} ORDER BY id DESC LIMIT 1")
  UserWechatAuth findByUserIdAndSource(@Param("userId") Long userId, @Param("sourceType") String sourceType);

  @Insert("INSERT INTO user_wechat_auth(user_id, source_type, openid, unionid, nickname, avatar_url) VALUES(#{userId}, #{sourceType}, #{openid}, #{unionid}, #{nickname}, #{avatarUrl})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UserWechatAuth auth);

  @Update("UPDATE user_wechat_auth SET unionid = #{unionid}, nickname = #{nickname}, avatar_url = #{avatarUrl}, source_type = #{sourceType}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int updateProfile(UserWechatAuth auth);

  @Update("UPDATE user_wechat_auth SET user_id = #{userId}, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
  int updateUserId(@Param("id") Long id, @Param("userId") Long userId);

  @Update("UPDATE user_wechat_auth SET nickname = #{nickname} WHERE id = #{id}")
  int updateNicknameOnly(@Param("id") Long id, @Param("nickname") String nickname);

  @Insert("INSERT INTO user_wechat_auth(user_id, openid, nickname) VALUES(#{userId}, #{openid}, #{nickname})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insertLegacy(@Param("userId") Long userId, @Param("openid") String openid, @Param("nickname") String nickname);
}
