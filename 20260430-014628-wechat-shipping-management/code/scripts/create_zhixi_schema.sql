-- Zhixi project MySQL schema
-- Based on docs/01-完整需求文档.md
-- MySQL version: 8.0+

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE DATABASE IF NOT EXISTS `zhixi`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE `zhixi`;

-- ----------------------------
-- Admin and auth
-- ----------------------------
CREATE TABLE IF NOT EXISTS `admin_roles` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `role_code` VARCHAR(64) NOT NULL,
  `role_name` VARCHAR(128) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_roles_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admins` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `display_name` VARCHAR(128) NOT NULL,
  `mobile` VARCHAR(32) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled,0=disabled',
  `last_login_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admins_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_role_rel` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `role_id` BIGINT UNSIGNED NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_role_rel` (`admin_id`, `role_id`),
  KEY `idx_admin_role_rel_role` (`role_id`),
  CONSTRAINT `fk_admin_role_rel_admin` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`id`),
  CONSTRAINT `fk_admin_role_rel_role` FOREIGN KEY (`role_id`) REFERENCES `admin_roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_login_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `ip` VARCHAR(64) DEFAULT NULL,
  `user_agent` VARCHAR(512) DEFAULT NULL,
  `login_result` TINYINT NOT NULL DEFAULT 1 COMMENT '1=success,0=failed',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_login_logs_admin_id` (`admin_id`),
  CONSTRAINT `fk_admin_login_logs_admin` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_operation_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `module` VARCHAR(64) NOT NULL,
  `action` VARCHAR(128) NOT NULL,
  `target_type` VARCHAR(64) DEFAULT NULL,
  `target_id` BIGINT UNSIGNED DEFAULT NULL,
  `request_payload` JSON DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_logs_admin_id` (`admin_id`),
  KEY `idx_admin_operation_logs_module` (`module`),
  CONSTRAINT `fk_admin_operation_logs_admin` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `admin_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `admin_id` BIGINT UNSIGNED NOT NULL,
  `token` VARCHAR(128) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_sessions_token` (`token`),
  KEY `idx_admin_sessions_admin_id` (`admin_id`),
  KEY `idx_admin_sessions_expires_at` (`expires_at`),
  CONSTRAINT `fk_admin_sessions_admin` FOREIGN KEY (`admin_id`) REFERENCES `admins` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- User and profile
-- ----------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `nickname` VARCHAR(128) NOT NULL,
  `mobile` VARCHAR(32) NOT NULL,
  `avatar_url` VARCHAR(512) DEFAULT NULL,
  `invite_code` VARCHAR(32) NOT NULL,
  `inviter_user_id` BIGINT UNSIGNED DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=active,0=disabled',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_mobile` (`mobile`),
  UNIQUE KEY `uk_users_invite_code` (`invite_code`),
  KEY `idx_users_inviter_user_id` (`inviter_user_id`),
  CONSTRAINT `fk_users_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_auth_wechat` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `openid` VARCHAR(128) NOT NULL,
  `unionid` VARCHAR(128) DEFAULT NULL,
  `session_key` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_auth_wechat_openid` (`openid`),
  UNIQUE KEY `uk_user_auth_wechat_user_id` (`user_id`),
  KEY `idx_user_auth_wechat_unionid` (`unionid`),
  CONSTRAINT `fk_user_auth_wechat_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_addresses` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `receiver_name` VARCHAR(64) NOT NULL,
  `receiver_mobile` VARCHAR(32) NOT NULL,
  `province` VARCHAR(64) NOT NULL,
  `city` VARCHAR(64) NOT NULL,
  `district` VARCHAR(64) NOT NULL,
  `detail_address` VARCHAR(255) NOT NULL,
  `postal_code` VARCHAR(20) DEFAULT NULL,
  `is_default` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_addresses_user_id` (`user_id`),
  KEY `idx_user_addresses_default` (`user_id`, `is_default`),
  CONSTRAINT `fk_user_addresses_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Product
-- ----------------------------
CREATE TABLE IF NOT EXISTS `products` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `sub_title` VARCHAR(255) DEFAULT NULL,
  `description` TEXT,
  `main_image_url` VARCHAR(512) DEFAULT NULL,
  `price` DECIMAL(10,2) NOT NULL DEFAULT 99.00,
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1=on_sale,0=off_sale',
  `is_featured` TINYINT NOT NULL DEFAULT 0 COMMENT '1=featured/default price binding product',
  `sort_no` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_products_status_sort` (`status`, `sort_no`),
  KEY `idx_products_featured` (`status`, `is_featured`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Order and payment
-- ----------------------------
CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(64) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `address_id` BIGINT UNSIGNED DEFAULT NULL,
  `order_status` VARCHAR(32) NOT NULL COMMENT 'PENDING_PAY,PAID,SHIPPED,COMPLETED,CANCELLED,CLOSED,REFUNDED',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `pay_amount` DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  `pay_time` DATETIME DEFAULT NULL,
  `cancel_time` DATETIME DEFAULT NULL,
  `complete_time` DATETIME DEFAULT NULL,
  `remark` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_id_status` (`user_id`, `order_status`),
  KEY `idx_orders_created_at` (`created_at`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_orders_address` FOREIGN KEY (`address_id`) REFERENCES `user_addresses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `product_name` VARCHAR(255) NOT NULL,
  `price` DECIMAL(10,2) NOT NULL,
  `quantity` INT NOT NULL DEFAULT 1,
  `line_amount` DECIMAL(10,2) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_items_order_id` (`order_id`),
  KEY `idx_order_items_product_id` (`product_id`),
  CONSTRAINT `fk_order_items_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_order_items_product` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `order_status_logs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `from_status` VARCHAR(32) DEFAULT NULL,
  `to_status` VARCHAR(32) NOT NULL,
  `operator_type` VARCHAR(32) NOT NULL COMMENT 'SYSTEM,USER,ADMIN',
  `operator_id` BIGINT UNSIGNED DEFAULT NULL,
  `remark` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_order_status_logs_order_id` (`order_id`),
  CONSTRAINT `fk_order_status_logs_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `payment_transactions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `out_trade_no` VARCHAR(64) NOT NULL,
  `wechat_transaction_id` VARCHAR(64) DEFAULT NULL,
  `pay_channel` VARCHAR(32) NOT NULL DEFAULT 'WECHAT',
  `pay_status` VARCHAR(32) NOT NULL COMMENT 'INIT,SUCCESS,FAILED,CLOSED,REFUNDED',
  `pay_amount` DECIMAL(10,2) NOT NULL,
  `notify_payload` JSON DEFAULT NULL,
  `paid_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_transactions_out_trade_no` (`out_trade_no`),
  KEY `idx_payment_transactions_order_id` (`order_id`),
  CONSTRAINT `fk_payment_transactions_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `shipping_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `company_name` VARCHAR(128) DEFAULT NULL,
  `tracking_no` VARCHAR(128) DEFAULT NULL,
  `ship_time` DATETIME DEFAULT NULL,
  `receive_time` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_shipping_records_order_id` (`order_id`),
  KEY `idx_shipping_records_tracking_no` (`tracking_no`),
  CONSTRAINT `fk_shipping_records_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `refund_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `refund_no` VARCHAR(64) NOT NULL,
  `refund_reason` VARCHAR(512) DEFAULT NULL,
  `refund_amount` DECIMAL(10,2) NOT NULL,
  `refund_status` VARCHAR(32) NOT NULL COMMENT 'INIT,SUCCESS,FAILED',
  `wechat_refund_id` VARCHAR(64) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_records_refund_no` (`refund_no`),
  KEY `idx_refund_records_order_id` (`order_id`),
  CONSTRAINT `fk_refund_records_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Invite and cashback
-- ----------------------------
CREATE TABLE IF NOT EXISTS `invite_relations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inviter_user_id` BIGINT UNSIGNED NOT NULL,
  `invitee_user_id` BIGINT UNSIGNED NOT NULL,
  `bind_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `first_paid_order_id` BIGINT UNSIGNED DEFAULT NULL,
  `first_paid_time` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_relations_invitee_user_id` (`invitee_user_id`),
  KEY `idx_invite_relations_inviter_user_id` (`inviter_user_id`),
  CONSTRAINT `fk_invite_relations_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_invite_relations_invitee` FOREIGN KEY (`invitee_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_invite_relations_first_order` FOREIGN KEY (`first_paid_order_id`) REFERENCES `orders` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `invite_batches` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `inviter_user_id` BIGINT UNSIGNED NOT NULL,
  `batch_no` INT NOT NULL COMMENT '1,2,3...',
  `invitee_count` INT NOT NULL DEFAULT 3,
  `cashback_amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'SETTLED' COMMENT 'PENDING,SETTLED,ROLLED_BACK',
  `settled_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_batches_inviter_batch` (`inviter_user_id`, `batch_no`),
  CONSTRAINT `fk_invite_batches_inviter` FOREIGN KEY (`inviter_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `cashback_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `cashback_type` VARCHAR(32) NOT NULL COMMENT 'PERSONAL_ORDER,INVITE_BATCH,ROLLBACK',
  `related_order_id` BIGINT UNSIGNED DEFAULT NULL,
  `related_invite_batch_id` BIGINT UNSIGNED DEFAULT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'SETTLED' COMMENT 'PENDING,SETTLED,ROLLED_BACK',
  `remark` VARCHAR(512) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cashback_records_user_id` (`user_id`),
  KEY `idx_cashback_records_type` (`cashback_type`),
  KEY `idx_cashback_records_created_at` (`created_at`),
  CONSTRAINT `fk_cashback_records_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_cashback_records_order` FOREIGN KEY (`related_order_id`) REFERENCES `orders` (`id`),
  CONSTRAINT `fk_cashback_records_batch` FOREIGN KEY (`related_invite_batch_id`) REFERENCES `invite_batches` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `token` VARCHAR(128) NOT NULL,
  `login_type` VARCHAR(32) NOT NULL COMMENT 'SMS,WECHAT',
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_sessions_token` (`token`),
  KEY `idx_user_sessions_user_id` (`user_id`),
  CONSTRAINT `fk_user_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `sms_login_codes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone` VARCHAR(32) NOT NULL,
  `code` VARCHAR(8) NOT NULL,
  `used` TINYINT(1) NOT NULL DEFAULT 0,
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sms_login_codes_phone` (`phone`),
  KEY `idx_sms_login_codes_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_wechat_auth` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `openid` VARCHAR(128) NOT NULL,
  `unionid` VARCHAR(128) DEFAULT NULL,
  `nickname` VARCHAR(128) DEFAULT NULL,
  `avatar_url` VARCHAR(255) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_wechat_auth_user_id` (`user_id`),
  UNIQUE KEY `uk_user_wechat_auth_openid` (`openid`),
  CONSTRAINT `fk_user_wechat_auth_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `wechat_qr_sessions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `scene` VARCHAR(64) NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING,SCANNED,AUTHORIZED,EXPIRED',
  `openid` VARCHAR(128) DEFAULT NULL,
  `nickname` VARCHAR(128) DEFAULT NULL,
  `user_id` BIGINT UNSIGNED DEFAULT NULL,
  `token` VARCHAR(128) DEFAULT NULL,
  `expires_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `confirmed_at` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wechat_qr_sessions_scene` (`scene`),
  KEY `idx_wechat_qr_sessions_status` (`status`),
  KEY `idx_wechat_qr_sessions_user_id` (`user_id`),
  CONSTRAINT `fk_wechat_qr_sessions_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------
-- Seed data
-- ----------------------------
INSERT INTO `admin_roles` (`role_code`, `role_name`)
VALUES ('SUPER_ADMIN', '超级管理员')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`);

INSERT INTO `admins` (`username`, `password_hash`, `display_name`, `mobile`, `status`)
VALUES ('zhixi_admin', 'f55586fc3faf59b4582b1717a48dd7726c18735c6a0bbcd25b23756ef9a7b6e0', '知禧管理员', NULL, 1)
ON DUPLICATE KEY UPDATE
  `password_hash` = VALUES(`password_hash`),
  `display_name` = VALUES(`display_name`),
  `status` = VALUES(`status`);

INSERT INTO `products` (`name`, `sub_title`, `description`, `main_image_url`, `price`, `status`, `is_featured`, `sort_no`)
VALUES (
  '知禧洗衣液',
  '家庭深层洁净',
  '单价99元/件，温和洁净，适合家庭日常使用。',
  NULL,
  99.00,
  1,
  1,
  1
)
ON DUPLICATE KEY UPDATE
  `sub_title` = VALUES(`sub_title`),
  `description` = VALUES(`description`),
  `price` = VALUES(`price`),
  `status` = VALUES(`status`),
  `is_featured` = VALUES(`is_featured`);

SET FOREIGN_KEY_CHECKS = 1;
