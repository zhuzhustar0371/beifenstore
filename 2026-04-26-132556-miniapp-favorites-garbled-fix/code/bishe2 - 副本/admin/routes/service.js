/**
 * Customer-service dashboard routes.
 */

const express = require("express");
const multer = require("multer");
const path = require("path");
const fs = require("fs");

const { db } = require("../../cloudbase");
const { listCollection, normalizeDoc } = require("../../init-db");
const { webSocketHub } = require("../websocket");

const router = express.Router();

const privateChatUploadDir = path.join(__dirname, "../public/uploads/service-chat");
if (!fs.existsSync(privateChatUploadDir)) {
  fs.mkdirSync(privateChatUploadDir, { recursive: true });
}

const privateChatStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, privateChatUploadDir);
  },
  filename: (req, file, cb) => {
    const uniqueSuffix = Date.now() + "-" + Math.round(Math.random() * 1e9);
    const ext = path.extname(file.originalname).toLowerCase();
    cb(null, "sc-" + uniqueSuffix + ext);
  },
});

const privateChatUpload = multer({
  storage: privateChatStorage,
  limits: { fileSize: 5 * 1024 * 1024 },
  fileFilter: (req, file, cb) => {
    const allowedTypes = ["image/jpeg", "image/jpg", "image/pjpeg", "image/png", "image/webp"];
    if (allowedTypes.includes(file.mimetype)) {
      cb(null, true);
    } else {
      cb(new Error("仅支持 JPG、PNG、WEBP 格式的图片"), false);
    }
  },
});

function getDocId(doc) {
  return doc?.id || doc?._id || "";
}

function getOpenId(doc) {
  return doc?.openid || doc?.open_id || "";
}

function getDisplayName(user, fallback = "未知用户") {
  return user?.nickname || user?.username || user?.account || getOpenId(user) || fallback;
}

function toTimestamp(value) {
  if (value instanceof Date) {
    return value.getTime();
  }

  const numeric = Number(value || 0);
  if (Number.isFinite(numeric) && numeric > 0) {
    return numeric;
  }

  const parsed = Date.parse(String(value || ""));
  if (Number.isFinite(parsed)) {
    return parsed;
  }

  return 0;
}

function createId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

function buildUserMaps(users) {
  const byId = new Map();
  const byOpenId = new Map();

  users.forEach((user) => {
    const docId = getDocId(user);
    const openid = getOpenId(user);

    if (docId) {
      byId.set(docId, user);
    }

    if (openid) {
      byOpenId.set(openid, user);
    }
  });

  return { byId, byOpenId };
}

function resolveUser(key, userMaps) {
  if (!key) {
    return null;
  }

  return userMaps.byId.get(key) || userMaps.byOpenId.get(key) || null;
}

function normalizeConversationServiceStatus(conversation) {
  return String(conversation?.service_status || "").trim() || "open";
}

function normalizeListingStatus(listing) {
  const raw = String(listing?.status || "").trim();
  if (raw === "removed") {
    return "off_shelf";
  }
  return raw || "pending_review";
}

function normalizeSingleDoc(input) {
  if (Array.isArray(input)) {
    if (!input.length) {
      return null;
    }
    const first = input[0];
    if (!first || typeof first !== "object") {
      return null;
    }
    return normalizeDoc(first);
  }

  if (!input || typeof input !== "object") {
    return null;
  }

  return normalizeDoc(input);
}

