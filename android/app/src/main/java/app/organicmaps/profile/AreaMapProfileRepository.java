package app.organicmaps.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import app.organicmaps.R;
import app.organicmaps.safety.TripSafety;
import app.organicmaps.sdk.bookmarks.data.BookmarkCategory;
import app.organicmaps.sdk.bookmarks.data.BookmarkManager;
import app.organicmaps.sdk.downloader.MapManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Aggregates existing SDK data without treating saved tracks as completed hikes. */
public final class AreaMapProfileRepository
{
  private final Context mContext;
  private final SharedPreferences mPrefs;

  public static final class Statistics
  {
    public final int peaks, trips, routes, offlineRegions;
    public Statistics(int peaks, int trips, int routes, int offlineRegions)
    {
      this.peaks = peaks;
      this.trips = trips;
      this.routes = routes;
      this.offlineRegions = offlineRegions;
    }
  }

  public AreaMapProfileRepository(Context context)
  {
    mContext = context.getApplicationContext();
    mPrefs = mContext.getSharedPreferences("areamap_profile", Context.MODE_PRIVATE);
  }

  public void addListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
  { mPrefs.registerOnSharedPreferenceChangeListener(listener); }

  public void removeListener(SharedPreferences.OnSharedPreferenceChangeListener listener)
  { mPrefs.unregisterOnSharedPreferenceChangeListener(listener); }

  public AreaMapUserProfile profile()
  {
    return new AreaMapUserProfile(mPrefs.getString("name", ""), mPrefs.getString("bio", ""),
        mPrefs.getString("region", ""), mPrefs.getString("avatar", ""), mPrefs.getLong("created_at", 0));
  }

  public void save(String name, String bio, String region)
  {
    mPrefs.edit().putString("name", name.trim()).putString("bio", bio.trim()).putString("region", region.trim())
        .putLong("created_at", mPrefs.getLong("created_at", System.currentTimeMillis())).apply();
  }

  public Statistics statistics()
  {
    int tracks = 0;
    for (BookmarkCategory category : BookmarkManager.INSTANCE.getCategories()) tracks += category.getTracksCount();
    // TripSafety retains only the active/last plan. Neither summit completion nor trip history exists.
    return new Statistics(0, 0, tracks, MapManager.nativeGetDownloadedCount());
  }

  public List<Achievement> achievements()
  {
    final Statistics stats = statistics();
    final TripSafety.Profile safety = TripSafety.get(mContext).getProfile();
    final int preparation = (!safety.emergencyName.isEmpty() && !safety.emergencyPhone.isEmpty() ? 1 : 0)
        + (stats.offlineRegions > 0 ? 1 : 0);
    long unlocked = mPrefs.getLong("safe_hiker_unlocked_at", 0);
    if (preparation == 2 && unlocked == 0)
    {
      unlocked = System.currentTimeMillis();
      mPrefs.edit().putLong("safe_hiker_unlocked_at", unlocked).apply();
    }
    return List.of(
        new Achievement("first_peak", R.string.p_first_peak, R.string.p_peak_desc, R.drawable.profile_badge_peak,
                        "summit_completion", 1, stats.peaks, 0),
        new Achievement("10_hikes", R.string.p_ten_hikes, R.string.p_hikes_desc, R.drawable.profile_badge_hike,
                        "completed_hikes", 10, stats.trips, 0),
        new Achievement("safe_hiker", R.string.p_safe_hiker, R.string.p_safe_desc, R.drawable.profile_badge_safety,
                        "safety_preparation", 2, preparation, unlocked),
        new Achievement("almaty_explorer", R.string.p_explorer, R.string.p_explorer_desc, R.drawable.profile_badge_region,
                        "confirmed_regional_visits", 10, 0, 0));
  }

  /** Copy a bounded, resized image into private storage; no expiring URI or storage permission. */
  public void importAvatar(Uri uri) throws IOException
  {
    final File incoming = File.createTempFile("profile-avatar-input-", ".tmp", mContext.getCacheDir());
    try
    {
      try (InputStream input = mContext.getContentResolver().openInputStream(uri);
           FileOutputStream output = new FileOutputStream(incoming))
      {
        if (input == null) throw new IOException("No image stream");
        byte[] buffer = new byte[8192]; int count, total = 0;
        while ((count = input.read(buffer)) != -1)
        {
          total += count;
          if (total > 20 * 1024 * 1024) throw new IOException("Image too large");
          output.write(buffer, 0, count);
        }
      }
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inJustDecodeBounds = true;
      BitmapFactory.decodeFile(incoming.getPath(), options);
      if (options.outWidth <= 0 || options.outHeight <= 0) throw new IOException("Invalid image");
      options.inSampleSize = 1;
      while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 1024) options.inSampleSize *= 2;
      options.inJustDecodeBounds = false;
      Bitmap bitmap = BitmapFactory.decodeFile(incoming.getPath(), options);
      if (bitmap == null) throw new IOException("Cannot decode image");
      final File destination = File.createTempFile("profile-avatar-", ".jpg", mContext.getFilesDir());
      try (FileOutputStream output = new FileOutputStream(destination))
      {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw new IOException("Cannot save image");
      }
      finally { bitmap.recycle(); }
      final String old = mPrefs.getString("avatar", "");
      mPrefs.edit().putString("avatar", destination.getPath()).apply();
      if (!old.isEmpty()) new File(old).delete();
    }
    finally { incoming.delete(); }
  }
}
