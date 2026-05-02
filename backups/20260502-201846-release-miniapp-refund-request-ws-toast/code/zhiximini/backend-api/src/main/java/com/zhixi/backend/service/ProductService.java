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

  public Product getPreferredActive() {
    Product featured = productMapper.findFeaturedActive();
    if (featured != null) {
      return featured;
    }
    List<Product> activeProducts = listActive();
    if (activeProducts.isEmpty()) {
      throw new BusinessException("鏆傛棤鍙敤鍟嗗搧");
    }
    return activeProducts.get(0);
  }

  public Product getById(Long id) {
    Product product = productMapper.findById(id);
    if (product == null || !Boolean.TRUE.equals(product.getActive())) {
      throw new BusinessException("product not found or inactive");
    }
    return product;
  }

  public Product getAnyById(Long id) {
    Product product = productMapper.findById(id);
    if (product == null) {
      throw new BusinessException("product not found");
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