function buildConversationViewModel(conversation, userMaps, listingMap, districtMap) {
  const listing = listingMap.get(conversation.listing_id);
  const buyer = resolveUser(conversation.buyer_openid, userMaps);
  const seller = resolveUser(conversation.seller_openid, userMaps);
  const district = districtMap.get(listing?.district_code);
  const serviceStatus = normalizeConversationServiceStatus(conversation);
  const lastActiveAt = toTimestamp(conversation.updated_at || conversation.created_at);

  return {
    ...conversation,
    id: getDocId(conversation),
    route_id: conversation?._id || getDocId(conversation),
    service_status: serviceStatus,
    listing,
    listing_title: listing?.title || "商品已删除",
    listing_type: listing?.listing_type || "sale",
    listing_status: normalizeListingStatus(listing),
    listing_image: Array.isArray(listing?.image_urls) ? listing.image_urls[0] || "" : "",
    listing_price: Number(listing?.price || 0),
    district_name: district?.name || listing?.district_name || listing?.district_code || "-",
    buyer,
    buyer_name: getDisplayName(buyer, "买家"),
    seller,
    seller_name: getDisplayName(seller, "卖家"),
    last_message_preview: conversation.last_message || "还没有消息",
    last_active_at: lastActiveAt,
    last_active_label: new Date(lastActiveAt || Date.now()).toLocaleString("zh-CN"),
    is_processed: serviceStatus === "processed",
  };
}

async function recordServiceAction(req, targetType, targetId, action, note = "") {
  try {
    await db.collection("admin_actions").add({
      admin_user_id: req.session.user.id,
      target_type: targetType,
      target_id: targetId,
      action,
      action_note: note,
      created_at: Date.now(),
    });
  } catch (error) {
    console.error("Failed to record service action:", error.message);
  }
}

async function loadServiceCollections() {
  const [users, listings, conversations, messages, feedbacks, districts] = await Promise.all([
    listCollection("users", 400),
    listCollection("listings", 400),
    listCollection("conversations", 500),
    listCollection("messages", 1000),
    listCollection("feedback", 300),
    listCollection("districts", 400),
  ]);

  return {
    users,
    userMaps: buildUserMaps(users),
    listingMap: new Map(listings.map((listing) => [getDocId(listing), listing])),
    districtMap: new Map(districts.map((item) => [item.code, item])),
    conversations,
    messages,
    feedbacks,
  };
}

async function findConversationByAnyId(value) {
  const normalized = String(value || "").trim();
  if (!normalized) {
    return null;
  }

  try {
    // 优先使用 _id 直接查询（最可靠）
    const raw = await db.collection("conversations").doc(normalized).get();
    const parsed = normalizeSingleDoc(raw?.data);
    if (parsed) {
      return parsed;
    }
  } catch (error) {
    console.log("[findConversationByAnyId] doc lookup failed:", error.message);
  }

  try {
    // 其次按 id 字段查询
    const result = await db.collection("conversations").where({ id: normalized }).limit(1).get();
    const matched = normalizeSingleDoc(result?.data);
    if (matched) {
      return matched;
    }
  } catch (error) {
    console.log("[findConversationByAnyId] where lookup failed:", error.message);
  }

  // 最后遍历所有对话
  const conversations = await listCollection("conversations", 500);
  return (
    conversations.find((item) => {
      const ids = [item._id, item.id, getDocId(item)].filter(Boolean).map((entry) => String(entry));
      return ids.includes(normalized);
    }) || null
  );
}

async function listConversationMessages(conversation) {
  const ids = Array.from(
    new Set([conversation?._id, conversation?.id, getDocId(conversation)].filter(Boolean)),
  );
  if (!ids.length) {
    return [];
  }

  const command = db.command;
  const result = await db
    .collection("messages")
    .where({ conversation_id: command.in(ids) })
    .limit(500)
    .get();

  return (result.data || [])
    .map(normalizeDoc)
    .sort((left, right) => toTimestamp(left.created_at) - toTimestamp(right.created_at));
}

function buildMessageViewModel(message, conversationVm, userMaps, currentUser) {
  const senderOpenid = String(message?.sender_openid || "").trim();
  const sender = resolveUser(senderOpenid, userMaps);
  const currentOpenid = String(currentUser?.openid || "").trim();
  const isServiceSender =
    senderOpenid === currentOpenid ||
    senderOpenid === conversationVm.service_sender_openid ||
    senderOpenid === `service-${currentUser?.id || ""}`;

  return {
    ...message,
    id: getDocId(message),
    sender_openid: senderOpenid,
    sender_name: isServiceSender
      ? currentUser?.nickname || "客服"
      : senderOpenid === conversationVm.buyer_openid
        ? conversationVm.buyer_name
        : senderOpenid === conversationVm.seller_openid
          ? conversationVm.seller_name
          : getDisplayName(sender, "平台消息"),
    is_service_sender: isServiceSender,
    is_image:
      String(message?.message_type || "text") === "image" &&
      Boolean(String(message?.image_url || message?.content || "").trim()),
    image_url: String(message?.image_url || message?.content || "").trim(),
  };
}

