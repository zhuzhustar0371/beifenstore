# 20260430-002436 miniapp share config

## Atomic Operation

- Task: configure WeChat Mini Program page sharing for friend/group and timeline scenarios.
- Approval: user approved execution after atomic plan review.
- Workspace: `G:\zhiximini\wechat-app`
- Backup timestamp: `20260430-002436`
- Source branch: `release/20260423-invite-cashback-linkage`
- Source commit: `86dc4d6247a8d0af4687395ea4419286a63d3bf7`
- Source commit summary: `86dc4d6 2026-04-29 19:45:13 +0800 fix: bind home cashback price text`

## Planned File Changes

1. Add `utils/share.js` for reusable share title, path, image, inviter parameter, and share menu helpers.
2. Update `pages/index/index.js` with homepage share handlers.
3. Update `pages/product/product.js` with product detail share handlers and product id preservation.
4. Update `pages/invite/invite.js` with invite share handlers and inviter id preservation.
5. Update `pages/cashback/cashback.js` with cashback share handlers.
6. Update `pages/rules/rules.js` with rules share handlers.
7. Update `pages/web-login/web-login.js` so the existing empty share handler returns a deterministic homepage share config.

## Not Planned

- Do not modify `app.json` unless later verification proves a required standard schema field is missing.
- Do not add sharing to login, order, or address pages to avoid forwarding private or state-specific screens.

## Rollback Basis

- Full source backup path: `G:\store\20260430-002436-miniapp-share-config\code\wechat-app`
- Remote backup target: `git@github.com:zhuzhustar0371/beifenstore.git`

