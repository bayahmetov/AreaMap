package app.organicmaps.safety;

import android.content.Context;
import android.graphics.Color;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import app.organicmaps.sdk.bookmarks.data.Bookmark;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkInfo;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.bookmarks.data.MapObject;

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

    if (plan.routeCheckpoints.isEmpty())
      return;

    final BookmarkManager manager = BookmarkManager.INSTANCE;
    final long categoryId = manager.createCategory(CATEGORY);

    int number = 1;
    for (TripPlan.Checkpoint checkpoint : plan.routeCheckpoints)
    {
      final Bookmark bookmark = manager.addNewBookmark(checkpoint.lat, checkpoint.lon);
      if (bookmark == null)
        continue;

      bookmark.setCategoryId(categoryId);
      bookmark.setIconColor(checkpoint.role == 1 ? Color.rgb(241, 94, 104) : Color.rgb(57, 216, 111));

      bookmark.setRouteCheckpointLabel(checkpoint.role < 0   ? context.getString(R.string.route_preview_start_marker)
                                       : checkpoint.role > 0 ? context.getString(R.string.route_preview_finish_marker)
                                                             : Integer.toString(number));

      final BookmarkInfo info = manager.getBookmarkInfo(bookmark.getBookmarkId());
      if (info != null)
      {
        final String label = checkpoint.role < 0 ? context.getString(R.string.areamap_route_start)
                           : checkpoint.role > 0 ? context.getString(R.string.areamap_route_finish)
                                                 : context.getString(R.string.areamap_checkpoint_map_title, number);
        final String title = checkpoint.title.isEmpty() ? label : label + " — " + checkpoint.title;
        final String description = context.getString(
            R.string.areamap_checkpoint_map_description, checkpoint.distanceMeters / 1000.0,
            Math.max(1, checkpoint.etaSeconds / 60), checkpoint.altitudeMeters, checkpoint.lat, checkpoint.lon);
        info.update(title, null, description);
      }
      if (checkpoint.role == 0)
        number++;
    }
  }

  public static boolean isCheckpoint(@NonNull MapObject object)
  {
    if (!(object instanceof Bookmark bookmark))
      return false;

    final BookmarkCategory category = BookmarkManager.INSTANCE.getCategoryById(bookmark.getCategoryId());
    return isCheckpointCategory(category);
  }

  public static boolean isCheckpointCategory(@androidx.annotation.Nullable BookmarkCategory category)
  {
    return category != null && CATEGORY.equals(category.getName());
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