router.get("/dashboard", async (req, res) => {
  try {
    const currentServiceStatus = String(req.query.service_status || "all").trim() || "all";
    const currentListingStatus = String(req.query.listing_status || "all").trim() || "all";
    const keyword = String(req.query.keyword || "").trim().toLowerCase();
    const { userMaps, listingMap, districtMap, conversations, messages, feedbacks } =
      await loadServiceCollections();

    const recentConversations = conversations
      .map((conversation) => buildConversationViewModel(conversation, userMaps, listingMap, districtMap))
      .filter((conversation) => {
        if (currentServiceStatus !== "all") {
          const expectedStatus = currentServiceStatus === "processed" ? "processed" : "open";
          if (conversation.service_status !== expectedStatus) {
            return false;
          }
        }

        if (currentListingStatus !== "all" && conversation.listing_status !== currentListingStatus) {
          return false;
        }

        if (!keyword) {
          return true;
        }

        const haystack = [
          conversation.listing_title,
          conversation.buyer_name,
          conversation.seller_name,
          conversation.buyer_openid,
          conversation.seller_openid,
          conversation.last_message_preview,
          conversation.district_name,
        ]
          .filter(Boolean)
          .join(" ")
          .toLowerCase();

        return haystack.includes(keyword);
      })
      .sort((left, right) => toTimestamp(right.last_active_at) - toTimestamp(left.last_active_at));

    const pendingFeedbacks = feedbacks
      .map((feedback) => ({
        ...feedback,
        user: resolveUser(feedback.user_id || feedback.openid || feedback.open_id, userMaps),
      }))
      .filter((feedback) => feedback.status !== "closed")
      .sort((left, right) => toTimestamp(right.created_at) - toTimestamp(left.created_at))
      .slice(0, 10);

    const stats = {
      conversationCount: recentConversations.length,
      messageCount: messages.length,
      processingConversationCount: recentConversations.filter((item) => !item.is_processed).length,
      pendingFeedbackCount: feedbacks.filter((item) => item.status !== "closed").length,
    };

    // 加载客服的私聊对话
    let serviceConversations = [];
    try {
      const scResult = await db.collection("service_conversations")
        .where({ service_user_id: req.session.user.id })
        .orderBy("updated_at", "desc")
        .limit(50)
        .get();

      serviceConversations = (scResult.data || []).map(normalizeDoc);

      for (const sc of serviceConversations) {
        if (sc.participant_openid && !sc.participant_nickname) {
          try {
            const userResult = await db.collection("users")
              .where({ openid: sc.participant_openid })
              .limit(1)
              .get();
            if (userResult.data && userResult.data.length > 0) {
              sc.participant_nickname = normalizeDoc(userResult.data[0]).nickname || sc.participant_name;
            }
          } catch (e) { /* ignore */ }
        }
        if (sc.listing_id && !sc.listing_title) {
          try {
            const listingResult = await db.collection("listings")
              .where({ id: sc.listing_id })
              .limit(1)
              .get();
            if (listingResult.data && listingResult.data.length > 0) {
              sc.listing_title = normalizeDoc(listingResult.data[0]).title || "未知商品";
            }
          } catch (e) { /* ignore */ }
        }
      }

      console.log("[Dashboard] Loaded service conversations:", serviceConversations.length);
    } catch (error) {
      console.error("[Dashboard] Failed to load service conversations:", error.message);
    }

    res.render("service/dashboard", {
      title: "客服工作台",
      stats,
      recentConversations,
      serviceConversations,
      pendingFeedbacks,
      currentServiceStatus,
      currentListingStatus,
      keyword,
      serviceStatusOptions: [
        { value: "all", label: "全部对话" },
        { value: "open", label: "未处理" },
        { value: "processed", label: "已处理" },
      ],
      listingStatusOptions: [
        { value: "all", label: "全部商品状态" },
        { value: "approved", label: "未下架" },
        { value: "off_shelf", label: "已下架" },
        { value: "pending_review", label: "待审核" },
      ],
    });
  } catch (error) {
    console.error("Failed to load service dashboard:", error);
    req.flash("error", "客服工作台加载失败。");
    res.redirect("/");
  }
});

