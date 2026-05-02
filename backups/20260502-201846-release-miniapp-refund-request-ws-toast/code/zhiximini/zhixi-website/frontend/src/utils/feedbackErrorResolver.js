import { FEEDBACK_AUTH_ERROR_CODE } from "../constants/feedbackErrorCodes";
import { FEEDBACK_TEXT } from "../constants/feedbackMessages";

const AUTH_ERROR_MATCHERS = [
  {
    keyword: "验证码错误或已过期",
    code: FEEDBACK_AUTH_ERROR_CODE.SMS_INVALID_OR_EXPIRED,
    message: FEEDBACK_TEXT.auth.codeInvalidOrExpiredServer
  },
  {
    keyword: "手机号格式不正确",
    code: FEEDBACK_AUTH_ERROR_CODE.PHONE_INVALID,
    message: FEEDBACK_TEXT.auth.phoneInvalidServer
  },
  {
    keyword: "手机号不能为空",
    code: FEEDBACK_AUTH_ERROR_CODE.PHONE_REQUIRED,
    message: FEEDBACK_TEXT.auth.phoneRequiredServer
  },
  {
    keyword: "验证码不能为空",
    code: FEEDBACK_AUTH_ERROR_CODE.CODE_REQUIRED,
    message: FEEDBACK_TEXT.auth.codeRequiredServer
  },
  {
    keyword: "登录状态已过期",
    code: FEEDBACK_AUTH_ERROR_CODE.LOGIN_EXPIRED,
    message: FEEDBACK_TEXT.auth.loginExpired
  },
  {
    keyword: "请求频繁",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.requestTooFrequent
  },
  {
    keyword: "短信发送过于频繁",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.requestTooFrequent
  },
  {
    keyword: "短信发送次数已达上限",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.smsDailyLimitExceeded
  },
  {
    keyword: "every day exceeds the upper limit",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.smsDailyLimitExceeded
  },
  {
    keyword: "daily limit",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.smsDailyLimitExceeded
  },
  {
    keyword: "too frequent",
    code: FEEDBACK_AUTH_ERROR_CODE.TOO_FREQUENT,
    message: FEEDBACK_TEXT.auth.requestTooFrequent
  },
  {
    keyword: "密码错误",
    code: FEEDBACK_AUTH_ERROR_CODE.PASSWORD_WRONG,
    message: FEEDBACK_TEXT.auth.passwordWrongServer
  },
  {
    keyword: "账号不存在",
    code: FEEDBACK_AUTH_ERROR_CODE.ACCOUNT_NOT_EXIST,
    message: FEEDBACK_TEXT.auth.accountNotExistServer
  },
  {
    keyword: "该手机号已注册",
    code: FEEDBACK_AUTH_ERROR_CODE.PHONE_ALREADY_REGISTERED,
    message: FEEDBACK_TEXT.auth.phoneAlreadyRegistered
  },
  {
    keyword: "图片验证码错误",
    code: FEEDBACK_AUTH_ERROR_CODE.CAPTCHA_INVALID,
    message: FEEDBACK_TEXT.auth.captchaInvalidServer
  },
  {
    keyword: "图片验证码已过期",
    code: FEEDBACK_AUTH_ERROR_CODE.CAPTCHA_EXPIRED,
    message: FEEDBACK_TEXT.auth.captchaExpiredServer
  },
  {
    keyword: "密码长度",
    code: FEEDBACK_AUTH_ERROR_CODE.PASSWORD_TOO_SHORT,
    message: FEEDBACK_TEXT.auth.passwordTooShort
  }
];

export function resolveAuthErrorByMessage(rawMessage) {
  const source = (rawMessage || "").trim();
  const matched = AUTH_ERROR_MATCHERS.find((item) => source.includes(item.keyword));
  if (matched) {
    return { code: matched.code, message: matched.message };
  }
  return {
    code: FEEDBACK_AUTH_ERROR_CODE.UNKNOWN,
    message: source || FEEDBACK_TEXT.auth.unknownError
  };
}
