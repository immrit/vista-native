# SLICE-01C Contract Audit

**Date:** 2026-07-28
**Backend source:** `E:\vista-backend\internal\profile\handlers.go`,
`service.go`, and `repository.go`
**Flutter source:** `E:\vista\lib\features\profile\data\profile_repository.dart`,
`providers\user_profile_provider.dart`, `features/posts/screens/profileScreen.dart`,
`ExploreFeedScreen.dart`, and `features/posts/navigation/content_routes.dart`

## Public Profile

- Request: `GET /v1/profiles/{user_id}`.
- Path argument: UUID; an invalid UUID returns HTTP 400 with `{"error":"invalid user id"}`.
- Authentication: optional. With a valid viewer session the response includes relationship
  state; without one it remains a public read.
- Success envelope: the profile object is the top-level JSON object; there is no `data`
  wrapper.
- Not found/inactive user: HTTP 404,
  `{"code":"PROFILE_NOT_FOUND","message":"پروفایل یافت نشد"}`.
- Method mismatch: HTTP 405 with `{"error":"method not allowed"}`.
- Middleware unauthorized/rate-limit errors use the nested shape
  `{"error":{"code":"error","message":"..."}}`; profile service errors use flat
  `code/message`; generic handler errors use `{"error":"internal server error"}`.
- Go `time.Time` values are encoded as RFC3339/RFC3339Nano timestamps.

Fields consumed by this Slice:

| JSON field | Contract |
|---|---|
| `user_id` | required string UUID |
| `username` | nullable/omitted string |
| `full_name` | required string |
| `bio` | nullable/omitted string |
| `avatar_url` | nullable/omitted string |
| `is_verified` | required boolean |
| `verification_type` | nullable/omitted string |
| `is_private` | required boolean |
| `post_count` | required integer |
| `follower_count` | required integer |
| `following_count` | required integer |
| `follow_status` | omitted/empty for anonymous or self; otherwise `none`, `following`, or `requested` |
| `is_blocked` | required boolean; indicates the viewer blocked the target |
| `subscription_plan` | nullable/omitted string |
| `premium_days_remaining` | nullable/omitted integer |
| `updated_at` | required timestamp |

The Backend also returns private account/contact/profile-completion/security fields that
this Slice does not persist. For non-self reads it explicitly removes email, phone number,
and birth date. It does not hide the basic public identity/counts solely because
`is_private=true`. It does not expose a separate `is_blocked_by` field on this endpoint.

## Follow and Unfollow

### Follow

- Request: authenticated `POST /v1/me/follow`.
- Body: `{"target_user_id":"<UUID>"}`.
- Success: HTTP 200,
  `{"status":"following","message":"با موفقیت دنبال کردید"}` for public accounts.
- Private account: HTTP 200,
  `{"status":"requested","message":"درخواست دنبال کردن ارسال شد"}`.
- Repeating Follow is idempotent at persistence level through `ON CONFLICT DO NOTHING`;
  it returns the same canonical status.

### Unfollow

- Request: authenticated `POST /v1/me/unfollow`.
- Body: `{"target_user_id":"<UUID>"}`.
- Success: HTTP 200, `{"status":"unfollowed"}`.
- Repeating Unfollow is idempotent: deleting a missing row still succeeds.
- Unfollow also cancels an existing pending private-account request.

### Edge behavior

- Malformed target UUID: HTTP 400,
  `{"code":"INVALID_USER_ID","message":"شناسه کاربر نامعتبر است"}`.
- Unauthorized requests are rejected by middleware with HTTP 401 and the nested error
  envelope.
- Self-follow is rejected in the repository, but is currently surfaced as a generic HTTP
  500 because the service does not map that repository error. Native prevents the request
  by redirecting self navigation to Own Profile.
- A missing target is not explicitly mapped by Follow before the foreign-key insert and
  can surface as a generic server error. Native does not invent a 404 contract for it.
- No profile-specific HTTP conflict or rate-limit behavior is implemented in the profile
  service. Global middleware may return HTTP 429.
- Follow does not add an explicit block-pair check in this code path; Native presents
  Backend `is_blocked=true` as an unavailable relationship and sends no mutation.

## Flutter Behavior Reference

- Feed avatar opens `ProfileRoute(userId, username)`; the whole post opens Post Detail.
  The Native Slice additionally makes author name and username explicit profile targets as
requested.
- Other Profile initially loads by ID and falls back to a local profile cache after network
  errors.
- Header shows avatar, post/follower/following counts, full name, verification badge, and
  private lock. Flutter derives premium presentation from subscription/profile metadata.
- Pull-to-refresh reloads profile and profile content providers.
- Follow button labels are `دنبال کردن`, `دنبال می‌کنید`, and the disabled private state
  `در انتظار تایید`.
- Follow/Unfollow updates local relationship/counts only after the request returns; errors
  are shown through a user-friendly snackbar. The Native Slice deliberately provides the
  requested bounded optimistic update plus rollback.
- Flutter can navigate to Followers/Following lists, Chat, posts/reels/music, reporting,
  avatar zoom, and sharing. All of those behaviors are explicitly outside this Native
  Slice and will not be added.
- Self identity is detected from the authenticated user ID and switches to own-profile
  controls. Native uses canonical redirection to its existing Own Profile tab.
