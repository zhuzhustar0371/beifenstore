package com.zhixi.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {
  private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationRunner.class);

  private final JdbcTemplate jdbcTemplate;

  public DatabaseMigrationRunner(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public void run(String... args) {
    ensureTable(
        "user_addresses",
        "CREATE TABLE IF NOT EXISTS user_addresses ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id BIGINT NOT NULL,"
            + "recipient_name VARCHAR(64) NOT NULL,"
            + "recipient_phone VARCHAR(32) NOT NULL,"
            + "province VARCHAR(64) NOT NULL,"
            + "city VARCHAR(64) NOT NULL,"
            + "district VARCHAR(64) NOT NULL,"
            + "detail_address VARCHAR(255) NOT NULL,"
            + "is_default BOOLEAN NOT NULL DEFAULT FALSE,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "KEY idx_user_addresses_user_id (user_id)"
            + ")"
    );
    ensureTable(
        "shipping_records",
        "CREATE TABLE IF NOT EXISTS shipping_records ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "order_id BIGINT NOT NULL,"
            + "company_name VARCHAR(128) NOT NULL,"
            + "tracking_no VARCHAR(128) NOT NULL,"
            + "ship_time TIMESTAMP NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "UNIQUE KEY uk_shipping_records_order_id (order_id)"
            + ")"
    );
    ensureTable(
        "withdrawal_requests",
        "CREATE TABLE IF NOT EXISTS withdrawal_requests ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id BIGINT NOT NULL,"
            + "amount DECIMAL(10,2) NOT NULL,"
            + "requested_amount DECIMAL(10,2) NULL,"
            + "status VARCHAR(32) NOT NULL,"
            + "source VARCHAR(32) NOT NULL DEFAULT 'USER',"
            + "apply_mode VARCHAR(32) NOT NULL DEFAULT 'MATURED_ONLY',"
            + "remark VARCHAR(255) NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "approved_at TIMESTAMP NULL,"
            + "completed_at TIMESTAMP NULL"
            + ")"
    );
    ensureTable(
        "withdrawal_request_items",
        "CREATE TABLE IF NOT EXISTS withdrawal_request_items ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "request_id BIGINT NOT NULL,"
            + "cashback_record_id BIGINT NOT NULL UNIQUE,"
            + "amount DECIMAL(10,2) NOT NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "KEY idx_withdrawal_items_request_id (request_id)"
            + ")"
    );
    ensureTable(
        "invite_product_relations",
        "CREATE TABLE IF NOT EXISTS invite_product_relations ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "inviter_user_id BIGINT NOT NULL,"
            + "invitee_user_id BIGINT NOT NULL,"
            + "product_id BIGINT NOT NULL,"
            + "bind_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "first_paid_time TIMESTAMP NULL,"
            + "UNIQUE KEY uk_invite_product_invitee (invitee_user_id, product_id),"
            + "KEY idx_invite_product_batch (inviter_user_id, product_id, first_paid_time)"
            + ")"
    );
    ensureRenamedColumn(
        "user_addresses.receiver_name -> recipient_name",
        "user_addresses",
        "receiver_name",
        "recipient_name",
        "VARCHAR(64) NOT NULL"
    );
    ensureRenamedColumn(
        "user_addresses.receiver_mobile -> recipient_phone",
        "user_addresses",
        "receiver_mobile",
        "recipient_phone",
        "VARCHAR(32) NOT NULL"
    );
    ensureColumn(
        "user_addresses.recipient_name",
        "SELECT recipient_name FROM user_addresses LIMIT 1",
        "ALTER TABLE user_addresses ADD COLUMN recipient_name VARCHAR(64) NULL"
    );
    ensureColumn(
        "user_addresses.recipient_phone",
        "SELECT recipient_phone FROM user_addresses LIMIT 1",
        "ALTER TABLE user_addresses ADD COLUMN recipient_phone VARCHAR(32) NULL"
    );

    ensureColumn(
        "users.miniapp_openid",
        "SELECT miniapp_openid FROM users LIMIT 1",
        "ALTER TABLE users ADD COLUMN miniapp_openid VARCHAR(128) NULL UNIQUE AFTER status"
    );

    ensureColumn(
        "products.sales_count",
        "SELECT sales_count FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN sales_count BIGINT NOT NULL DEFAULT 0"
    );
    ensureColumn(
        "products.is_featured",
        "SELECT is_featured FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN is_featured TINYINT NOT NULL DEFAULT 0 AFTER status"
    );
    ensureColumn(
        "products.detail_content",
        "SELECT detail_content FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN detail_content TEXT NULL AFTER description"
    );
    ensureColumn(
        "products.personal_second_ratio",
        "SELECT personal_second_ratio FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN personal_second_ratio DECIMAL(10,4) NULL DEFAULT 0.1000 AFTER price"
    );
    ensureColumn(
        "products.personal_third_ratio",
        "SELECT personal_third_ratio FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN personal_third_ratio DECIMAL(10,4) NULL DEFAULT 0.2000 AFTER personal_second_ratio"
    );
    ensureColumn(
        "products.personal_fourth_ratio",
        "SELECT personal_fourth_ratio FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN personal_fourth_ratio DECIMAL(10,4) NULL DEFAULT 1.0000 AFTER personal_third_ratio"
    );
    ensureColumn(
        "products.invite_batch_size",
        "SELECT invite_batch_size FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN invite_batch_size INT NULL DEFAULT 3 AFTER personal_fourth_ratio"
    );
    ensureColumn(
        "products.invite_first_ratio",
        "SELECT invite_first_ratio FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN invite_first_ratio DECIMAL(10,4) NULL DEFAULT 1.0000 AFTER invite_batch_size"
    );
    ensureColumn(
        "products.invite_repeat_ratio",
        "SELECT invite_repeat_ratio FROM products LIMIT 1",
        "ALTER TABLE products ADD COLUMN invite_repeat_ratio DECIMAL(10,4) NULL DEFAULT 0.2000 AFTER invite_first_ratio"
    );

    ensureColumn(
        "orders.pay_type",
        "SELECT pay_type FROM orders LIMIT 1",
        "ALTER TABLE orders "
            + "ADD COLUMN pay_type VARCHAR(32) NULL, "
            + "ADD COLUMN transaction_id VARCHAR(64) NULL, "
            + "ADD COLUMN refund_status VARCHAR(32) NOT NULL DEFAULT 'NONE', "
            + "ADD COLUMN refund_no VARCHAR(64) NULL, "
            + "ADD COLUMN refund_id VARCHAR(64) NULL, "
            + "ADD COLUMN refund_apply_time TIMESTAMP NULL"
    );
    ensureColumn(
        "orders.refund_no",
        "SELECT refund_no FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN refund_no VARCHAR(64) NULL AFTER refund_status"
    );
    ensureColumn(
        "orders.refund_apply_time",
        "SELECT refund_apply_time FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN refund_apply_time TIMESTAMP NULL AFTER refund_id"
    );
    ensureColumn(
        "orders.refund_request_status",
        "SELECT refund_request_status FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN refund_request_status VARCHAR(32) NOT NULL DEFAULT 'NONE',"
            + " ADD COLUMN refund_request_reason VARCHAR(255) NULL,"
            + " ADD COLUMN refund_request_at TIMESTAMP NULL,"
            + " ADD COLUMN refund_review_at TIMESTAMP NULL,"
            + " ADD COLUMN refund_review_remark VARCHAR(255) NULL,"
            + " ADD COLUMN refund_review_admin_id BIGINT NULL"
    );

    ensureColumn(
        "sms_login_codes.scene",
        "SELECT scene FROM sms_login_codes LIMIT 1",
        "ALTER TABLE sms_login_codes ADD COLUMN scene VARCHAR(32) NOT NULL DEFAULT 'REGISTER' AFTER code"
    );

    ensureColumn(
        "orders.order_no",
        "SELECT order_no FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN order_no VARCHAR(64) NULL"
    );
    ensureColumn(
        "user_wechat_auth.source_type",
        "SELECT source_type FROM user_wechat_auth LIMIT 1",
        "ALTER TABLE user_wechat_auth ADD COLUMN source_type VARCHAR(32) NOT NULL DEFAULT 'MINIAPP' AFTER user_id"
    );
    ensureColumn(
        "user_wechat_auth.unionid",
        "SELECT unionid FROM user_wechat_auth LIMIT 1",
        "ALTER TABLE user_wechat_auth ADD COLUMN unionid VARCHAR(128) NULL AFTER openid"
    );
    ensureColumn(
        "user_wechat_auth.avatar_url",
        "SELECT avatar_url FROM user_wechat_auth LIMIT 1",
        "ALTER TABLE user_wechat_auth ADD COLUMN avatar_url VARCHAR(255) NULL AFTER nickname"
    );
    ensureColumn(
        "user_wechat_auth.updated_at",
        "SELECT updated_at FROM user_wechat_auth LIMIT 1",
        "ALTER TABLE user_wechat_auth ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"
    );
    dropConstraintIfExists(
        "user_wechat_auth.user_id unique index",
        "ALTER TABLE user_wechat_auth DROP INDEX user_id"
    );
    dropConstraintIfExists(
        "user_wechat_auth.user_id unique constraint",
        "ALTER TABLE user_wechat_auth DROP CONSTRAINT user_id"
    );
    ensureColumn(
        "orders.order_status",
        "SELECT order_status FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN order_status VARCHAR(32) NOT NULL DEFAULT 'PENDING'"
    );
    ensureColumn(
        "orders.pay_amount",
        "SELECT pay_amount FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN pay_amount DECIMAL(10,2) NULL DEFAULT 0"
    );
    ensureColumn(
        "orders.remark",
        "SELECT remark FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN remark VARCHAR(255) NULL"
    );
    ensureColumn(
        "orders.updated_at",
        "SELECT updated_at FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP"
    );
    ensureColumn(
        "orders.pay_time",
        "SELECT pay_time FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN pay_time TIMESTAMP NULL"
    );
    ensureColumn(
        "orders.complete_time",
        "SELECT complete_time FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN complete_time TIMESTAMP NULL"
    );
    ensureColumn(
        "orders.product_id",
        "SELECT product_id FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN product_id BIGINT NULL"
    );
    ensureColumn(
        "orders.quantity",
        "SELECT quantity FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN quantity INT NOT NULL DEFAULT 1"
    );
    ensureColumn(
        "orders.recipient_name",
        "SELECT recipient_name FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN recipient_name VARCHAR(64) NULL"
    );
    ensureColumn(
        "orders.recipient_phone",
        "SELECT recipient_phone FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN recipient_phone VARCHAR(32) NULL"
    );
    ensureColumn(
        "orders.address",
        "SELECT address FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN address VARCHAR(255) NULL"
    );
    ensureColumn(
        "orders.province",
        "SELECT province FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN province VARCHAR(64) NULL AFTER recipient_phone"
    );
    ensureColumn(
        "orders.city",
        "SELECT city FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN city VARCHAR(64) NULL AFTER province"
    );
    ensureColumn(
        "orders.district",
        "SELECT district FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN district VARCHAR(64) NULL AFTER city"
    );
    ensureColumn(
        "orders.product_amount",
        "SELECT product_amount FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN product_amount DECIMAL(10,2) NULL AFTER quantity"
    );
    ensureColumn(
        "orders.shipping_fee",
        "SELECT shipping_fee FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN shipping_fee DECIMAL(10,2) NULL DEFAULT 0 AFTER product_amount"
    );
    ensureColumn(
        "orders.cashback_base_amount",
        "SELECT cashback_base_amount FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN cashback_base_amount DECIMAL(10,2) NULL AFTER shipping_fee"
    );
    ensureColumn(
        "orders.tracking_no",
        "SELECT tracking_no FROM orders LIMIT 1",
        "ALTER TABLE orders ADD COLUMN tracking_no VARCHAR(64) NULL"
    );
    ensureRenamedColumn(
        "cashback_records.order_id -> related_order_id",
        "cashback_records",
        "order_id",
        "related_order_id",
        "BIGINT NULL"
    );
    ensureRenamedColumn(
        "cashback_records.type -> cashback_type",
        "cashback_records",
        "type",
        "cashback_type",
        "VARCHAR(32) NOT NULL"
    );
    ensureRenamedColumn(
        "cashback_records.batch_no -> related_invite_batch_id",
        "cashback_records",
        "batch_no",
        "related_invite_batch_id",
        "INT NULL"
    );
    ensureColumn(
        "cashback_records.related_order_id",
        "SELECT related_order_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN related_order_id BIGINT NULL"
    );
    ensureColumn(
        "cashback_records.cashback_type",
        "SELECT cashback_type FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN cashback_type VARCHAR(32) NOT NULL DEFAULT 'PERSONAL_ORDER'"
    );
    ensureColumn(
        "cashback_records.related_invite_batch_id",
        "SELECT related_invite_batch_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN related_invite_batch_id INT NULL"
    );
    ensureColumn(
        "cashback_records.product_id",
        "SELECT product_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN product_id BIGINT NULL AFTER related_order_id"
    );
    dropConstraintIfExists(
        "cashback_records.related_invite_batch_id legacy invite batch foreign key",
        "ALTER TABLE cashback_records DROP FOREIGN KEY fk_cashback_records_batch"
    );
    ensureColumn(
        "cashback_records.out_batch_no",
        "SELECT out_batch_no FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN out_batch_no VARCHAR(64) NULL"
    );
    ensureColumn(
        "cashback_records.out_detail_no",
        "SELECT out_detail_no FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN out_detail_no VARCHAR(64) NULL"
    );
    ensureColumn(
        "cashback_records.transfer_id",
        "SELECT transfer_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN transfer_id VARCHAR(64) NULL"
    );
    ensureColumn(
        "cashback_records.transfer_detail_id",
        "SELECT transfer_detail_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN transfer_detail_id VARCHAR(64) NULL"
    );
    ensureColumn(
        "cashback_records.transfer_fail_reason",
        "SELECT transfer_fail_reason FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN transfer_fail_reason VARCHAR(512) NULL"
    );
    ensureColumn(
        "cashback_records.transfer_package_info",
        "SELECT transfer_package_info FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN transfer_package_info VARCHAR(1024) NULL"
    );
    ensureColumn(
        "cashback_records.transfer_time",
        "SELECT transfer_time FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN transfer_time TIMESTAMP NULL"
    );
    ensureColumn(
        "cashback_records.eligible_at",
        "SELECT eligible_at FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN eligible_at TIMESTAMP NULL"
    );
    ensureColumn(
        "cashback_records.withdrawal_request_id",
        "SELECT withdrawal_request_id FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN withdrawal_request_id BIGINT NULL"
    );
    ensureColumn(
        "cashback_records.early_withdrawal",
        "SELECT early_withdrawal FROM cashback_records LIMIT 1",
        "ALTER TABLE cashback_records ADD COLUMN early_withdrawal TINYINT NOT NULL DEFAULT 0"
    );

    ensureColumn(
        "withdrawal_requests.idempotency_key",
        "SELECT idempotency_key FROM withdrawal_requests LIMIT 1",
        "ALTER TABLE withdrawal_requests ADD COLUMN idempotency_key VARCHAR(64) NULL UNIQUE"
    );
    ensureColumn(
        "withdrawal_requests.request_no",
        "SELECT request_no FROM withdrawal_requests LIMIT 1",
        "ALTER TABLE withdrawal_requests ADD COLUMN request_no VARCHAR(32) NULL UNIQUE"
    );
    ensureColumn(
        "withdrawal_requests.requested_amount",
        "SELECT requested_amount FROM withdrawal_requests LIMIT 1",
        "ALTER TABLE withdrawal_requests ADD COLUMN requested_amount DECIMAL(10,2) NULL AFTER amount"
    );
    ensureColumn(
        "withdrawal_requests.apply_mode",
        "SELECT apply_mode FROM withdrawal_requests LIMIT 1",
        "ALTER TABLE withdrawal_requests ADD COLUMN apply_mode VARCHAR(32) NOT NULL DEFAULT 'MATURED_ONLY' AFTER source"
    );

    ensureTable(
        "cashback_debts",
        "CREATE TABLE IF NOT EXISTS cashback_debts ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "user_id BIGINT NOT NULL,"
            + "order_id BIGINT NULL,"
            + "cashback_id BIGINT NULL,"
            + "amount DECIMAL(10,2) NOT NULL,"
            + "reason VARCHAR(255) NOT NULL,"
            + "status VARCHAR(32) NOT NULL DEFAULT 'PENDING',"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "INDEX idx_user_status (user_id, status)"
            + ")"
    );

    ensureColumn(
        "users.cashback_reset_at",
        "SELECT cashback_reset_at FROM users LIMIT 1",
        "ALTER TABLE users ADD COLUMN cashback_reset_at TIMESTAMP NULL AFTER miniapp_openid"
    );

    syncLegacyOrderColumns();
    backfillWithdrawalRequestedAmount();
    backfillCashbackEligibility();
    backfillProductDetailContent();
    backfillProductSalesCount();
    backfillProductCashbackRules();
    backfillFeaturedProduct();
    backfillOrderAmountBreakdown();
    backfillCashbackProductId();
    backfillInviteProductRelations();
  }

  private void ensureTable(String label, String ddlSql) {
    try {
      jdbcTemplate.execute(ddlSql);
      log.info("Ensure table {} completed.", label);
    } catch (Exception ex) {
      log.error("Ensure table {} failed.", label, ex);
    }
  }

  private void ensureColumn(String label, String probeSql, String alterSql) {
    try {
      jdbcTemplate.execute(probeSql);
    } catch (Exception e) {
      log.info("Detected missing {}, applying migration.", label);
      try {
        jdbcTemplate.execute(alterSql);
        log.info("Migration for {} completed.", label);
      } catch (Exception ex) {
        log.error("Migration for {} failed.", label, ex);
      }
    }
  }

  private void ensureRenamedColumn(String label, String tableName, String oldColumn, String newColumn, String columnDefinition) {
    if (columnExists(tableName, newColumn) || !columnExists(tableName, oldColumn)) {
      return;
    }

    log.info("Detected legacy column {}, applying rename migration.", label);
    try {
      jdbcTemplate.execute(
          "ALTER TABLE `" + tableName + "` CHANGE COLUMN `" + oldColumn + "` `" + newColumn + "` " + columnDefinition
      );
      log.info("Rename migration for {} completed.", label);
    } catch (Exception ex) {
      log.error("Rename migration for {} failed.", label, ex);
    }
  }

  private boolean columnExists(String tableName, String columnName) {
    try {
      Integer count = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM information_schema.columns "
              + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
          Integer.class,
          tableName,
          columnName
      );
      return count != null && count > 0;
    } catch (Exception ex) {
      log.warn("Check column {}.{} failed.", tableName, columnName, ex);
      return false;
    }
  }

  private void dropConstraintIfExists(String label, String sql) {
    try {
      jdbcTemplate.execute(sql);
      log.info("Dropped {}.", label);
    } catch (Exception ignored) {
      // The index/constraint may not exist or may use a database-specific name.
    }
  }

  private void syncLegacyOrderColumns() {
    try {
      jdbcTemplate.update(
          "UPDATE orders SET order_status = status "
              + "WHERE status IS NOT NULL "
              + "AND (order_status IS NULL OR order_status = '' "
              + "OR (order_status = 'PENDING' AND status IN ('PAID','SHIPPED','COMPLETED','REFUNDED')))"
      );
    } catch (Exception ignored) {
      // Skip when the old/new source column does not exist.
    }
    try {
      jdbcTemplate.update("UPDATE orders SET pay_time = paid_at WHERE pay_time IS NULL AND paid_at IS NOT NULL");
    } catch (Exception ignored) {
      // Skip when the old/new source column does not exist.
    }
    try {
      jdbcTemplate.update("UPDATE orders SET complete_time = completed_at WHERE complete_time IS NULL AND completed_at IS NOT NULL");
    } catch (Exception ignored) {
      // Skip when the old/new source column does not exist.
    }
    try {
      jdbcTemplate.update(
          "UPDATE orders SET pay_amount = total_amount "
              + "WHERE total_amount IS NOT NULL AND (pay_amount IS NULL OR pay_amount = 0)"
      );
    } catch (Exception ignored) {
      // Skip when the old/new source column does not exist.
    }
    try {
      jdbcTemplate.update("UPDATE orders SET updated_at = created_at WHERE updated_at IS NULL AND created_at IS NOT NULL");
    } catch (Exception ignored) {
      // Skip when the old/new source column does not exist.
    }
    try {
      jdbcTemplate.update(
          "UPDATE orders SET order_no = CONCAT('ZX', DATE_FORMAT(created_at, '%Y%m%d%H%i%s'), LPAD(id, 4, '0')) "
              + "WHERE order_no IS NULL"
      );
    } catch (Exception ignored) {
      // Skip when the SQL dialect or columns are not compatible.
    }
  }

  private void backfillProductSalesCount() {
    try {
      jdbcTemplate.update(
          "UPDATE products p SET sales_count = ("
              + "SELECT COALESCE(SUM(o.quantity), 0) "
              + "FROM orders o "
              + "WHERE o.product_id = p.id AND o.order_status IN ('PAID', 'SHIPPED', 'COMPLETED')"
              + ")"
      );
      log.info("Backfilled product sales_count from paid orders.");
    } catch (Exception ex) {
      log.warn("Backfill product sales_count failed.", ex);
    }
  }

  private void backfillProductDetailContent() {
    try {
      jdbcTemplate.update(
          "UPDATE products SET detail_content = description "
              + "WHERE (detail_content IS NULL OR detail_content = '') "
              + "AND description IS NOT NULL AND description <> ''"
      );
      log.info("Backfilled product detail_content from description.");
    } catch (Exception ex) {
      log.warn("Backfill product detail_content failed.", ex);
    }
  }

  private void backfillFeaturedProduct() {
    try {
      Integer featuredCount = jdbcTemplate.queryForObject(
          "SELECT COUNT(*) FROM products WHERE status = 1 AND COALESCE(is_featured, 0) = 1",
          Integer.class
      );
      if (featuredCount != null && featuredCount > 0) {
        return;
      }

      Long productId = jdbcTemplate.queryForObject(
          "SELECT id FROM products WHERE status = 1 ORDER BY COALESCE(sales_count, 0) DESC, id DESC LIMIT 1",
          Long.class
      );
      if (productId == null) {
        return;
      }
      jdbcTemplate.update("UPDATE products SET is_featured = 1 WHERE id = ?", productId);
      log.info("Backfilled featured product id={}.", productId);
    } catch (Exception ex) {
      log.warn("Backfill featured product failed or no active products exist.", ex);
    }
  }

  private void backfillProductCashbackRules() {
    try {
      jdbcTemplate.update(
          "UPDATE products SET personal_second_ratio = 0.1000 "
              + "WHERE personal_second_ratio IS NULL"
      );
      jdbcTemplate.update(
          "UPDATE products SET personal_third_ratio = 0.2000 "
              + "WHERE personal_third_ratio IS NULL"
      );
      jdbcTemplate.update(
          "UPDATE products SET personal_fourth_ratio = 1.0000 "
              + "WHERE personal_fourth_ratio IS NULL"
      );
      jdbcTemplate.update(
          "UPDATE products SET invite_batch_size = 3 "
              + "WHERE invite_batch_size IS NULL OR invite_batch_size < 1"
      );
      jdbcTemplate.update(
          "UPDATE products SET invite_first_ratio = 1.0000 "
              + "WHERE invite_first_ratio IS NULL"
      );
      jdbcTemplate.update(
          "UPDATE products SET invite_repeat_ratio = 0.2000 "
              + "WHERE invite_repeat_ratio IS NULL"
      );
      log.info("Backfilled product cashback rules.");
    } catch (Exception ex) {
      log.warn("Backfill product cashback rules failed.", ex);
    }
  }

  private void backfillCashbackEligibility() {
    try {
      jdbcTemplate.update(
          "UPDATE cashback_records SET eligible_at = DATE_ADD(created_at, INTERVAL 7 DAY) "
              + "WHERE eligible_at IS NULL AND created_at IS NOT NULL"
      );
      log.info("Backfilled cashback eligible_at.");
    } catch (Exception ex) {
      log.warn("Backfill cashback eligible_at failed.", ex);
    }
  }

  private void backfillWithdrawalRequestedAmount() {
    try {
      jdbcTemplate.update(
          "UPDATE withdrawal_requests SET requested_amount = amount WHERE requested_amount IS NULL AND amount IS NOT NULL"
      );
      log.info("Backfilled withdrawal requested_amount.");
    } catch (Exception ex) {
      log.warn("Backfill withdrawal requested_amount failed.", ex);
    }
  }

  private void backfillCashbackProductId() {
    try {
      jdbcTemplate.update(
          "UPDATE cashback_records cr "
              + "JOIN orders o ON o.id = cr.related_order_id "
              + "SET cr.product_id = o.product_id "
              + "WHERE cr.product_id IS NULL AND o.product_id IS NOT NULL"
      );
      log.info("Backfilled cashback_records.product_id from related orders.");
    } catch (Exception ex) {
      log.warn("Backfill cashback_records.product_id failed.", ex);
    }
  }

  private void backfillInviteProductRelations() {
    try {
      jdbcTemplate.update(
          "INSERT IGNORE INTO invite_product_relations(inviter_user_id, invitee_user_id, product_id, bind_time, first_paid_time) "
              + "SELECT ir.inviter_user_id, ir.invitee_user_id, po.product_id, ir.bind_time, po.first_paid_time "
              + "FROM invite_relations ir "
              + "JOIN ("
              + "  SELECT user_id, product_id, MIN(pay_time) AS first_paid_time "
              + "  FROM orders "
              + "  WHERE product_id IS NOT NULL "
              + "    AND order_status IN ('PAID', 'SHIPPED', 'COMPLETED') "
              + "    AND pay_time IS NOT NULL "
              + "  GROUP BY user_id, product_id"
              + ") po ON po.user_id = ir.invitee_user_id "
              + "WHERE ir.inviter_user_id IS NOT NULL"
      );
      log.info("Backfilled invite_product_relations from invite relations and paid orders.");
    } catch (Exception ex) {
      log.warn("Backfill invite_product_relations failed.", ex);
    }
  }

  private void backfillOrderAmountBreakdown() {
    try {
      jdbcTemplate.update(
          "UPDATE orders SET product_amount = total_amount "
              + "WHERE total_amount IS NOT NULL AND (product_amount IS NULL OR product_amount = 0)"
      );
      jdbcTemplate.update(
          "UPDATE orders SET shipping_fee = 0 WHERE shipping_fee IS NULL"
      );
      jdbcTemplate.update(
          "UPDATE orders SET cashback_base_amount = product_amount "
              + "WHERE product_amount IS NOT NULL AND (cashback_base_amount IS NULL OR cashback_base_amount = 0)"
      );
      log.info("Backfilled order amount breakdown.");
    } catch (Exception ex) {
      log.warn("Backfill order amount breakdown failed.", ex);
    }
  }
}
