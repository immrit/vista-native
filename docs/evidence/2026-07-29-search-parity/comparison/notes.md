# Search Visual Comparison Notes

## Valid pair inventory

Only one controlled Flutter/Native pair was captured at 1080×2400 on API 33 before the parallel Feed/Profile task began replacing the installed Native APK under the same application id.

| Pair | State | Threshold | Mismatched pixels | Mismatch | MAE |
| --- | --- | ---: | ---: | ---: | ---: |
| `01-initial` | Search launcher initial | 24 | 28,390 / 2,592,000 | 1.0953% | 3.5777 |

Artifacts for the pair include side-by-side, alpha overlay, thresholded diff, bounding boxes, and machine-readable metrics under `comparison/initial/`.

## Product mismatches

- Flutter exposes a QR scanner suffix action. Native deliberately omits it because no real scanner destination exists; adding a dead control would violate functional parity.
- The Search chip typography/tint and launcher header geometry still have visible differences.
- The base branch Shell bottom navigation differs from Flutter in labels, selection capsule, spacing, and non-Search icons. Final reconciliation belongs in the separate Feed/Profile integration gate.
- Only the initial launcher pair was stable enough for automated comparison. Focused, results, history, pagination, error, and navigation pairs remain unverified visually.

## Non-product noise

- Status-bar clock differs between captures.
- Cursor blink and Gboard suggestion contents are excluded when keyboard pairs are eventually compared.

## Iteration status

One capture/compare/source-fix iteration was completed. A second controlled cycle could not be produced because another worktree repeatedly installed an APK with the same package id on the shared Emulator. Visual parity therefore remains Pending.
