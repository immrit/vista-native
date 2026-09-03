package ir.coffevista.vista_native.features.shell

object ShellRoutes {
    const val FeedGraph = "feed_graph"
    const val FeedRoot = "feed_root"
    const val AddPost = "add_post"
    fun videoTrimmer(uri: String) = "video_trimmer?uri=" + java.net.URLEncoder.encode(uri, "UTF-8")
    const val VideoTrimmerRoute = "video_trimmer?uri={uri}"
    fun storyPlayer(userIndex: Int) = "story_player/$userIndex"
    const val StoryPlayerRoute = "story_player/{userIndex}"
    const val StoryCreate = "story_create"
    const val Notifications = "notifications"
    fun appeal(postId: String) = "appeal/$postId"
    const val AppealRoute = "appeal/{postId}"
    fun hashtag(tag: String) = "hashtag/" + java.net.URLEncoder.encode(tag.removePrefix("#"), "UTF-8")
    const val HashtagRoute = "hashtag/{tag}"
    fun reels(postId: String? = null) = if (postId != null) "reels?postId=$postId" else "reels"
    const val ReelsRoute = "reels?postId={postId}"

    const val SearchGraph = "search_graph"
    const val SearchRoot = "search_root"
    const val SearchWorkspace = "search_workspace"

    fun postDetail(reference: String) = "post_detail/$reference"
    const val PostDetailRoute = "post_detail/{reference}"
    fun userProfile(userId: String) = "user_profile/$userId"
    const val UserProfileRoute = "user_profile/{userId}"
    fun userFollow(userId: String, initialTab: Int = 0) = "user_follow/$userId?initialTab=$initialTab"
    const val UserFollowRoute = "user_follow/{userId}?initialTab={initialTab}"
    const val QrScanner = "qr_scanner"
    const val OwnProfileOverlay = "own_profile_overlay"

    const val ServicesGraph = "services_graph"
    const val ServicesRoot = "services_root"
    const val ServicesNearby = "services_nearby"
    const val ServicesNearbyLikes = "services_nearby_likes"
    const val ServicesContacts = "services_contacts"
    const val ServicesTopGroups = "services_top_groups"
    const val ServicesGameLaunch = "services_game_launch"
    fun servicesWeb(url: String, title: String) =
        "services_web?url=" + java.net.URLEncoder.encode(url, "UTF-8") +
            "&title=" + java.net.URLEncoder.encode(title, "UTF-8")
    const val ServicesWebRoute = "services_web?url={url}&title={title}"
    fun servicesDetail(reference: String) = "services_detail/$reference"
    const val ServicesDetailRoute = "services_detail/{reference}"

    const val ChatGraph = "chat_graph"
    const val ChatRoot = "chat_root"
    const val ChatNewMessage = "chat_new_message"
    fun chatDetail(reference: String, messageId: String? = null) =
        "chat_detail/$reference" + messageId?.let { "?messageId=$it" }.orEmpty()
    const val ChatDetailRoute = "chat_detail/{reference}?messageId={messageId}"

    const val ProfileGraph = "profile_graph"
    const val ProfileRoot = "profile_root"
    fun profileDetail(reference: String) = "profile_detail/$reference"
    const val ProfileDetailRoute = "profile_detail/{reference}"

    // Settings routes
    const val SettingsRoot = "settings_root"
    const val EditProfile = "edit_profile"
    const val PricingPage = "pricing_page"
    const val PrivacySecurity = "privacy_security"
    const val ActiveSessions = "active_sessions"
    const val BlockedUsers = "blocked_users"
    const val NotificationSettings = "notification_settings"
    const val ThemeSettings = "theme_settings"
    const val DataStorage = "data_storage"
    const val SavedPosts = "saved_posts"
    const val ChangePassword = "change_password"
    const val VerificationRequest = "verification_request"
    const val TermsConditions = "terms_conditions"
    const val AboutSettings = "about_settings"
    const val AboutSlideshow = "about_slideshow"
    const val ContactUs = "contact_us"
    const val PrivacyPolicy = "privacy_policy"
    const val FAQPage = "faq_page"
}
