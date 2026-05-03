package com.zhixi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdminProductUpsertRequest {
  @NotBlank(message = "商品名称不能为空")
  @Size(max = 128, message = "商品名称过长")
  private String name;

  @NotNull(message = "价格不能为空")
  @DecimalMin(value = "0.01", message = "价格必须大于0")
  private BigDecimal price;

  @Size(max = 512, message = "商品描述过长")
  private String description;

  @Size(max = 20000, message = "详情内容过长")
  private String detailContent;

  @Size(max = 255, message = "图片URL过长")
  private String imageUrl;

  @NotNull(message = "上架状态不能为空")
  private Boolean active;

  private Boolean featured;

  @NotNull(message = "个人第二次返现比例不能为空")
  @DecimalMin(value = "0.00", message = "个人第二次返现比例不能为负数")
  @DecimalMax(value = "1.00", message = "个人第二次返现比例不能大于1")
  private BigDecimal personalSecondRatio;

  @NotNull(message = "个人第三次返现比例不能为空")
  @DecimalMin(value = "0.00", message = "个人第三次返现比例不能为负数")
  @DecimalMax(value = "1.00", message = "个人第三次返现比例不能大于1")
  private BigDecimal personalThirdRatio;

  @NotNull(message = "个人第四次返现比例不能为空")
  @DecimalMin(value = "0.00", message = "个人第四次返现比例不能为负数")
  @DecimalMax(value = "1.00", message = "个人第四次返现比例不能大于1")
  private BigDecimal personalFourthRatio;

  @NotNull(message = "邀请批次大小不能为空")
  @Min(value = 1, message = "邀请批次大小至少为1")
  private Integer inviteBatchSize;

  @NotNull(message = "首次邀请返现比例不能为空")
  @DecimalMin(value = "0.00", message = "首次邀请返现比例不能为负数")
  @DecimalMax(value = "1.00", message = "首次邀请返现比例不能大于1")
  private BigDecimal inviteFirstRatio;

  @NotNull(message = "重复邀请返现比例不能为空")
  @DecimalMin(value = "0.00", message = "重复邀请返现比例不能为负数")
  @DecimalMax(value = "1.00", message = "重复邀请返现比例不能大于1")
  private BigDecimal inviteRepeatRatio;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDetailContent() {
    return detailContent;
  }

  public void setDetailContent(String detailContent) {
    this.detailContent = detailContent;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }

  public Boolean getFeatured() {
    return featured;
  }

  public void setFeatured(Boolean featured) {
    this.featured = featured;
  }

  public BigDecimal getPersonalSecondRatio() {
    return personalSecondRatio;
  }

  public void setPersonalSecondRatio(BigDecimal personalSecondRatio) {
    this.personalSecondRatio = personalSecondRatio;
  }

  public BigDecimal getPersonalThirdRatio() {
    return personalThirdRatio;
  }

  public void setPersonalThirdRatio(BigDecimal personalThirdRatio) {
    this.personalThirdRatio = personalThirdRatio;
  }

  public BigDecimal getPersonalFourthRatio() {
    return personalFourthRatio;
  }

  public void setPersonalFourthRatio(BigDecimal personalFourthRatio) {
    this.personalFourthRatio = personalFourthRatio;
  }

  public Integer getInviteBatchSize() {
    return inviteBatchSize;
  }

  public void setInviteBatchSize(Integer inviteBatchSize) {
    this.inviteBatchSize = inviteBatchSize;
  }

  public BigDecimal getInviteFirstRatio() {
    return inviteFirstRatio;
  }

  public void setInviteFirstRatio(BigDecimal inviteFirstRatio) {
    this.inviteFirstRatio = inviteFirstRatio;
  }

  public BigDecimal getInviteRepeatRatio() {
    return inviteRepeatRatio;
  }

  public void setInviteRepeatRatio(BigDecimal inviteRepeatRatio) {
    this.inviteRepeatRatio = inviteRepeatRatio;
  }
}
