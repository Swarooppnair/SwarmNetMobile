package com.bitchat.android.mapfeature

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bitchat.android.ui.ChatViewModel

@Composable
fun MapFeatureScreen(chatViewModel: ChatViewModel? = null) {
    var showBookmarks by remember { mutableStateOf(false) }

    if (showBookmarks) {
        BookmarksScreen(
            onBack = { showBookmarks = false }
        )
    } else {
        MapScreen(
            chatViewModel = chatViewModel,
            onNavigateToBookmarks = { showBookmarks = true }
        )
    }
}
