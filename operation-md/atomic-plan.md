# 阶段 1：backend-api 邀请进度接口改造

## 备份时间
2026-05-03

## 备份基线
| 子仓 | HEAD commit | 说明 |
|------|-------------|------|
| backend-api | 7796a00 | fix: use production order status columns |
| wechat-app | eb1ed03 | fix: retry wechat confirm receive sync |
| zhixi-website | b330a4e | fix: improve admin light theme button contrast |

## 历史遗留脏工作区（非本次任务改动）
- backend-api: 37 个 modified 文件 + 6 个 untracked 文件（含 .bak 文件），均为历史改造遗留
- wechat-app: 44 个 modified 文件 + 6 �� untracked 文件（含性能分析和 changelog）
- zhixi-website: 11 个 modified 文件 + 7 个 untracked 文件（含审计日志页等新功能）

## 本次原子化待执行事项
1. 新建 `InviteProductProgressItem.java` — 商品级首单进度 DTO
2. 新建 `MiniappInviteProgressVO.java` — ���程序邀请进度 VO
3. 新建 `AdminInviteProgressVO.java` — 管理端邀请进�� VO
4. `InviteProductRelationMapper.java` 新增 `findByInviterAndInvitee` ���法
5. `InviteService.java` 注入新依赖 + 新增 `listMiniappProgressByInviter`
6. `AdminManageService.java` 新增 `pageInviteProgress`
7. `InviteController.java` 新增 `GET /api/invites/me/progress`
8. `AdminController.java` 新增 `GET /api/admin/invites/progress`

## 风险
- N+1 查询（每个被邀请人一次 `findByInviterAndInvitee`），小规模可接受
- 新端点仅新增，不破坏旧接口

## 验收标准
- `mvn compile` 通过
- 新端点返回正确 JSON 结构
- 旧端点行为不变

## 回退方案
- 还原本地备份 `G:\store\20260503-phase1-invite-progress-alignment\code\zhiximini`
- 或还原远端备份分支 `backup/20260503-phase1-invite-progress-alignment`
