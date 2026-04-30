package com.zhixi.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class SaveUserAddressRequest {
  @NotBlank(message = "收货人不能为空")
  private String recipientName;

  @NotBlank(message = "手机号不能为空")
  @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
  private String recipientPhone;

  @NotBlank(message = "省份不能为空")
  private String province;

  @NotBlank(message = "城市不能为空")
  private String city;

  @NotBlank(message = "区县不能为空")
  private String district;

  @NotBlank(message = "详细地址不能为空")
  private String detailAddress;

  private Boolean isDefault;

  public String getRecipientName() {
    return recipientName;
  }

  public void setRecipientName(String recipientName) {
    this.recipientName = recipientName;
  }

  public String getRecipientPhone() {
    return recipientPhone;
  }

  public void setRecipientPhone(String recipientPhone) {
    this.recipientPhone = recipientPhone;
  }

  public String getProvince() {
    return province;
  }

  public void setProvince(String province) {
    this.province = province;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getDistrict() {
    return district;
  }

  public void setDistrict(String district) {
    this.district = district;
  }

  public String getDetailAddress() {
    return detailAddress;
  }

  public void setDetailAddress(String detailAddress) {
    this.detailAddress = detailAddress;
  }

  public Boolean getIsDefault() {
    return isDefault;
  }

  public void setIsDefault(Boolean aDefault) {
    isDefault = aDefault;
  }
}
