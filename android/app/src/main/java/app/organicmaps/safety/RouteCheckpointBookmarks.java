package app.organicmaps.safety;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.Bookmark;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkInfo;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;

/**
 * Renders route checkpoints as real Organic Maps bookmarks.
 *
 * They are intentionally temporary: unlike a decorative overlay, bookmarks are native map
 * objects, can be tapped, and open the normal place page with coordinates and checkpoint data.
 */
public final class RouteCheckpointBookmarks
{
  private static final String CATEGORY = "__AreaMap_Current_Route_Checkpoints__";

  private RouteCheckpointBookmarks() {}

  public static void show(@NonNull Context context, @NonNull TripPlan plan)
  {
    clear();

    if (plan.checkpoints.isEmpty())
      return;

    final BookmarkManager manager = BookmarkManager.INSTANCE;
    final long categoryId = manager.createCategory(CATEGORY);

    int number = 1;
    for (TripPlan.Checkpoint checkpoint : plan.checkpoints)
    {
      final Bookmark bookmark = manager.addNewBookmark(checkpoint.lat, checkpoint.lon);
      if (bookmark == null)
        continue;

      bookmark.setCategoryId(categoryId);
      bookmark.setIconColor(Color.rgb(66, 242, 123));

      final BookmarkInfo info = manager.getBookmarkInfo(bookmark.getBookmarkId());
      if (info != null)
      {
        final String title = context.getString(R.string.areamap_checkpoint_map_title, number);
        final String description = context.getString(
            R.string.areamap_checkpoint_map_description,
            checkpoint.distanceMeters / 1000.0,
            Math.max(1, checkpoint.etaSeconds / 60),
            checkpoint.altitudeMeters,
            checkpoint.lat,
            checkpoint.lon);
        info.update(title, null, description);
      }
      number++;
    }
  }

  public static void clear()
  {
    final BookmarkManager manager = BookmarkManager.INSTANCE;
    for (BookmarkCategory category : manager.getCategories())
    {
      if (CATEGORY.equals(category.getName()))
      {
        manager.deleteCategory(category.getId());
        break;
      }
    }
  }
}
