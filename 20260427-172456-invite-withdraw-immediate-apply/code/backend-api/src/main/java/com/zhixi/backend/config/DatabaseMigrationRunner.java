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
            + "status VARCHAR(32) NOT NULL,"
            + "source VARCHAR(32) NOT NULL DEFAULT 'USER',"
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
        "users.cashback_reset_at",
        "SELECT cashback_reset_at FROM users LIMIT 1",
        "ALTER TABLE users ADD COLUMN cashback_reset_at TIMESTAMP NULL AFTER miniapp_openid"
    );

    syncLegacyOrderColumns();
    backfillCashbackEligibility();
    backfillProductSalesCount();
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
      jdbcTemplate.update("UPDATE orders SET order_status = status WHERE order_status IS NULL AND status IS NOT NULL");
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
      jdbcTemplate.update("UPDATE orders SET pay_amount = total_amount WHERE pay_amount IS NULL AND total_amount IS NOT NULL");
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
}
