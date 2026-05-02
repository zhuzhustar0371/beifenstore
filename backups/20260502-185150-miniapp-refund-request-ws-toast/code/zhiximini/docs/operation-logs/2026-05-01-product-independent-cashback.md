# 2026-05-01 Product-Independent Cashback Execution Log

## Task

Implement product-independent cashback rules after approval.

## Approval

- Analysis completed in-thread before coding.
- User approval received on `2026-05-01`.

## Backup

- Local backup: `G:\store\zhiximini-backup-20260501-200019`
- Local atomic backup note: `G:\store\zhiximini-backup-20260501-200019\backup-operation-20260501-200019.md`
- Remote backup worktree: `G:\store\_remote_backup_work\beifenstore`
- Remote backup branch: `backup/20260501-200019-product-independent-cashback-prechange`
- Remote atomic plan: `G:\store\_remote_backup_work\beifenstore\20260501-200019-product-independent-cashback-prechange\2026-05-01-product-independent-cashback-prechange-atomic-plan.md`

## Approved Scope

- Personal cashback counts independently per `user + product`.
- Invite cashback counts independently per `inviter + product`.
- Product carries its own cashback ratios and invite batch configuration.
- Rules UI copy must reflect product independence.

## Execution Notes

### 2026-05-01 Initial State

- Root workspace is not a git repo.
- `backend-api` and `wechat-app` already had unrelated dirty changes before implementation.
- Because target files already contain pre-existing modifications, any later publish step must be evaluated carefully to avoid shipping unrelated work by accident.

### 2026-05-01 Planned Changes

- Add product cashback configuration columns and product-scoped invite relation table.
- Update settlement/refund recalculation logic to use product scope.
- Add product information to cashback records.
- Update admin product editor and rule pages.

### 2026-05-01 Files Changed

- `backend-api/src/main/java/com/zhixi/backend/config/DatabaseMigrationRunner.java`
- `backend-api/src/main/java/com/zhixi/backend/controller/CashbackController.java`
- `backend-api/src/main/java/com/zhixi/backend/dto/AdminCashbackVO.java`
- `backend-api/src/main/java/com/zhixi/backend/dto/AdminProductUpsertRequest.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/CashbackRecordMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/InviteProductRelationMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/OrderMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/mapper/ProductMapper.java`
- `backend-api/src/main/java/com/zhixi/backend/model/CashbackRecord.java`
- `backend-api/src/main/java/com/zhixi/backend/model/InviteProductRelation.java`
- `backend-api/src/main/java/com/zhixi/backend/model/Product.java`
- `backend-api/src/main/java/com/zhixi/backend/service/AdminManageService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/CashbackService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/OrderService.java`
- `backend-api/src/main/java/com/zhixi/backend/service/ProductService.java`
- `backend-api/src/main/resources/schema.sql`
- `wechat-app/pages/product/product.js`
- `wechat-app/pages/product/product.wxml`
- `wechat-app/pages/product/product.wxss`
- `wechat-app/pages/rules/rules.js`
- `wechat-app/pages/rules/rules.wxml`
- `zhixi-website/admin-frontend/src/views/ProductsPage.vue`
- `zhixi-website/frontend/src/views/HomePage.vue`
- `zhixi-website/frontend/src/views/RulesPage.vue`

### 2026-05-01 Backend Outcome

- Added product-level cashback configuration fields on `Product`.
- Added `invite_product_relations` mapper/model and startup migration.
- Added `product_id` to cashback record model/mapper usage.
- Switched personal cashback sequence to `user + product`.
- Switched invite cashback sequence to `inviter + product`.
- Added refund-time reconciliation for product-scoped invite batches.

### 2026-05-01 Frontend Outcome

- Admin product editor now supports per-product cashback rule inputs.
- Mini Program product detail page now links to the current product's rule page.
- Mini Program rule page now loads `/api/cashbacks/rules?productId=...`.
- Public web rule page now shows product-scoped wording and supports `?productId=...`.
- Public web home page now links to the current product's rule page and loads preview rules by product.

### 2026-05-01 Validation

- `backend-api`: `mvn -q -DskipTests compile` -> passed
- `zhixi-website/admin-frontend`: `npm run build` -> passed
- `zhixi-website/frontend`: `npm run build` -> passed
- `zhixi-website/frontend`: `npm run build` after `HomePage.vue` patch -> passed
- `wechat-app/pages/index/index.js`: `node --check` -> passed
- `wechat-app/pages/product/product.js`: `node --check` -> passed
- `wechat-app/pages/rules/rules.js`: `node --check` -> passed

### 2026-05-01 Build Notes

- `zhixi-website/admin-frontend` build emitted one non-blocking CSS minify warning:
  - `Expected identifier but found "-" [css-syntax-error]`
  - Build still completed successfully and produced `dist/`.

### 2026-05-01 Isolated Submit

- Created isolated staging root at `G:\store\submit-staging-20260501-222600-product-independent-cashback`.
- Created clean submit branches from `origin/release/20260423-invite-cashback-linkage`:
  - `backend-api`: `submit/20260501-product-independent-cashback-isolated`
  - `wechat-app`: `submit/20260501-product-independent-cashback-isolated`
  - `zhixi-website`: `submit/20260501-product-independent-cashback-isolated`
- Isolated commit SHAs:
  - `backend-api`: `c8409a6a18f3d559ed6b1e7208a35577c20ed286`
  - `wechat-app`: `14a2d05078fe574376a45d70d5b4076292fdafdc`
  - `zhixi-website`: `d934b79baed037ec099d40ec69fc713cb06e0bad`
