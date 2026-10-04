package app.organicmaps.home;

import android.content.Context;
import android.widget.Toast;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.Bookmark;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkInfo;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;

/** Uses the core's saved places, including externally edited/deleted bookmarks. */
public final class DestinationBookmarks
{
  private DestinationBookmarks() {}

  public static BookmarkInfo find(HikeRecommendation item)
  {
    for (BookmarkCategory category : BookmarkManager.INSTANCE.getCategories())
    {
      if (app.organicmaps.safety.RouteCheckpointBookmarks.isCheckpointCategory(category))
        continue;
      for (long id : category.getBookmarkIds())
      {
        final BookmarkInfo info = BookmarkManager.INSTANCE.getBookmarkInfo(id);
        if (info != null && Math.abs(info.getLat() - item.lat) < 0.00001
            && Math.abs(info.getLon() - item.lon) < 0.00001)
          return info;
      }
    }
    return null;
  }

  public static void toggle(Context context, HikeRecommendation item)
  {
    if (BookmarkManager.INSTANCE.isAsyncBookmarksLoadingInProgress())
    {
      Toast.makeText(context, R.string.destination_bookmarks_loading, Toast.LENGTH_SHORT).show();
      return;
    }
    final BookmarkInfo existing = find(item);
    if (existing != null)
      BookmarkManager.INSTANCE.deleteBookmark(existing.getBookmarkId());
    else
      save(context, item);
  }

  private static boolean save(Context context, HikeRecommendation item)
  {
    final Bookmark bookmark = BookmarkManager.INSTANCE.addNewBookmark(item.lat, item.lon);
    if (bookmark == null)
    {
      Toast.makeText(context, R.string.destination_save_failed, Toast.LENGTH_SHORT).show();
      return false;
    }
    final BookmarkInfo info = BookmarkManager.INSTANCE.getBookmarkInfo(bookmark.getBookmarkId());
    if (info != null)
      info.update(context.getString(item.title), null, info.getDescription());
    return true;
  }

  public static void migrate(Context context, HikeRecommendation item)
  {
    final android.content.SharedPreferences old =
        context.getSharedPreferences("areamap_home_favorites", Context.MODE_PRIVATE);
    if (old.getBoolean(item.id, false) && !BookmarkManager.INSTANCE.isAsyncBookmarksLoadingInProgress()
        && (find(item) != null || save(context, item)))
      old.edit().remove(item.id).apply();
  }
}
