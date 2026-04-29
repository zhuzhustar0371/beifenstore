export const FEEDBACK_TEXT = {
  auth: {
    title: "登录提示",
    invalidPhone: "请输入正确的 11 位手机号",
    invalidCode: "请输入有效验证码",
    invalidPassword: "请输入密码（至少 6 位）",
    invalidCaptcha: "请输入图片验证码",
    sendCodeSuccess: "验证码已发送，请注意查收",
    sendCodeFirst: '验证码未发送，请先点击"发送验证码"再继续',
    codeExpired: "验证码已过期，请重新发送验证码",
    tooManyAttempts: "验证码输入错误次数过多，请重新获取验证码后再试",
    loginSuccess: "登录成功，已同步账号状态",
    registerSuccess: "注册成功，已自动登录",
    loginExpired: "登录状态已过期，请重新登录",
    requestTooFrequent: "操作过于频繁，请稍后再试",
    smsDailyLimitExceeded: "该手机号今日短信发送次数已达上限，请明天再试",
    unknownError: "操作失败，请稍后重试",
    phoneInvalidServer: "手机号格式不正确，请输入 11 位手机号",
    phoneRequiredServer: "手机号不能为空，请先输入手机号",
    codeRequiredServer: "验证码不能为空，请输入收到的验证码",
    codeInvalidOrExpiredServer: "验证码错误或已过期，请重新获取后再试",
    passwordWrongServer: "密码错误，请重新输入",
    accountNotExistServer: "账号不存在，请先注册",
    phoneAlreadyRegistered: "该手机号已注册，请直接登录",
    captchaInvalidServer: "图片验证码错误，请重新输入",
    captchaExpiredServer: "图片验证码已过期，请刷新重试",
    passwordTooShort: "密码长度需在 6-32 位之间",
    codeRetryHint: (remaining) => `验证码不正确或已失效，请重试（剩余 ${remaining} 次）`
  },
  order: {
    title: "订单提示",
    productLoadFail: "商品加载失败，请稍后重试",
    productSelect: (productId) => `已选择商品 #${productId}，请填写收货信息`,
    invalidProduct: "请选择正确的商品 ID",
    invalidQuantity: "购买数量至少为 1",
    recipientRequired: "请填写收货人姓名",
    recipientPhoneInvalid: "请填写正确的收货手机号",
    addressRequired: "请填写收货地址",
    createSuccess: "订单创建成功，请完成支付",
    createFail: "订单创建失败，请稍后重试",
    paySuccess: "支付成功，返现将按规则自动结算",
    payFail: "支付失败，请稍后重试"
  },
  user: {
    title: "状态提示",
    refreshSuccess: "数据已更新",
    refreshFail: "数据加载失败，请稍后重试",
    needLogin: "尚未登录，请先回首页登录"
  },
  admin: {
    title: "看板提示",
    refreshSuccess: "看板数据已刷新",
    refreshFail: "看板数据加载失败，请稍后重试"
  },
  rule: {
    title: "规则提示",
    notice: "返现以系统订单支付记录自动结算，若有疑问请在用户中心核对返现明细。"
  }
};
