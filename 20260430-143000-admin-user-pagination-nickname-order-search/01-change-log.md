# Change Log: admin-user-pagination-nickname-order-search

## Time
- Started: 2026-04-30 14:30 Asia/Shanghai

## Request
- Re-analyze user management pagination and nickname search.
- Implement after approval.
- Follow backup, local modification, build, release/rollback documentation workflow.

## Initial Findings
- Root `G:\zhiximini` is not a Git repository.
- Backend repository is under `G:\zhiximini\backend-api`.
- Website repository is under `G:\zhiximini\zhixi-website`.
- Existing uncommitted changes were present before this task and must be preserved.

## Planned Files
- `backend-api/src/main/java/com/zhixi/backend/mapper/UserMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `zhixi-website/admin-frontend/src/api.js`
- `zhixi-website/admin-frontend/src/views/UsersPage.vue`
- `zhixi-website/admin-frontend/src/views/OrdersPage.vue`

## Operation Log
- 2026-04-30 14:30 Created local backup directory structure.
- 2026-04-30 14:30 Created atomic operation document.
- 2026-04-30 14:31 Completed local source backup under `code/`.
- 2026-04-30 14:35 Initial cloud backup clone timed out and left an incomplete checkout.
- 2026-04-30 14:49 Recloned `beifenstore`; Windows long path checkout issue was fixed in the temporary staging repository with `core.longpaths` and reset to `HEAD`.
- 2026-04-30 14:50 Pushed cloud backup to `beifenstore`, commit `3636107`.
- 2026-04-30 14:51 Updated backend admin user search to include WeChat auth nickname/openid.
- 2026-04-30 14:52 Updated backend admin order search and CSV export to include recipient, user mobile, user nickname, WeChat auth nickname/openid.
- 2026-04-30 14:53 Updated admin frontend API user fetch to preserve pagination metadata.
- 2026-04-30 14:55 Added user page search, status filter, pagination, jump page, and user-to-orders navigation.
- 2026-04-30 14:58 Added order page userId filter, route query sync, and export userId parameter.
- 2026-04-30 14:59 Backend local build succeeded with `mvn -DskipTests package`.
- 2026-04-30 14:59 Admin frontend local build succeeded with `npm run build`.
- 2026-04-30 15:00 Started local Vite dev server at `http://127.0.0.1:4174/`.
- 2026-04-30 15:00 Browser Use plugin could not run because local Node runtime is `v22.15.1` and the plugin requires `>= v22.22.0`.
- 2026-04-30 15:00 Playwright MCP opened `http://127.0.0.1:4174/orders?userId=1`; app rendered the admin login screen. Only console error was missing `favicon.ico`.
- 2026-04-30 15:01 Synced final operation log to `beifenstore`, commit `13aab52`.
- 2026-04-30 15:02 Synced cloud-backup commit reference to `beifenstore`, commit `8e3f04c`.
- 2026-04-30 15:05 Backend business repository commit created: `4acabd5 feat: improve admin user and order search`.
- 2026-04-30 15:05 Website business repository commit created: `0748a0d feat: add admin user pagination search`.
- 2026-04-30 15:05 Pushed backend branch `release/20260423-invite-cashback-linkage` to `origin`.
- 2026-04-30 15:05 Pushed website branch `release/20260423-invite-cashback-linkage` to `origin`.
- 2026-04-30 15:06 Ran `cloud-preview.ps1 -Target all`; frontend deployment succeeded, backend deployment uploaded and restarted but health check failed.
- 2026-04-30 15:06 Rolled backend back to remote backup `backups/app-20260430150557.jar`; health restored with `{"status":"UP"}`.
- 2026-04-30 15:07 Diagnosed backend error from `/home/ubuntu/apps/backend-api/app-systemd.log`: SQL referenced non-production column `orders.status`.
- 2026-04-30 15:07 Fixed `OrderMapper.totalPaidAmount()` and `todayPaidAmount()` to use production columns `order_status` and `pay_time`.
- 2026-04-30 15:08 Backend local build succeeded after fix.
- 2026-04-30 15:08 Backend fix commit created and pushed: `7796a00 fix: use production order status columns`.
- 2026-04-30 15:53 Rebuilt backend locally with `mvn -DskipTests package`; build succeeded.
- 2026-04-30 15:53 Uploaded fixed backend JAR to `ubuntu@43.139.76.37:/tmp/backend-api-20260430155317.jar`.
- 2026-04-30 15:53 Backed up remote stable JAR to `/home/ubuntu/apps/backend-api/backups/app-20260430155317.jar`.
- 2026-04-30 15:53 Replaced `/home/ubuntu/apps/backend-api/app.jar` with fixed JAR and restarted `zhixi-backend.service`.
- 2026-04-30 15:53 Remote service check returned `active`.
- 2026-04-30 15:53 Remote local API health check returned `{"success":true,"message":"OK","data":{"status":"UP"}}`.
- 2026-04-30 15:53 Public API health check `https://api.mashishi.com/api/health` returned `{"success":true,"message":"OK","data":{"status":"UP"}}`.
- 2026-04-30 15:53 Public site check `https://mashishi.com` returned HTTP `200 OK`.
- 2026-04-30 15:53 Reviewed backend service log after restart. The previous `Unknown column 'status'` SQL error did not recur.
- 2026-04-30 15:53 Observed one unrelated request error for `/api/ws/withdrawals` (`NoResourceFoundException`). This was recorded as a residual route/WebSocket issue and did not block this release because core health checks were normal.

## Verification
- `mvn -DskipTests package`: success.
- `npm run build`: success.
- Browser smoke test: success for route load and app render; login-protected content was not exercised because no administrator password was entered.
- Remote backend service: `active`.
- Remote backend health: success, status `UP`.
- Public API health: success, status `UP`.
- Public website: HTTP `200 OK`.
- Known non-blocking warning: Vite CSS minifier reported one existing CSS syntax warning (`Expected identifier but found "-"`); build completed successfully.

## Cloud Backup
- Complete. Remote: `git@github.com:zhuzhustar0371/beifenstore.git`
- Commit: `3636107 backup: admin user pagination nickname order search 20260430-143000`
- Final log update commit: `13aab52 docs: update admin user search backup log 20260430-1500`
- Latest log sync commit: `8e3f04c docs: record cloud backup log commit 20260430-1501`

## Rollback
- Use `code/` snapshot from this backup directory.
- Pre-redeploy remote backup JAR: `/home/ubuntu/apps/backend-api/backups/app-20260430155317.jar`.
- Previous rollback already validated with `/home/ubuntu/apps/backend-api/backups/app-20260430150557.jar` after the first failed backend deployment.
