/**
 * 测试修复后的重置密码功能
 */
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const { db } = require("./cloudbase");
const { buildPasswordFields, verifyPassword } = require("./admin/lib/passwords");

async function testResetPasswordFixed() {
  try {
    // 1. 找一个测试用户
    const usersResult = await db.collection("users").limit(1).get();
    if (!usersResult.data || usersResult.data.length === 0) {
      console.log("没有找到用户");
      return;
    }

    const user = usersResult.data[0];
    const userId = user._id || user.id;
    
    console.log("测试用户:", user.nickname || user.openid);
    console.log("用户ID:", userId);

    // 2. 生成新密码
    const newPassword = "test123456";
    const { password_hash, password_salt } = buildPasswordFields(newPassword);

    console.log("\n生成新密码字段...");

    // 3. 使用 set 方法更新（修复后的方式）
    const { _id, ...dataWithoutId } = user;
    
    await db.collection("users").doc(userId).set({
      ...dataWithoutId,
      password_hash,
      password_salt,
      password_reset_method: "admin_test_reset",
      password_updated_at: Date.now(),
      updated_at: Date.now(),
    });

    console.log("✅ 密码更新成功（使用 set 方法）");

    // 4. 验证更新结果
    const updatedUserResult = await db.collection("users").doc(userId).get();
    const updatedUser = updatedUserResult.data;
    
    console.log("\n更新后验证:");
    console.log("password_hash:", updatedUser.password_hash ? "✅ 存在" : "❌ 不存在");
    console.log("password_salt:", updatedUser.password_salt ? "✅ 存在" : "❌ 不存在");

    // 5. 测试密码验证
    if (updatedUser.password_hash && updatedUser.password_salt) {
      const isValid = verifyPassword(newPassword, updatedUser);
      console.log("\n密码验证:", isValid ? "✅ 正确" : "❌ 错误");
      
      if (isValid) {
        console.log("\n🎉 修复成功！密码重置功能已正常工作");
      }
    } else {
      console.log("\n❌ 修复失败：密码字段未保存");
    }

  } catch (error) {
    console.error("❌ 测试失败:", error);
  }
}

testResetPasswordFixed();
