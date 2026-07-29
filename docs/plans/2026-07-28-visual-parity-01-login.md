# VISUAL-PARITY-01 — Flutter Assets + Login Exact Parity

**Type:** ui
**Status:** complete — awaiting user visual approval
**Progress:** `[██████████] 100%`

## Problem / Goal

صفحه Login و زیرساخت بصری مشترک Native با Flutter هم‌خوان نیست. Flutter منبع حقیقت
بصری است و این مأموریت assetها، tokenهای مصرف‌شده در Login و تمام stateهای Login/OTP را
روی Emulator یکسان audit، migrate، پیاده‌سازی و مقایسه می‌کند.

## Root cause / Design

مقادیر و assetها فقط از source و runtime واقعی Flutter استخراج می‌شوند. تغییرات Native
به Design System مشترکِ دارای consumer واقعی و Auth محدود می‌مانند؛ Flutter و Backend
read-only هستند و سایر featureها فقط regression verification می‌شوند.

## Phases

### Phase 1 — Investigate
- [x] Git baseline Native و Flutter و dirty-tree inventory ثبت شد.
- [x] branch `visual-parity-01/flutter-assets-login` از `f663484` ساخته شد.
- [x] assetها، theme، typography و auth widgets Flutter inventory شوند.
- [x] geometry و stateهای Login/OTP از source Flutter استخراج شوند.
- [x] package/build و مسیر اجرای Flutter روی API 33 تأیید شود.

### Phase 2 — Implement
- [x] audit inventory در `docs/audits/` ثبت شود.
- [x] ۹ screenshot واقعی Flutter گرفته شود.
- [x] asset migration manifest و hashهای مبدأ/مقصد ثبت شوند.
- [x] assetهای دارای مالکیت روشن و مصرف واقعی به Native منتقل شوند.
- [x] tokenهای مشترکِ مصرف‌شده در Login دقیقاً reconcile شوند.
- [x] Login و OTP Native با Flutter منطبق شوند.
- [x] debug UI از product Shell حذف شود.
- [x] bidi برای phone، OTP و `@username` اصلاح و تست شود.
- [x] ۹ screenshot واقعی Native گرفته شود.

### Phase 3 — Verify
- [x] برای ۹ state، side-by-side، overlay و diff ساخته شود.
- [x] mismatchهای ساختاری و geometry بالاتر از 2dp اصلاح شوند.
- [x] unit testهای token، asset، bidi و UiState پاس شوند.
- [x] instrumentation stateهای Login/OTP، keyboard و recreation پاس شود.
- [x] `assembleBetaDebug` و lint ماژول‌های تغییرکرده پاس شوند.
- [x] runtime API 33 و logcat بدون crash/ANR پاس شود.
- [x] regression Startup/Shell/Feed/Profile پاس شود.
- [x] final verification و contact sheet ثبت شوند.
- [x] پنج commit هدفمند ساخته و working tree پاک شود.

## Decisions & Notes

- Flutter checkout در شروع dirty بود؛ تمام تغییرات موجود کاربر حفظ و Repository فقط
  read-only بررسی می‌شود.
- `SLICE-01D` و تغییر featureهای Feed/Profile/Follow خارج Scope هستند.
- Visual Review قبلی evidence baseline است، نه اثبات parity فعلی.