- Pushed all three submit branches to origin successfully.

### 2026-05-01 Cloud Build

- Initial server-side `git clone` attempt failed because the server did not have GitHub deploy key access:
  - `git@github.com: Permission denied (publickey).`
- Fallback strategy:
  - Exported local isolated commits with `git archive --format=zip`.
  - Uploaded source archives to `/home/ubuntu/build-uploads/submit-20260501-product-independent-cashback-isolated`.
  - Downloaded portable build tools into `/home/ubuntu/build-tools-cache` without modifying live app directories:
    - `node v20.17.0`
    - `npm 10.8.2`
    - `Apache Maven 3.9.9`
- Server prebuild snapshot saved at:
  - `/home/ubuntu/server-backups/prebuild-20260501-230902-product-independent-cashback/server-prebuild-snapshot.md`
- Server isolated build root:
  - `/home/ubuntu/builds/submit-20260501-product-independent-cashback-isolated-20260501-230902`
- Server build summary:
  - `/home/ubuntu/builds/submit-20260501-product-independent-cashback-isolated-20260501-230902/build-summary.md`
- Server build result:
  - `backend-api`: `mvn -q -DskipTests package` -> passed
  - `zhixi-website/frontend`: `npm install && npm run build` -> passed
  - `zhixi-website/admin-frontend`: `npm install && npm run build` -> passed
  - `wechat-app`: `node --check pages/product/product.js` -> passed
  - `wechat-app`: `node --check pages/rules/rules.js` -> passed

### 2026-05-01 Rollback Position

- Pre-change local backup remains at `G:\store\zhiximini-backup-20260501-200019`.
- Pre-change remote backup branch remains at `backup/20260501-200019-product-independent-cashback-prechange`.
- Rollback space currently includes:
  - original pre-change local backup
  - original pre-change remote backup branch
  - current production frontend backup: `/home/ubuntu/zhixi/backups/current-20260501232743`
  - current production admin dist backup: `/home/ubuntu/apps/manager-backend/backups/dist-20260501232743`
  - current production backend jar backup: `/home/ubuntu/apps/backend-api/backups/app-20260501232819.jar`
  - isolated submit branches
  - isolated server build root and prebuild snapshot

### 2026-05-01 Production Publish

- Frontend production publish executed from isolated worktree:
  - command runner: `C:\Program Files\Git\bin\bash.exe`
  - script: `zhixi-website-final/scripts/deploy_to_server.sh`
  - target: `ubuntu@43.139.76.37:/home/ubuntu/zhixi`
- Frontend publish result:
  - local build completed again in isolated worktree
  - package uploaded to `/home/ubuntu/zhixi/releases`
  - current production frontend backed up to `/home/ubuntu/zhixi/backups/current-20260501232743`
  - legacy admin dist backed up to `/home/ubuntu/apps/manager-backend/backups/dist-20260501232743`
  - new frontend assets deployed under `/home/ubuntu/zhixi/current/frontend/dist`
  - new admin assets deployed under `/home/ubuntu/zhixi/current/admin-frontend/dist`

- Backend production publish executed from isolated worktree:
  - command runner: `C:\Program Files\Git\bin\bash.exe`
  - script: `scripts/deploy_backend_api.sh`
  - local backend source overridden by env: `BACKEND_PROJECT_ROOT=G:/store/submit-staging-20260501-222600-product-independent-cashback/backend-api-final`
  - target: `ubuntu@43.139.76.37:/home/ubuntu/apps/backend-api`
- Backend publish result:
  - local `mvn -DskipTests package` completed successfully
  - previous jar backed up to `/home/ubuntu/apps/backend-api/backups/app-20260501232819.jar`
  - new jar replaced `/home/ubuntu/apps/backend-api/app.jar`
  - `zhixi-backend.service` restarted successfully

### 2026-05-01 Production Verification

- Immediate health check inside deploy script failed once because the service was probed too early:
  - script waits about 2 seconds
  - actual Spring Boot startup plus migrations completed several seconds later
- Follow-up verification confirmed production is healthy:
  - `zhixi-backend.service` -> `active`
  - `http://127.0.0.1:8080/api/health` -> `{"status":"UP"}`
  - startup log shows database migrations completed:
    - `products.personal_second_ratio`
    - `products.personal_third_ratio`
    - `products.personal_fourth_ratio`
    - `products.invite_batch_size`
    - `products.invite_first_ratio`
    - `products.invite_repeat_ratio`
    - `cashback_records.product_id`
    - `invite_product_relations`
- `nginx -t` passed and `nginx` reload completed successfully.
- Domain-routed production checks succeeded:
  - `https://127.0.0.1/` with `Host: mashishi.com` -> `HTTP/2 200`
  - `https://127.0.0.1/` with `Host: admin.mashishi.com` -> `HTTP/2 200`
  - `https://127.0.0.1/api/health` with `Host: api.mashishi.com` -> `UP`
- Production API returned product-scoped cashback rules successfully:
  - sample product id: `5`
  - sample product name: `矿泉水`
  - sample endpoint: `/api/cashbacks/rules?productId=5`
  - response included `productId`, `productName`, `inviteBatchSize`, `scopeNote`, `personalRules`, `inviteRules`

### 2026-05-01 Publish Status

- Git submit: completed on isolated branches.
- Cloud server build: completed in isolated directories.
- Production frontend publish: completed.
- Production backend publish: completed.
- Production rollback: not executed.
- WeChat Mini Program platform publish: not executed in this turn.
