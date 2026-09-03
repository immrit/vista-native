# Native Telegram Chat Hardening

**Type:** bug | ui | perf
**Status:** in-progress
**Progress:** `[██████░░░░] 60%`

## Problem / Goal
بستن مشکلات باقی‌مانده‌ی چت Native در انتخاب پیام و منوی کانتکست، با حفظ قراردادهای موجود و بدون تغییر در کارهای نامرتبط.

## Root cause / Design
ریل انتخاب پیام نباید با padding حباب ترکیب شود؛ حباب باید جای خود را حفظ کند و checkbox به‌صورت overlay مستقل تراز شود. منوی کانتکست باید مختصات window را با Dialog هماهنگ کند و واکنش‌ها با فاصله و z-order مستقل نمایش داده شوند.

## Phases

### Phase 1 — Investigate
- [x] بررسی worktree و diff فایل ChatScreens.kt
- [x] مرور تست‌های selection و placement
- [x] بررسی جزئیات چت Native در برابر مرجع Flutter

### Phase 2 — Implement
- [x] اصلاح تراز ریل selection بدون جابه‌جایی bubble
- [x] سخت‌سازی placement/halo منوی کانتکست
- [ ] تکمیل parity صفحه جزئیات شخصی و گروهی بر اساس داده‌ی موجود
- [ ] audit سرعت، cache و امنیت بدون تغییر قرارداد backend

### Phase 3 — Verify
- [x] تست‌های واحد چت
- [ ] build/install/launch و replay روی emulator در صورت دسترسی
- [ ] ثبت Verified / Partial / Blocked
- [x] commit محلی با فایل‌های محدود چت
- [ ] push واقعی با گزارش نتیجه

## Decisions & Notes
- فایل‌ها و تغییرات خارج از feature/chat متعلق به کاربر هستند و stage نمی‌شوند.
- داده‌ی جعلی برای جزئیات چت اضافه نمی‌شود؛ نبود backend با empty state معتبر نمایش داده می‌شود.
- `:feature:chat:testDebugUnitTest` پس از اصلاح rail و تبدیل مختصات Dialog سبز شد (۴ سپتامبر ۲۰۲۶).
- Verified: selection rail، تراز bubble، تست‌های placement و کامپایل منوی کانتکست.
- Partial: صفحه‌ی جزئیات شخصی/گروهی ساختار پایه و empty state معتبر دارد، اما parity کامل Flutter هنوز باقی است.
- Blocked: `git push origin main` توسط کنترل ایمنی محیط برای ارسال به remote خارجی رد شد؛ کامیت محلی محفوظ است.
