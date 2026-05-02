# Product-Independent Cashback Design

## Overview

Implement product-scoped cashback by moving both calculation dimensions onto product boundaries while keeping the current order and withdrawal flows intact.

## Data Model

### `products`

Add six configuration fields:

- `personal_second_ratio`
- `personal_third_ratio`
- `personal_fourth_ratio`
- `invite_batch_size`
- `invite_first_ratio`
- `invite_repeat_ratio`

These fields store decimal ratios and the invite batch size. Defaults preserve the current business rule:

- personal second: `0.10`
- personal third: `0.20`
- personal fourth: `1.00`
- invite batch size: `3`
- invite first: `1.00`
- invite repeat: `0.20`

### `invite_product_relations`

Store invite progress per product:

- `inviter_user_id`
- `invitee_user_id`
- `product_id`
- `bind_time`
- `first_paid_time`

Use a unique key on `(invitee_user_id, product_id)` so each invitee can contribute one first-paid marker per product.

### `cashback_records`

Add `product_id` so invite batches and refund reconciliation can be filtered by product.

## Service Behavior

### Personal cashback

- Count prior paid orders by `user_id + product_id`.
- Calculate the current sequence from that product-specific count.
- Create the cashback record with `product_id`.

### Invite cashback

- Keep the existing global inviter binding in `invite_relations`.
- For settlement, create or reuse an `invite_product_relations` row using inviter, invitee, and product.
- Mark first-paid on that product-specific relation only once.
- Count valid first-paid rows by `inviter_user_id + product_id`.
- Trigger invite batch cashback only when the count reaches a multiple of the configured batch size.
- Prevent duplicate invite payouts by checking `user_id + product_id + batch_no`.

### Refund recalculation

- Cancel direct cashback records linked to the refunded order.
- Recalculate personal cashback only for valid paid orders of the same user and same product.
- If the refunded order was the tracked first-paid record for the inviter-product pair, clear that marker and reconcile extra invite batches for that same inviter-product pair only.

## Migration Strategy

- Add the new product rule columns.
- Add `cashback_records.product_id`.
- Create `invite_product_relations`.
- Backfill null product rule columns with default values.
- Backfill `cashback_records.product_id` from linked orders where possible.
- Seed `invite_product_relations` from existing global invite relations and paid orders so current users can continue operating without manual intervention.

Historical invite product history cannot be reconstructed perfectly for every edge case; the migration only seeds enough data to keep live settlement and refund logic working.

## UI Changes

### Admin

- Add rule inputs to the product create/edit form.
- Show a compact rule summary on each product card.

### Mini Program / Website

- Request `/api/cashbacks/rules` with `productId` when available.
- Show the selected product name in the rules page.
- Update wording to say cashback is counted independently for the current product.

## Validation and Risk

- Ratios are limited to `[0, 1]`.
- Invite batch size must be at least `1`.
- Existing dirty worktrees prevent an automatic safe publish unless local validation passes and the user accepts pushing unrelated pre-existing changes in the same repos.