router.get("/conversations/:id", async (req, res) => {
  try {
    const conversation = await findConversationByAnyId(req.params.id);
    if (!conversation) {
      req.flash("error", "会话不存在。");
      return res.redirect("/service/dashboard");
    }

    const { users, userMaps, listingMap, districtMap } = await loadServiceCollections();
    const conversationVm = buildConversationViewModel(conversation, userMaps, listingMap, districtMap);
    const messages = await listConversationMessages(conversation);
    const messageItems = messages.map((message) =>
      buildMessageViewModel(message, conversationVm, userMaps, req.session.user),
    );

    res.render("service/conversation", {
      title: "客服对话处理",
      conversation: conversationVm,
      messages: messageItems,
      currentServiceUser: req.session.user,
      userCount: users.length,
    });
  } catch (error) {
    console.error("Failed to load service conversation:", error);
    req.flash("error", "会话详情加载失败。");
    res.redirect("/service/dashboard");
  }
});

router.post("/conversations/:id/messages", async (req, res) => {
  try {
    const conversation = await findConversationByAnyId(req.params.id);
    if (!conversation) {
      req.flash("error", "会话不存在。");
      return res.redirect("/service/dashboard");
    }

    const content = String(req.body.content || "").trim();
    if (!content) {
      req.flash("error", "回复内容不能为空。");
      return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
    }

    const now = Date.now();
    const senderOpenid = String(req.session.user?.openid || "").trim() || `service-${req.session.user?.id || "staff"}`;
    const payload = {
      id: createId("message"),
      conversation_id: getDocId(conversation),
      sender_openid: senderOpenid,
      sender_type: "service",
      sender_name: req.session.user?.nickname || "客服",
      content,
      message_type: "text",
      status: "sent",
      created_at: now,
    };

    const addResult = await db.collection("messages").add(payload);
    await db.collection("conversations").doc(conversation._id).update({
      last_message: content,
      unread_count: Number(conversation.unread_count || 0) + 1,
      updated_at: now,
      service_status: "open",
      service_sender_openid: senderOpenid,
      service_updated_at: now,
    });

    webSocketHub.notifyConversationParticipants(conversation, {
      type: "message:new",
      data: {
        conversation_id: getDocId(conversation),
        message: {
          ...payload,
          id: addResult.id,
        },
      },
    });

    await recordServiceAction(
      req,
      "conversation",
      getDocId(conversation),
      "service_reply_message",
      content.slice(0, 80),
    );
    req.flash("success", "客服回复已发送。");
    return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
  } catch (error) {
    console.error("Failed to send service message:", error);
    req.flash("error", "发送客服回复失败。");
    return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
  }
});

router.post("/conversations/:id/processed", async (req, res) => {
  try {
    const conversation = await findConversationByAnyId(req.params.id);
    if (!conversation) {
      req.flash("error", "会话不存在。");
      return res.redirect("/service/dashboard");
    }

    const nextStatus =
      normalizeConversationServiceStatus(conversation) === "processed" ? "open" : "processed";
    const now = Date.now();

    await db.collection("conversations").doc(conversation._id).update({
      service_status: nextStatus,
      service_processed_at: nextStatus === "processed" ? now : 0,
      service_processed_by: req.session.user?.id || "",
      service_updated_at: now,
    });

    await recordServiceAction(
      req,
      "conversation",
      getDocId(conversation),
      "service_mark_processed",
      nextStatus,
    );
    req.flash("success", nextStatus === "processed" ? "该对话已标记为已处理。" : "该对话已恢复为处理中。");

    const backTo = String(req.body.back_to || "").trim();
    if (backTo === "detail") {
      return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
    }
    return res.redirect("/service/dashboard");
  } catch (error) {
    console.error("Failed to update service conversation status:", error);
    req.flash("error", "更新处理状态失败。");
    return res.redirect("/service/dashboard");
  }
});

