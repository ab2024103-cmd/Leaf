package app.leaf.reader.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.leaf.reader.R

/** The five top-level destinations (§3, §5). Library is the default. */
enum class LeafDestination(
    val route: String,
    @param:StringRes val titleRes: Int,
    @param:DrawableRes val icon: Int
) {
    LIBRARY("library", R.string.nav_library, R.drawable.ic_nav_library),
    RECENTS("recents", R.string.nav_recents, R.drawable.ic_nav_recents),
    FAVORITES("favorites", R.string.nav_favorites, R.drawable.ic_nav_favorites),
    SEARCH("search", R.string.nav_search, R.drawable.ic_nav_search),
    SETTINGS("settings", R.string.nav_settings, R.drawable.ic_nav_settings);

    companion object {
        /** Library is the default destination, so an unknown route falls back to it. */
        fun fromRoute(route: String?): LeafDestination =
            entries.firstOrNull { it.route == route } ?: LIBRARY
    }
}
