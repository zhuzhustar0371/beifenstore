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

## Verification
- `mvn -DskipTests package`: success.
- `npm run build`: success.
- Browser smoke test: success for route load and app render; login-protected content was not exercised because no administrator password was entered.
- Known non-blocking warning: Vite CSS minifier reported one existing CSS syntax warning (`Expected identifier but found "-"`); build completed successfully.

## Cloud Backup
- Complete. Remote: `git@github.com:zhuzhustar0371/beifenstore.git`
- Commit: `3636107 backup: admin user pagination nickname order search 20260430-143000`

## Rollback
- Use `code/` snapshot from this backup directory.
