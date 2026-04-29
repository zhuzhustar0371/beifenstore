package com.zhixi.backend.dto;

import jakarta.validation.constraints.DecimalMin;
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

  @Size(max = 255, message = "商品图片地址过长")
  private String imageUrl;

  @NotNull(message = "上架状态不能为空")
  private Boolean active;

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
}
