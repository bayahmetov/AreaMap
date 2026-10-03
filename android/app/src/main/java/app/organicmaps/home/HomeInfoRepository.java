package app.organicmaps.home;

import android.content.Context;
import app.organicmaps.R;
import app.organicmaps.sdk.downloader.CountryItem;
import app.organicmaps.sdk.downloader.MapManager;
import java.util.ArrayList;
import java.util.List;

/** HOME offline status uses the existing map downloader. */
public final class HomeInfoRepository
{
  private HomeInfoRepository() {}

  public static String offlineStatus(Context context)
  {
    final int count = MapManager.nativeGetDownloadedCount();
    if (count == 0)
      return context.getString(R.string.home_offline_empty);
    final List<CountryItem> regions = new ArrayList<>();
    MapManager.nativeListItems(null, 0, 0, false, true, regions);
    for (CountryItem region : regions)
    {
      if (region.present && region.name != null && !region.name.isEmpty())
        return context.getString(R.string.home_offline_region, region.name);
    }
    return context.getString(R.string.home_offline_count, count);
  }
}
