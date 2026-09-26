package app.organicmaps.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Stores only the latest known fix for the offline SOS card.
 *
 * There is intentionally no inactivity detector, trip check-in, return timer or automatic rescue notification here.
 */
public final class TripSafety
{
  private static TripSafety sInstance;
  private final Context mContext;
  private final SharedPreferences mPrefs;

  private TripSafety(@NonNull Context context)
  {
    mContext = context.getApplicationContext();
    mPrefs = mContext.getSharedPreferences("areamap_sos", Context.MODE_PRIVATE);
  }

  @NonNull
  public static synchronized TripSafety get(@NonNull Context context)
  {
    if (sInstance == null)
      sInstance = new TripSafety(context);
    return sInstance;
  }

  public void onServiceStarted() {}
  public void onServiceStopped() {}
  public void onLocationUnavailable() {}

  public void onLocation(@NonNull Location location)
  {
    if (!location.hasAccuracy())
      return;
    save(location);
  }

  public void save(@NonNull Location location)
  {
    mPrefs.edit()
        .putString("lat", Double.toString(location.getLatitude()))
        .putString("lon", Double.toString(location.getLongitude()))
        .putFloat("accuracy", location.getAccuracy())
        .putLong("measured", location.getTime())
        .apply();
  }

  @NonNull
  public String coordinates()
  {
    if (!mPrefs.contains("lat"))
      return mContext.getString(R.string.areamap_no_fix);

    final String coordinates =
        String.format(Locale.US, "%.6f, %.6f", Double.parseDouble(mPrefs.getString("lat", "0")),
                      Double.parseDouble(mPrefs.getString("lon", "0")));
    final long measured = mPrefs.getLong("measured", 0);
    return mContext.getString(R.string.areamap_fix, coordinates, Math.round(mPrefs.getFloat("accuracy", 0)),
                              DateFormat.getDateTimeInstance().format(new Date(measured)));
  }

  @NonNull
  public String card()
  {
    return mContext.getString(R.string.areamap_card, coordinates());
  }
}
