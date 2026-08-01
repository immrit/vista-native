# Search Backend Contract

Backend reference: `E:\vista-backend` (source read-only). Git identity could not be queried because Git reported dubious ownership; no global `safe.directory` mutation was made. Contract evidence comes from the current readable Go source.

Flutter client reference: `E:\vista` at observed HEAD `64994fa1dfa6f1b146cb9d132758f105670e4d71`.

## Endpoints actually used by Flutter Search

### `GET /v1/profiles/search`

- Middleware: optional authentication.
- Query:
  - `q`: trimmed query; Flutter removes leading `@`.
  - `limit`: default/effective 20 if `<=0` or `>50`; Flutter uses 20.
  - `offset`: negative becomes 0; Flutter starts at 0 and adds the raw received count.
- Empty `q`: HTTP 200 with `{"profiles":[]}`.
- Search: case-insensitive substring over active users' username/full name.
- Ordering: exact username, username prefix, verified-or-active-premium, newest account.
- Envelope:

```json
{
  "profiles": [
    {
      "user_id": "string",
      "username": "string or omitted",
      "full_name": "string",
      "avatar_url": "string or omitted"
    }
  ]
}
```

- There is no `has_more`, total, cursor, relationship state, verified flag, verification type, role, or account type in this search envelope.

### `GET /v1/profiles/by-username/{username}`

- Middleware: optional authentication.
- Used by Flutter only on `offset=0` when the normalized query matches `[A-Za-z0-9_.]{2,32}`.
- An exact result is prepended to `/profiles/search` results and deduplicated by user id.
- Response is the full profile envelope. Search consumes identity/avatar and may incidentally obtain verification fields through this enrichment.
- Missing username: HTTP 400 `{"error":"username is required"}`.
- Other typed profile failures use the flat `{code,message}` service error; untyped failures use HTTP 500 `{"error":"internal server error"}`.

### `GET /v1/posts/hashtag/{tag}`

- Middleware: optional authentication.
- Path tag is required; service removes one leading `#`.
- Query:
  - `limit`: if `<=0` or `>50`, Backend resets it to 15.
  - `offset`: integer offset, or an RFC3339Nano timestamp interpreted as a cursor.
- Flutter currently sends `limit=60`, which Backend converts to 15. Native uses the effective contract value 15 rather than repeating an invalid request.
- Empty tag service result: HTTP 200 with empty posts and `has_more=false`.
- Envelope:

```json
{
  "posts": [
    {
      "id": "string",
      "user_id": "string",
      "content": "string or omitted",
      "image_url": "string or omitted",
      "image_urls": ["string"],
      "video_url": "string or omitted",
      "music_url": "string or omitted",
      "aspect_ratio": "string or omitted",
      "music_title": "string or omitted",
      "tags": ["string"],
      "like_count": 0,
      "comment_count": 0,
      "is_liked": false,
      "is_saved": false,
      "hide_like_count": false,
      "hide_comment_count": false,
      "author": {
        "user_id": "string",
        "username": "string or omitted",
        "full_name": "string",
        "avatar_url": "string or omitted",
        "is_verified": false,
        "verification_type": "string or omitted"
      },
      "created_at": "RFC3339 timestamp",
      "updated_at": "RFC3339 timestamp"
    }
  ],
  "has_more": false,
  "next_cursor": "RFC3339Nano timestamp or omitted"
}
```

- Flutter Search parses only `posts` and exposes no post load-more interaction.

### `GET /v1/hashtags/search`

- Middleware: optional authentication.
- Query:
  - `q`: trimmed; Backend removes one leading `#`.
  - `limit`: default 20 if `<=0` or `>50`; Flutter uses 10.
- Empty keyword: HTTP 200 `{"hashtags":[]}`.
- Envelope: `{"hashtags":[{"tag":"string","usage_count":0}]}`.
- Flutter debounces this independently by 300 ms and displays at most six rows.

### `GET /v1/hashtags/trending`

- Middleware: optional authentication.
- Query:
  - `limit`: default 20 if `<=0` or `>50`; Flutter uses 12.
  - `days`: default 30 if `<=0` or `>365`; Flutter relies on 30.
- Envelope: `{"hashtags":[{"tag":"string","usage_count":0}]}`.
- Used by both launcher and workspace zero state; failure is best-effort and produces empty/local fallback UI.

## Authentication and errors

- All five Search read endpoints are optional-auth endpoints. A bearer token enriches viewer-sensitive fields where supported, but absence of auth is not itself an endpoint rejection.
- Module service errors serialize as flat `{"code":"...","message":"..."}` with their HTTP status.
- Untyped profile/post failures serialize as HTTP 500 `{"error":"internal server error"}`.
- Method mismatch is HTTP 405 with `{"error":"method not allowed"}`.
- Missing hashtag path is HTTP 400 with `{"error":"hashtag is required"}`.
- Search-specific minimum-length validation and Search-specific rate limiting do not exist in the audited handlers. Global middleware may still return standard unauthorized/rate-limit/server responses.
- Native must parse flat service errors, nested/global envelopes already supported by `BackendErrorParser`, `Retry-After`, malformed JSON, timeouts, cancellation, and 5xx.

## Contract decisions for Native

- User paging remains `limit=20` plus integer `offset`; no cursor abstraction is invented.
- Direct hashtag search uses `limit=15`, parses `has_more` and `next_cursor`, but does not expose post pagination because Flutter has no such interaction.
- Search results are not persisted as an offline cache because Flutter does not cache them. Recent history remains available offline.
- Profile search failure follows the Flutter-visible behavior (empty list) while direct hashtag failure exposes retry; append failure retains existing user content.
- Exact username enrichment is kept on the first page and deduplicated by `user_id`.
- DTOs retain nullable/omitted fields; UI models apply safe fallbacks and never display fake verification or media.
