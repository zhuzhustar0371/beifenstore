package com.zhixi.backend.mapper;

import com.zhixi.backend.model.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {
  @Select("SELECT id,name,price,description,main_image_url AS imageUrl,COALESCE(sales_count, 0) AS salesCount,(status = 1) AS active,created_at AS createdAt FROM products WHERE status = 1 ORDER BY COALESCE(sales_count, 0) DESC, id DESC")
  List<Product> findActive();

  @Select("SELECT id,name,price,description,main_image_url AS imageUrl,COALESCE(sales_count, 0) AS salesCount,(status = 1) AS active,created_at AS createdAt FROM products WHERE id = #{id}")
  Product findById(Long id);

  @Select("SELECT id,name,price,description,main_image_url AS imageUrl,COALESCE(sales_count, 0) AS salesCount,(status = 1) AS active,created_at AS createdAt FROM products ORDER BY id DESC")
  List<Product> findAll();

  @Select({
      "<script>",
      "SELECT COUNT(1) FROM products",
      "WHERE 1=1",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND name LIKE CONCAT('%', #{keyword}, '%')",
      "</if>",
      "<if test='active != null'>",
      "  AND status = CASE WHEN #{active} THEN 1 ELSE 0 END",
      "</if>",
      "</script>"
  })
  long countByAdminQuery(@Param("keyword") String keyword, @Param("active") Boolean active);

  @Select({
      "<script>",
      "SELECT id,name,price,description,main_image_url AS imageUrl,COALESCE(sales_count, 0) AS salesCount,(status = 1) AS active,created_at AS createdAt FROM products",
      "WHERE 1=1",
      "<if test='keyword != null and keyword != \"\"'>",
      "  AND name LIKE CONCAT('%', #{keyword}, '%')",
      "</if>",
      "<if test='active != null'>",
      "  AND status = CASE WHEN #{active} THEN 1 ELSE 0 END",
      "</if>",
      "ORDER BY id DESC",
      "LIMIT #{limit} OFFSET #{offset}",
      "</script>"
  })
  List<Product> findByAdminQuery(
      @Param("keyword") String keyword,
      @Param("active") Boolean active,
      @Param("offset") int offset,
      @Param("limit") int limit
  );

  @Update("UPDATE products SET sales_count = COALESCE(sales_count, 0) + #{quantity}, updated_at = NOW() WHERE id = #{productId}")
  int incrementSalesCount(@Param("productId") Long productId, @Param("quantity") int quantity);

  @Insert("INSERT INTO products(name, sub_title, description, main_image_url, price, status, sort_no, created_at, updated_at) " +
      "VALUES(#{name}, #{name}, #{description}, #{imageUrl}, #{price}, CASE WHEN #{active} THEN 1 ELSE 0 END, 0, NOW(), NOW())")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(Product product);

  @Update("UPDATE products SET name = #{name}, sub_title = #{name}, price = #{price}, description = #{description}, main_image_url = #{imageUrl}, " +
      "status = CASE WHEN #{active} THEN 1 ELSE 0 END, updated_at = NOW() WHERE id = #{id}")
  int update(Product product);

  @Update("UPDATE products SET status = CASE WHEN #{active} THEN 1 ELSE 0 END, updated_at = NOW() WHERE id = #{id}")
  int updateActive(@Param("id") Long id, @Param("active") Boolean active);

  @Delete("DELETE FROM products WHERE id = #{id}")
  int deleteById(Long id);
}
