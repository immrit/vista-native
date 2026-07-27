package ir.coffevista.vista_native.features.shell

object ShellRoutes {
    const val FeedGraph = "feed_graph"
    const val FeedRoot = "feed_root"
    fun feedDetail(reference: String) = "feed_detail/$reference"
    const val FeedDetailRoute = "feed_detail/{reference}"

    const val SearchGraph = "search_graph"
    const val SearchRoot = "search_root"
    fun searchDetail(reference: String) = "search_detail/$reference"
    const val SearchDetailRoute = "search_detail/{reference}"

    const val ServicesGraph = "services_graph"
    const val ServicesRoot = "services_root"
    fun servicesDetail(reference: String) = "services_detail/$reference"
    const val ServicesDetailRoute = "services_detail/{reference}"

    const val ChatGraph = "chat_graph"
    const val ChatRoot = "chat_root"
    fun chatDetail(reference: String) = "chat_detail/$reference"
    const val ChatDetailRoute = "chat_detail/{reference}"

    const val ProfileGraph = "profile_graph"
    const val ProfileRoot = "profile_root"
    fun profileDetail(reference: String) = "profile_detail/$reference"
    const val ProfileDetailRoute = "profile_detail/{reference}"
}
