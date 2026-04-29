package com.zhixi.backend.service;

import com.zhixi.backend.common.BusinessException;
import com.zhixi.backend.mapper.ProductMapper;
import com.zhixi.backend.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {
  private final ProductMapper productMapper;

  public ProductService(ProductMapper productMapper) {
    this.productMapper = productMapper;
  }

  public List<Product> listActive() {
    return productMapper.findActive();
  }

  public Product getById(Long id) {
    Product product = productMapper.findById(id);
    if (product == null || !Boolean.TRUE.equals(product.getActive())) {
      throw new BusinessException("product not found or inactive");
    }
    return product;
  }

  public void incrementSalesCount(Long productId, int quantity) {
    if (quantity <= 0) {
      return;
    }
    productMapper.incrementSalesCount(productId, quantity);
  }
}
