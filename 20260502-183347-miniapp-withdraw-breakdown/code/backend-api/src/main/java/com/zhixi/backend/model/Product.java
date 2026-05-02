package com.zhixi.backend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Product {
  private Long id;
  private String name;
  private BigDecimal price;
  private String description;
  private String detailContent;
  private String imageUrl;
  private Long salesCount;
  private Boolean active;
  private Boolean featured;
  private BigDecimal personalSecondRatio;
  private BigDecimal personalThirdRatio;
  private BigDecimal personalFourthRatio;
  private Integer inviteBatchSize;
  private BigDecimal inviteFirstRatio;
  private BigDecimal inviteRepeatRatio;
  private LocalDateTime createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

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

  public Long getSalesCount() {
    return salesCount;
  }

  public void setSalesCount(Long salesCount) {
    this.salesCount = salesCount;
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

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }
}
