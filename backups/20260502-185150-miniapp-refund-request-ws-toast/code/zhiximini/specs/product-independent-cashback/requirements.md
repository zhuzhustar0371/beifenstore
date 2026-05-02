# Product-Independent Cashback Requirements

## Background

The current cashback flow uses global order sequencing and global invite-first-paid sequencing. That allows a low-price order to advance the sequence for a later high-price order, which creates loss-making behavior and does not match the expected "per product" rule boundary.

## Scope

- Change self-purchase cashback counting from `user` scope to `user + product` scope.
- Change invite cashback counting from global invitee first-paid scope to `inviter + product` scope.
- Add per-product cashback rule configuration.
- Update admin product management to edit per-product cashback rules.
- Update user-facing rule displays so the copy states that rules are counted independently per product.

## Non-goals

- Rebuild historical cashback records into perfect per-product history.
- Introduce a versioned cashback rule engine.
- Change payout waiting period or withdrawal flow.

## User Stories

- As an operator, I want each product to have its own cashback ratios so that expensive and cheap products do not share one global ladder.
- As a buyer, I want my repeat-purchase cashback for product A to be calculated independently from product B.
- As an inviter, I want invite cashback for product A and product B to be counted separately.
- As support staff, I want cashback records to carry product information so that refund reconciliation and manual review are traceable.

## Acceptance Criteria

1. When a user pays for a product, the backend shall calculate personal cashback using only the paid-order history of the same user and the same product.
2. When a user pays for a product and has an inviter, the backend shall count invite progress using only first-paid records of the same inviter and the same product.
3. When an operator creates or edits a product, the system shall allow configuring personal second/third/fourth ratios and invite batch size/first ratio/repeat ratio for that product.
4. When the cashback rules API returns rules for a product, the payload shall describe the selected product and shall state that the counting scope is independent for that product.
5. When a paid order is refunded within the cashback-cancel window, the backend shall recalculate personal cashback and invite batch validity only inside the refunded order's product scope.
6. When existing environments start with older schemas, the migration runner shall add the required columns and the new invite-product relation table without breaking startup.
7. When admin or user interfaces show cashback rules, the copy shall no longer describe global order sequencing across all products.
