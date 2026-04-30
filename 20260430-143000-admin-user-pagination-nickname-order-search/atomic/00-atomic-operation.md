# Atomic Operation: admin-user-pagination-nickname-order-search

## Time
- Created: 2026-04-30 14:30 Asia/Shanghai

## Goal
- Add usable pagination and keyword search to the admin user management page.
- Allow searching users by nickname, mobile, invite code, and WeChat authorization nickname/openid.
- Allow admin order search to locate orders by user nickname/mobile in addition to order number and tracking number.
- Add a user-to-orders navigation path so a specific user's orders can be filtered quickly.

## Approved Scope
- Frontend: `zhixi-website/admin-frontend/src/api.js`
- Frontend: `zhixi-website/admin-frontend/src/views/UsersPage.vue`
- Frontend: `zhixi-website/admin-frontend/src/views/OrdersPage.vue`
- Backend: `backend-api/src/main/java/com/zhixi/backend/mapper/UserMapper.java`
- Backend: `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- Backend/service export query if required for consistent order CSV search.

## Backup Requirement
- Local backup root: `G:\store\20260430-143000-admin-user-pagination-nickname-order-search`
- Local code snapshot: `G:\store\20260430-143000-admin-user-pagination-nickname-order-search\code`
- Cloud backup target: `git@github.com:zhuzhustar0371/beifenstore.git`

## Execution Plan
1. Record current repository status.
2. Copy current source code into local backup `code/`.
3. Push the same backup structure to `beifenstore`.
4. Implement backend search expansion.
5. Implement frontend user pagination/search and order query linkage.
6. Build backend and admin frontend locally.
7. Record all commands, changed files, and verification result in `01-change-log.md`.

## Rollback Basis
- Restore files from this backup `code/` snapshot.
- If a deployed build fails, push the backed-up stable source to the deployment branch/server and rebuild.
