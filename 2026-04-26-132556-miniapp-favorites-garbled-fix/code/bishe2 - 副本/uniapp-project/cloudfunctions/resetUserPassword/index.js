/**
 * 云函数：管理员重置用户密码
 */
const cloud = require('wx-server-sdk')
const crypto = require('crypto')

cloud.init({
  env: cloud.DYNAMIC_CURRENT_ENV
})

const db = cloud.database()

function createPasswordSalt() {
  return crypto.randomBytes(16).toString('hex')
}

function hashPassword(password, salt) {
  return crypto.pbkdf2Sync(String(password), String(salt), 100000, 64, 'sha512').toString('hex')
}

function buildPasswordFields(password) {
  const password_salt = createPasswordSalt()
  return {
    password_salt,
    password_hash: hashPassword(password, password_salt),
  }
}

exports.main = async (event, context) => {
  const { userId, newPassword, adminOpenId } = event

  // 验证参数
  if (!userId || !newPassword) {
    return {
      success: false,
      message: '缺少必要参数'
    }
  }

  if (newPassword.length < 6) {
    return {
      success: false,
      message: '密码长度不能少于6位'
    }
  }

  try {
    // 1. 验证管理员权限
    const adminResult = await db.collection('users').where({
      openid: adminOpenId,
      role: 'admin'
    }).limit(1).get()

    if (!adminResult.data || adminResult.data.length === 0) {
      return {
        success: false,
        message: '无权限操作'
      }
    }

    // 2. 获取目标用户
    const userResult = await db.collection('users').doc(userId).get()
    if (!userResult.data) {
      return {
        success: false,
        message: '用户不存在'
      }
    }

    // 3. 生成新密码
    const { password_hash, password_salt } = buildPasswordFields(newPassword)

    // 4. 更新用户密码 - 使用云函数权限可以绕过客户端限制
    await db.collection('users').doc(userId).update({
      data: {
        password_hash,
        password_salt,
        password_reset_method: 'admin_cloud_function_reset',
        password_updated_at: Date.now(),
        updated_at: Date.now()
      }
    })

    // 5. 记录操作日志
    await db.collection('admin_logs').add({
      data: {
        admin_id: adminOpenId,
        target_type: 'user',
        target_id: userId,
        action: 'reset_password',
        created_at: Date.now()
      }
    })

    return {
      success: true,
      message: '密码重置成功',
      data: {
        user_id: userId,
        new_password: newPassword
      }
    }

  } catch (error) {
    console.error('重置密码失败:', error)
    return {
      success: false,
      message: '重置密码失败: ' + error.message
    }
  }
}