router.post("/conversations/:id/refund", async (req, res) => {
  try {
    const conversation = await findConversationByAnyId(req.params.id);
    if (!conversation) {
      req.flash("error", "浼氳瘽涓嶅瓨鍦ㄣ€?");
      return res.redirect("/service/dashboard");
    }

    await recordServiceAction(
      req,
      "conversation",
      getDocId(conversation),
      "service_refund_reserved",
      "refund_placeholder",
    );
    req.flash("success", "已预留退款接口按钮，当前先记录动作，后续可直接接入真实退款流程。");

    const backTo = String(req.body.back_to || "").trim();
    if (backTo === "detail") {
      return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
    }
    return res.redirect("/service/dashboard");
  } catch (error) {
    console.error("Failed to trigger reserved refund action:", error);
    req.flash("error", "退款预留接口触发失败。");
    return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
  }
});

router.post("/conversations/:id/reject", async (req, res) => {
  try {
    const conversation = await findConversationByAnyId(req.params.id);
    if (!conversation) {
      req.flash("error", "浼氳瘽涓嶅瓨鍦ㄣ€?");
      return res.redirect("/service/dashboard");
    }

    await recordServiceAction(
      req,
      "conversation",
      getDocId(conversation),
      "service_reject_reserved",
      "reject_placeholder",
    );
    req.flash("success", "已预留不批准接口按钮，当前先记录动作，后续可直接接入真实不批准流程。");

    const backTo = String(req.body.back_to || "").trim();
    if (backTo === "detail") {
      return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
    }
    return res.redirect("/service/dashboard");
  } catch (error) {
    console.error("Failed to trigger reserved reject action:", error);
    req.flash("error", "不批准预留接口触发失败。");
    return res.redirect(`/service/conversations/${encodeURIComponent(req.params.id)}`);
  }
});

router.get("/feedback/:id", async (req, res) => {
  try {
    const feedbackId = req.params.id;
    const [feedbacks, users] = await Promise.all([listCollection("feedback", 200), listCollection("users", 400)]);
    const feedback = feedbacks.find((item) => getDocId(item) === feedbackId);

    if (!feedback) {
      req.flash("error", "反馈不存在。");
      return res.redirect("/service/dashboard");
    }

    const userMaps = buildUserMaps(users);
    const user = resolveUser(feedback.user_id || feedback.openid || feedback.open_id, userMaps);

    res.render("feedback/show", {
      title: "反馈详情",
      feedback,
      user,
      backPath: "/service/dashboard",
      statusAction: `/service/feedback/${feedbackId}/status`,
    });
  } catch (error) {
    console.error("Failed to load service feedback detail:", error);
    req.flash("error", "反馈详情加载失败。");
    res.redirect("/service/dashboard");
  }
});

router.post("/feedback/:id/status", async (req, res) => {
  const feedbackId = req.params.id;
  const status = String(req.body.status || "").trim();

  if (!["new", "processing", "closed"].includes(status)) {
    req.flash("error", "状态值无效。");
    return res.redirect(`/service/feedback/${feedbackId}`);
  }

  try {
    await db.collection("feedback").doc(feedbackId).update({
      status,
      updated_at: Date.now(),
    });

    await recordServiceAction(req, "feedback", feedbackId, "service_update_status", status);
    req.flash("success", "反馈状态已更新。");
    res.redirect(`/service/feedback/${feedbackId}`);
  } catch (error) {
    console.error("Failed to update feedback in service web:", error);
    req.flash("error", "状态更新失败。");
    res.redirect(`/service/feedback/${feedbackId}`);
  }
});

