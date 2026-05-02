# 2026-04-30 17:37:25 Admin Mobile Layout Log

## Goal

- Switch the admin frontend to a mobile layout when either:
  - the user agent is mobile, or
  - the viewport width is smaller than `768px`
- Keep desktop layout available.
- Keep core admin actions available on mobile.

## Backups

- Local backup:
  - `G:\store\20260430-173725-admin-mobile-layout`
- Remote backup snapshot:
  - `G:\zhiximini\_tmp\beifenstore-20260430-161936\20260430-173725-admin-mobile-layout-backup`
- Remote backup push:
  - repo: `git@github.com:zhuzhustar0371/beifenstore.git`
  - pushed branch: `main`
  - final push result: success

## Pre-change Status

- Repo: `G:\zhiximini\zhixi-website`
- Existing unrelated worktree state before this task:
  - `M admin-frontend/src/views/ProductsPage.vue`
  - `?? frontend-dist-upload/`

## Atomic Steps

1. Create local timestamped backup under `G:\store`.
2. Create matching backup snapshot inside `beifenstore`.
3. Fix nested `.git` issue inside the backup snapshot so the remote backup stores plain files instead of gitlinks.
4. Push the `beifenstore` backup after rebasing on latest remote `main`.
5. Add shared admin viewport detection.
6. Update main admin layout to switch behavior on mobile.
7. Add mobile card layouts for orders and cashbacks pages.
8. Run local build verification.

## Files Changed

- `G:\zhiximini\zhixi-website\admin-frontend\src\composables\useAdminViewport.js`
- `G:\zhiximini\zhixi-website\admin-frontend\src\layouts\MainLayout.vue`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\OrdersPage.vue`
- `G:\zhiximini\zhixi-website\admin-frontend\src\views\CashbacksPage.vue`

## Implementation Notes

- Added shared `isMobileAdmin` state.
- Detection rule:
  - mobile UA match, or
  - viewport width `< 768`
- `MainLayout.vue`
  - disables desktop mouse trail effect on mobile
  - keeps drawer navigation
  - reduces content padding on mobile
- `OrdersPage.vue`
  - desktop table preserved
  - mobile mode uses cards with:
    - order number
    - amount
    - order/refund status
    - user and address summary
    - expandable shipping section
    - ship/refund action buttons
- `CashbacksPage.vue`
  - desktop tables preserved
  - mobile mode uses cards for:
    - withdrawal requests
    - cashback records
    - transfer/sync/copy actions remain available

## Validation

- Command:
  - `npm.cmd run build`
- Result:
  - success
- Output summary:
  - Vite production build completed successfully
  - generated `dist/assets/index-DabziSIV.css`
  - generated `dist/assets/index-CxTZEH2i.js`
- Warning:
  - CSS minifier reported one syntax warning during minification
  - build still succeeded
  - no blocking error was produced

## Rollback

- Restore from local backup:
  - `G:\store\20260430-173725-admin-mobile-layout\code\zhixi-website`
- Or restore from remote backup snapshot in `beifenstore`.

## Current Status

- Code changes completed locally.
- Backups completed locally and remotely.
- Build verification completed.
- No deployment executed in this step.
