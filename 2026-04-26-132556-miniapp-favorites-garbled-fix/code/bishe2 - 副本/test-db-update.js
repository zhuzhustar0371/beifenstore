/**
 * 测试数据库更新功能
 */
const path = require("path");
require("dotenv").config({ path: path.join(__dirname, ".env") });

const { db } = require("./cloudbase");

async function testDbUpdate() {
  try {
    // 找一个测试用户
    const usersResult = await db.collection("users").limit(1).get();
    if (!usersResult.data || usersResult.data.length === 0) {
      console.log("没有找到用户");
      return;
    }

    const user = usersResult.data[0];
    const userId = user._id || user.id;

    console.log("测试用户ID:", userId);
    console.log("更新前 test_field:", user.test_field || "不存在");

    // 尝试更新一个测试字段
    try {
      const updateResult = await db.collection("users").doc(userId).update({
        test_field: "test_value_" + Date.now(),
        updated_at: Date.now(),
      });
      console.log("\n更新结果:", JSON.stringify(updateResult, null, 2));
    } catch (updateError) {
      console.error("\n❌ 更新失败:", updateError.message);
      console.error("错误详情:", updateError);
    }

    // 验证更新
    const updatedUser = await db.collection("users").doc(userId).get();
    console.log("\n更新后 test_field:", updatedUser.data.test_field || "不存在");

  } catch (error) {
    console.error("❌ 测试失败:", error);
  }
}

testDbUpdate();
