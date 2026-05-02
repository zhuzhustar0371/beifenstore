package com.zhixi.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class AdminProductUpsertRequest {
  @NotBlank(message = "Product name is required")
  @Size(max = 128, message = "Product name is too long")
  private String name;

  @NotNull(message = "Price is required")
  @DecimalMin(value = "0.01", message = "Price must be greater than 0")
  private BigDecimal price;

  @Size(max = 512, message = "Description is too long")
  private String description;

  @Size(max = 20000, message = "Detail content is too long")
  private String detailContent;

  @Size(max = 255, message = "Image URL is too long")
  private String imageUrl;

  @NotNull(message = "Active status is required")
  private Boolean active;

  private Boolean featured;

  @NotNull(message = "Personal second ratio is required")
  @DecimalMin(value = "0.00", message = "Personal second ratio cannot be negative")
  @DecimalMax(value = "1.00", message = "Personal second ratio cannot be greater than 1")
  private BigDecimal personalSecondRatio;

  @NotNull(message = "Personal third ratio is required")
  @DecimalMin(value = "0.00", message = "Personal third ratio cannot be negative")
  @DecimalMax(value = "1.00", message = "Personal third ratio cannot be greater than 1")
  private BigDecimal personalThirdRatio;

  @NotNull(message = "Personal fourth ratio is required")
  @DecimalMin(value = "0.00", message = "Personal fourth ratio cannot be negative")
  @DecimalMax(value = "1.00", message = "Personal fourth ratio cannot be greater than 1")
  private BigDecimal personalFourthRatio;

  @NotNull(message = "Invite batch size is required")
  @Min(value = 1, message = "Invite batch size must be at least 1")
  private Integer inviteBatchSize;

  @NotNull(message = "Invite first ratio is required")
  @DecimalMin(value = "0.00", message = "Invite first ratio cannot be negative")
  @DecimalMax(value = "1.00", message = "Invite first ratio cannot be greater than 1")
  private BigDecimal inviteFirstRatio;

  @NotNull(message = "Invite repeat ratio is required")
  @DecimalMin(value = "0.00", message = "Invite repeat ratio cannot be negative")
  @DecimalMax(value = "1.00", message = "Invite repeat ratio cannot be greater than 1")
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