// 创建或查看客服与买家/卖家的独立对话
router.get("/conversations/:id/chat", async (req, res) => {
  try {
    const conversationId = req.params.id;
    console.log("[PrivateChat] Loading chat for conversation:", conversationId);
    
    const conversation = await findConversationByAnyId(conversationId);
    if (!conversation) {
      console.error("[PrivateChat] Conversation not found:", conversationId);
      req.flash("error", "会话不存在。");
      return res.redirect("/service/dashboard");
    }

    console.log("[PrivateChat] Found conversation:", getDocId(conversation));

    const chatType = req.query.type === "seller" ? "seller" : "buyer";
    const participantOpenid = chatType === "seller" ? conversation.seller_openid : conversation.buyer_openid;
    const participantName = chatType === "seller" ? "卖家" : "买家";
    
    const serviceUserId = req.session.user.id;
    const originalConversationId = getDocId(conversation);

    console.log("[PrivateChat] Chat type:", chatType, "Participant:", participantOpenid);

    // 查找是否已存在独立对话
    let existingResult;
    try {
      existingResult = await db.collection("service_conversations")
        .where({
          original_conversation_id: originalConversationId,
          service_user_id: serviceUserId,
          participant_type: chatType,
        })
        .limit(1)
        .get();
    } catch (dbError) {
      console.error("[PrivateChat] Database query failed:", dbError.message);
      req.flash("error", "数据库查询失败，请检查集合是否存在。");
      return res.redirect("/service/dashboard");
    }

    let serviceConversation;
    if (existingResult.data && existingResult.data.length > 0) {
      serviceConversation = normalizeDoc(existingResult.data[0]);
      console.log("[PrivateChat] Found existing service conversation:", serviceConversation.id);
    } else {
      // 创建新的独立对话
      const now = Date.now();
      const newConversation = {
        id: createId("sc"),
        original_conversation_id: originalConversationId,
        service_user_id: serviceUserId,
        service_user_name: req.session.user.nickname || "客服",
        participant_openid: participantOpenid,
        participant_type: chatType,
        participant_name: participantName,
        listing_id: conversation.listing_id,
        status: "open",
        created_at: now,
        updated_at: now,
      };
      
      try {
        const addResult = await db.collection("service_conversations").add(newConversation);
        serviceConversation = { ...newConversation, _id: addResult.id };
        console.log("[PrivateChat] Created new service conversation:", serviceConversation.id);
      } catch (createError) {
        console.error("[PrivateChat] Failed to create conversation:", createError.message);
        req.flash("error", "创建私聊对话失败。");
        return res.redirect("/service/dashboard");
      }
    }

    // 获取独立对话的消息
    let messages = [];
    try {
      const messagesResult = await db.collection("service_messages")
        .where({ service_conversation_id: serviceConversation.id })
        .orderBy("created_at", "asc")
        .limit(200)
        .get();
      
      messages = (messagesResult.data || []).map(normalizeDoc);
      console.log("[PrivateChat] Loaded messages:", messages.length);
    } catch (msgError) {
      console.error("[PrivateChat] Failed to load messages:", msgError.message);
      // 不中断，继续渲染页面
    }

    res.render("service/private-chat", {
      title: `与${participantName}的私聊`,
      serviceConversation,
      messages,
      chatType,
      participantName,
      currentUser: req.session.user,
    });
  } catch (error) {
    console.error("[PrivateChat] Failed to load private chat:", error);
    req.flash("error", "私聊页面加载失败：" + error.message);
    res.redirect("/service/dashboard");
  }
});

