/**
 * 测试重置密码功能
 */
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const { db } = require("./cloudbase");
const { buildPasswordFields } = require("./admin/lib/passwords");

async function testResetPassword() {
  try {
    // 1. 找一个测试用户
    const usersResult = await db.collection("users").limit(1).get();
    if (!usersResult.data || usersResult.data.length === 0) {
      console.log("没有找到用户");
      return;
    }

    const user = usersResult.data[0];
    console.log("测试用户:", user.nickname || user.openid);
    console.log("用户ID:", user._id || user.id);
    console.log("当前 password_hash:", user.password_hash ? "存在" : "不存在");
    console.log("当前 password_salt:", user.password_salt ? "存在" : "不存在");

    // 2. 生成新密码
    const newPassword = "test123456";
    const { password_hash, password_salt } = buildPasswordFields(newPassword);

    console.log("\n生成新密码字段:");
    console.log("新 password_hash:", password_hash.substring(0, 20) + "...");
    console.log("新 password_salt:", password_salt);

    // 3. 更新用户密码
    const userId = user._id || user.id;
    await db.collection("users").doc(userId).update({
      password_hash,
      password_salt,
      password_reset_method: "admin_test_reset",
      password_updated_at: Date.now(),
      updated_at: Date.now(),
    });

    console.log("\n✅ 密码重置成功!");

    // 4. 验证更新结果
    const updatedUser = await db.collection("users").doc(userId).get();
    console.log("\n更新后:");
    console.log("password_hash:", updatedUser.data.password_hash ? "存在" : "不存在");
    console.log("password_salt:", updatedUser.data.password_salt ? "存在" : "不存在");

    // 5. 测试密码验证
    const { verifyPassword } = require("./admin/lib/passwords");
    const isValid = verifyPassword(newPassword, updatedUser.data);
    console.log("\n密码验证结果:", isValid ? "✅ 正确" : "❌ 错误");

  } catch (error) {
    console.error("❌ 测试失败:", error);
  }
}

testResetPassword();
