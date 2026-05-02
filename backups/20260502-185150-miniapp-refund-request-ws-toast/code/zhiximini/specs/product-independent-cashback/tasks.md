# Implementation Plan

- [x] 1. Add spec and operation log artifacts
  - Create requirements, design, tasks, and execution log files.
  - Record backup paths and approved scope.
  - _Requirement: 1, 2, 3, 4, 5, 6, 7_

- [x] 2. Implement backend schema and model changes
  - Add product cashback configuration fields.
  - Add `product_id` to cashback records.
  - Add `invite_product_relations` table and mapper/model.
  - Extend the startup migration runner and schema file.
  - _Requirement: 3, 6_

- [x] 3. Implement product-scoped settlement and refund logic
  - Change personal cashback counting to `user + product`.
  - Change invite counting to `inviter + product`.
  - Update invite batch deduplication and refund reconciliation.
  - _Requirement: 1, 2, 5_

- [x] 4. Update admin product management
  - Expose the new product rule fields in DTOs, mapper SQL, and admin page form state.
  - Show concise rule summaries for operators.
  - _Requirement: 3_

- [x] 5. Update user-facing rule pages
  - Return product-specific rule payload from the backend.
  - Update mini program and web rule copy to state per-product independence.
  - Pass `productId` from product detail pages where applicable.
  - _Requirement: 4, 7_

- [x] 6. Validate and archive
  - Run local backend and frontend builds where possible.
  - Update the execution log with file changes, commands, and outcomes.
  - Document any publish blockers or rollback notes.
  - _Requirement: 6, 7_
