# 20260430-004700 miniapp ui fix

## Atomic Operation

- Task: fix Mini Program UI issues found in homepage and profile page screenshots.
- Approval: user approved atomic UI repair plan before execution.
- Workspace: `G:\zhiximini\wechat-app`
- Backup timestamp: `20260430-004700`
- Source branch: `release/20260423-invite-cashback-linkage`
- Source commit: `51db1a131256fe2c657de0c90d9aa4a2247817c3`
- Source commit summary: `51db1a1 2026-04-30 00:32:27 +0800 feat: configure miniapp sharing`

## Planned File Changes

1. Update `pages/user/user.wxss` to restore missing custom navigation styles and fix header/profile stack spacing.
2. Update `pages/user/user.wxml` only if a minimal wrapper is required for stable hero layout.
3. Update `pages/index/index.wxss` to rebalance hero title sizing, product card height, and bottom spacing.
4. Update `custom-tab-bar/index.wxss` to reduce floating tab bar visual bulk and bottom overlap.

## Not Planned

- Do not modify `pages/user/user.js` business logic unless layout repair requires a tiny metrics field.
- Do not change API requests, data binding semantics, or sharing logic.
- Do not replace image assets in this pass; unify layout around current icons first.

## Rollback Basis

- Full source backup path: `G:\store\20260430-004700-miniapp-ui-fix\code\wechat-app`
- Remote backup target: `git@github.com:zhuzhustar0371/beifenstore.git`

