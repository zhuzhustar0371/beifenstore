INSERT INTO products (name, price, description, image_url, active)
VALUES
  ('知禧洗衣液', 10.00, '单价 10.00 元/件，适合线上支付与返现链路测试。', '/images/product.png', TRUE);

INSERT INTO users (mobile, password_hash, nickname, invite_code, inviter_user_id, status)
VALUES
  ('13800138000', 'f55586fc3faf59b4582b1717a48dd7726c18735c6a0bbcd25b23756ef9a7b6e0', '测试用户01', 'ZX8888', NULL, 1);

INSERT INTO admins (username, password_hash, display_name, mobile, status)
VALUES
  ('zhixi_admin', 'f55586fc3faf59b4582b1717a48dd7726c18735c6a0bbcd25b23756ef9a7b6e0', '知禧管理员', NULL, 1);

INSERT INTO admin_roles (code, name)
VALUES
  ('SUPER_ADMIN', '超级管理员'),
  ('OPERATOR', '运营人员');

INSERT INTO admin_permissions (code, name)
VALUES
  ('dashboard:read', '看板查看'),
  ('users:read', '用户查看'),
  ('users:write', '用户修改'),
  ('orders:read', '订单查看'),
  ('orders:write', '订单修改'),
  ('products:read', '商品查看'),
  ('products:write', '商品修改'),
  ('invites:read', '邀请查看'),
  ('cashbacks:read', '返现查看'),
  ('cashbacks:write', '返现打款'),
  ('audit:read', '审计日志查看');

INSERT INTO admin_role_permissions (role_code, permission_code)
VALUES
  ('SUPER_ADMIN', 'dashboard:read'),
  ('SUPER_ADMIN', 'users:read'),
  ('SUPER_ADMIN', 'users:write'),
  ('SUPER_ADMIN', 'orders:read'),
  ('SUPER_ADMIN', 'orders:write'),
  ('SUPER_ADMIN', 'products:read'),
  ('SUPER_ADMIN', 'products:write'),
  ('SUPER_ADMIN', 'invites:read'),
  ('SUPER_ADMIN', 'cashbacks:read'),
  ('SUPER_ADMIN', 'cashbacks:write'),
  ('SUPER_ADMIN', 'audit:read'),
  ('OPERATOR', 'dashboard:read'),
  ('OPERATOR', 'users:read'),
  ('OPERATOR', 'orders:read'),
  ('OPERATOR', 'products:read'),
  ('OPERATOR', 'invites:read'),
  ('OPERATOR', 'cashbacks:read');