// 发送独立对话消息
router.post("/conversations/:id/chat/messages", (req, res) => {
  const messageType = req.body.message_type || "text";

  if (messageType === "image") {
    const uploadSingle = privateChatUpload.single("image");
    uploadSingle(req, res, async (err) => {
      if (err) {
        if (err instanceof multer.MulterError) {
          if (err.code === "LIMIT_FILE_SIZE") {
            req.flash("error", "图片文件大小不能超过 5MB");
          } else {
            req.flash("error", "图片上传失败：" + err.message);
          }
        } else {
          req.flash("error", err.message || "图片上传失败");
        }
        return res.redirect("back");
      }

      if (!req.file) {
        req.flash("error", "请选择图片文件。");
        return res.redirect("back");
      }

      try {
        const serviceConversationId = req.body.service_conversation_id;
        const chatType = req.body.chat_type || "buyer";

        if (!serviceConversationId) {
          fs.unlinkSync(req.file.path);
          req.flash("error", "缺少对话ID。");
          return res.redirect("back");
        }

        const imageUrl = `/uploads/service-chat/${req.file.filename}`;
        const now = Date.now();

        const message = {
          id: createId("sm"),
          service_conversation_id: serviceConversationId,
          sender_type: "service",
          sender_id: req.session.user.id,
          sender_name: req.session.user.nickname || "客服",
          content: "[图片]",
          message_type: "image",
          image_url: imageUrl,
          created_at: now,
        };

        await db.collection("service_messages").add(message);
        console.log("[PrivateChat] Image message saved:", message.id);

        let participantOpenid = null;
        try {
          const scResult = await db.collection("service_conversations")
            .where({ id: serviceConversationId })
            .limit(1)
            .get();

          if (scResult.data && scResult.data.length > 0) {
            const scDoc = normalizeDoc(scResult.data[0]);
            participantOpenid = scDoc.participant_openid;
            await db.collection("service_conversations").doc(scDoc._id).update({
              last_message: "[图片]",
              updated_at: now,
            });
          }
        } catch (updateError) {
          console.error("[PrivateChat] Failed to update conversation:", updateError.message);
        }

        if (participantOpenid) {
          webSocketHub.sendToOpenid(participantOpenid, {
            type: "service:message:new",
            data: {
              service_conversation_id: serviceConversationId,
              message: {
                ...message,
                sender_type: "service",
                sender_name: req.session.user.nickname || "客服",
              },
            },
          });
        }

        req.flash("success", "图片已发送。");
        return res.redirect(`/service/conversations/${req.params.id}/chat?type=${chatType}`);
      } catch (error) {
        console.error("[PrivateChat] Failed to send image message:", error);
        if (fs.existsSync(req.file.path)) {
          try { fs.unlinkSync(req.file.path); } catch (e) { /* ignore */ }
        }
        req.flash("error", "发送图片失败：" + error.message);
        return res.redirect("back");
      }
    });
  } else {
    (async () => {
      try {
        const serviceConversationId = req.body.service_conversation_id;
        const content = String(req.body.content || "").trim();
        const chatType = req.body.chat_type || "buyer";

        if (!content || !serviceConversationId) {
          req.flash("error", "消息内容不能为空。");
          return res.redirect("back");
        }

        const now = Date.now();
        const message = {
          id: createId("sm"),
          service_conversation_id: serviceConversationId,
          sender_type: "service",
          sender_id: req.session.user.id,
          sender_name: req.session.user.nickname || "客服",
          content,
          message_type: "text",
          created_at: now,
        };

        await db.collection("service_messages").add(message);
        console.log("[PrivateChat] Message saved:", message.id);

        let participantOpenid = null;
        try {
          const scResult = await db.collection("service_conversations")
            .where({ id: serviceConversationId })
            .limit(1)
            .get();

          if (scResult.data && scResult.data.length > 0) {
            const scDoc = normalizeDoc(scResult.data[0]);
            participantOpenid = scDoc.participant_openid;
            await db.collection("service_conversations").doc(scDoc._id).update({
              last_message: content,
              updated_at: now,
            });
            console.log("[PrivateChat] Conversation updated:", serviceConversationId);
          }
        } catch (updateError) {
          console.error("[PrivateChat] Failed to update conversation:", updateError.message);
        }

        if (participantOpenid) {
          webSocketHub.sendToOpenid(participantOpenid, {
            type: "service:message:new",
            data: {
              service_conversation_id: serviceConversationId,
              message: {
                ...message,
                sender_type: "service",
                sender_name: req.session.user.nickname || "客服",
              },
            },
          });
          console.log("[PrivateChat] WebSocket notification sent to:", participantOpenid);
        }

        req.flash("success", "消息已发送。");
        return res.redirect(`/service/conversations/${req.params.id}/chat?type=${chatType}`);
      } catch (error) {
        console.error("[PrivateChat] Failed to send private message:", error);
        req.flash("error", "发送消息失败：" + error.message);
        return res.redirect("back");
      }
    })();
  }
});

module.exports = router;
