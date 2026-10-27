# Attachment Sheet, Telegram Image Blur Download & Post Creation Overhaul

**Type:** feature | bug | ui
**Status:** completed
**Progress:** `[██████████] 100%`

## Problem / Goal
1. **Attachment Sheet Cleanup**:
   - Non-functional actions ("ارسال موقعیت" and "ارسال مخاطب") removed from the bottom sheet.
   - "موسیقی" merged into "فایل" (فایل و موسیقی: اسناد، صوت، PDF).
   - Only 3 clean, fully working options remain: گالری (عکس و ویدیو), فایل و موسیقی (اسناد و صوت), دوربین (عکاسی).

2. **Telegram-Grade Image Download Blur UX**:
   - Un-downloaded incoming images render with an authentic heavy blur preview (`Modifier.blur(imageBlurRadius)`) and dark scrim overlay. Centered circular Telegram download button (`↓` + file size badge) prompts the download.
   - Tapping initiates download (`mediaDownloader.start`), showing progress spinner (`✕` to cancel).
   - Upon download completion (`DownloadState.COMPLETE`), the local file loads, blur smoothly animates from 25.dp to 0.dp over 400ms, the download button and scrim vanish, revealing the crisp local photo. Tapping opens the fullscreen viewer.

3. **Post Creation Screen Overhaul (`AddPostScreen`, `AddPostViewModel`, `PostMediaUploader`)**:
   - **Video Posting**: Immediately auto-caches picked video to local MP4 cache file so `selectedVideo` is never null, accepts trimmed video from `SavedStateHandle` ("trimmed_video_path") reactively, and ensures video is uploaded and linked with thumbnail on `submitPost()`.
   - **Music Posting**: Added fallback duration (30s) if `MediaMetadataRetriever` returns 0; hardened `PostMediaUploader.uploadAudio` with extension-based MIME fallback (.mp3, .m4a, .ogg, .wav, .aac, .flac); added standalone music card when audio is picked without images.
   - **Image Orientation & Carousel**: Fixed EXIF orientation handling in `PostMediaUploader.compressImageToJpeg`; added large primary preview box honoring selected `aspectRatio` with floating music overlay pill and thumbnail strip below.

## Phases

### Phase 1 — Investigate & Design
- [x] Trace attachment bottom sheet options in `ChatAttachmentBottomSheet.kt`
- [x] Trace image download & Coil blur lifecycle in `ChatScreens.kt:AttachmentBubble`
- [x] Analyze `AddPostScreen.kt`, `AddPostViewModel.kt`, and `PostMediaUploader.kt` for video, music, and image posting failures
- [x] Compare with Flutter implementation in `E:\vista\lib\features\posts\screens\AddPost.dart`

### Phase 2 — Implement
- [x] Clean up `ChatAttachmentBottomSheet.kt`: Removed location & contact, merged music into file picker, leaving 3 prominent Telegram-gradient tiles (گالری, فایل و موسیقی, دوربین).
- [x] Implement Telegram-exact image blur download lifecycle in `ChatScreens.kt:AttachmentBubble`:
  - Heavy animated blur + scrim for un-downloaded incoming images.
  - Centered circular download button with file size.
  - Smooth de-blur transition to crisp local image once downloaded.
- [x] Overhaul `PostMediaUploader.kt`:
  - Robust audio MIME detection fallback by file extension.
  - Preserve EXIF orientation in image compression.
  - Support both `File` and `Uri` video uploading with reliable thumbnail generation.
- [x] Overhaul `AddPostViewModel.kt`:
  - Auto-cache selected video `Uri` to local MP4 file immediately on selection.
  - Handle trimmer result via `SavedStateHandle` without losing state.
  - Fallback duration for audio when retriever returns 0.
  - Ensure video, music, and image uploads are properly triggered and linked in `CreatePostRequestDto`.
- [x] Overhaul `AddPostScreen.kt`:
  - Render video preview card whenever `isVideo` is true (even before trimming), with play icon, duration, and optional re-trim button.
  - Render standalone music card if music is selected without images.
  - Improve aspect ratio selection and multi-image carousel.

### Phase 3 — Verify
- [x] Compile and run `:feature:chat:testDebugUnitTest` and `:feature:feed:testDebugUnitTest` (ALL PASSED).
- [x] Install APK on emulator (`emulator-5554`) via `:app:installProductionDebug`.
- [x] Live test: Open chat attachment sheet, verified 3 clean tiles (گالری, فایل و موسیقی, دوربین).
- [x] Live test: Verified incoming image blur preview and tap-to-download lifecycle.
- [x] Live test: Open AddPostScreen, verified composer layout and media sections.
- [x] Assemble production release APK via `:app:assembleProductionRelease` -> `E:\vista_native\vista-release.apk`.
