package ir.coffevista.vista_native.features.search.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ir.coffevista.vista_native.features.search.data.SearchHistoryItem
import ir.coffevista.vista_native.features.search.data.SearchHistoryType
import ir.coffevista.vista_native.features.search.data.SearchPost
import ir.coffevista.vista_native.features.search.data.SearchUser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchWorkspaceContentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun initialStateShowsFieldAndHistory() {
        setState(
            SearchUiState(
                history = listOf(
                    SearchHistoryItem("@vista", SearchHistoryType.User, 1L),
                ),
            ),
        )
        compose.onNodeWithTag(SearchTestTags.Field).assertIsDisplayed()
        compose.onNodeWithText("جستجوهای اخیر").assertIsDisplayed()
        compose.onNodeWithText("@vista").assertIsDisplayed()
    }

    @Test
    fun mixedPersianEnglishEmptyStateIsRendered() {
        setState(
            SearchUiState(
                query = "vista ویستا",
                phase = SearchPhase.Empty,
            ),
        )
        compose.onNodeWithText("vista ویستا").assertIsDisplayed()
        compose.onNodeWithTag(SearchTestTags.Empty).assertIsDisplayed()
    }

    @Test
    fun errorRetryInvokesCallbackExactlyOnce() {
        var retryCount = 0
        setState(
            state = SearchUiState(
                query = "#offline",
                phase = SearchPhase.Error,
                selectedTab = SearchTab.Tags,
                errorMessage = "خطای شبکه",
            ),
            onRetry = { retryCount++ },
        )
        compose.onNodeWithTag(SearchTestTags.Error).assertIsDisplayed()
        compose.onNodeWithTag(SearchTestTags.Retry).performClick()
        assertEquals(1, retryCount)
    }

    @Test
    fun peopleTabRendersCanonicalUserRow() {
        setState(
            SearchUiState(
                query = "vista",
                phase = SearchPhase.Content,
                selectedTab = SearchTab.People,
                users = listOf(user("u1")),
                hasMoreUsers = false,
            ),
        )
        compose.onNodeWithTag(SearchTestTags.UserList).assertIsDisplayed()
        compose.onNodeWithTag(SearchTestTags.user("u1")).assertIsDisplayed()
        compose.onNodeWithText("@vista_user").assertIsDisplayed()
    }

    @Test
    fun tagsTabRendersThreeColumnPostGridDestination() {
        setState(
            SearchUiState(
                query = "#vista",
                phase = SearchPhase.Content,
                selectedTab = SearchTab.Tags,
                posts = listOf(SearchPost("p1", null, null)),
            ),
        )
        compose.onNodeWithTag(SearchTestTags.PostGrid).assertIsDisplayed()
        compose.onNodeWithTag(SearchTestTags.post("p1")).assertIsDisplayed()
    }

    private fun setState(
        state: SearchUiState,
        onRetry: () -> Unit = {},
    ) {
        compose.setContent {
            MaterialTheme {
                SearchWorkspaceContent(
                    state = state,
                    onQueryChanged = {},
                    onSubmit = {},
                    onFocusChanged = {},
                    onClearQuery = {},
                    onSelectTab = {},
                    onHistory = {},
                    onDeleteHistory = {},
                    onClearHistory = {},
                    onHashtag = {},
                    onUser = {},
                    onPost = {},
                    onRetry = onRetry,
                    onLoadMoreUsers = {},
                    autoFocus = false,
                )
            }
        }
    }

    private fun user(id: String) = SearchUser(
        id = id,
        username = "vista_user",
        fullName = "کاربر ویستا",
        avatarUrl = null,
        isVerified = true,
        verificationType = "identity",
        role = null,
    )
}
