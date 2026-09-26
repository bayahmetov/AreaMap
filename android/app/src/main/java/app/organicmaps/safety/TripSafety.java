package app.organicmaps.safety;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import app.organicmaps.R;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Main-thread owner of the local trip; location and lifetime belong to TrackRecordingService. */
public final class TripSafety
{
  private static final String CHANNEL = "areamap_check_in";
  private static final int NOTIFICATION_ID = 54322;
  private static TripSafety sInstance;
  private final Context mContext;
  private final SharedPreferences mPrefs;
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private SafetyEngine mEngine;
  private boolean mServiceRunning;
  private SafetyEngine.State mNotifiedState;
  private final Runnable mTick = new Runnable() {
    @Override
    public void run()
    {
      updateNotification();
      if (mServiceRunning)
        mHandler.postDelayed(this, 10_000);
    }
  };

  private TripSafety(Context context)
  {
    mContext = context.getApplicationContext();
    mPrefs = mContext.getSharedPreferences("areamap_trip", Context.MODE_PRIVATE);
    NotificationManagerCompat.from(mContext).createNotificationChannel(
        new NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(mContext.getString(R.string.areamap_check_channel))
            .setVibrationEnabled(true)
            .build());
  }

  public static TripSafety get(Context context)
  {
    if (sInstance == null)
      sInstance = new TripSafety(context);
    return sInstance;
  }

  public boolean active()
  {
    return mPrefs.getBoolean("active", false);
  }
  public boolean running()
  {
    return active() && mEngine != null && mServiceRunning;
  }
  public String route()
  {
    return mPrefs.getString("route", "");
  }
  public String group()
  {
    return mPrefs.getString("group", "1");
  }
  public String contact()
  {
    return mPrefs.getString("contact", "");
  }

  public String hours()
  {
    return Long.toString((mPrefs.getLong("return", 0) - mPrefs.getLong("started", 0)) / 3_600_000L);
  }

  public void start(String route, String group, String contact, int hours)
  {
    mPrefs.edit()
        .clear()
        .putBoolean("active", true)
        .putString("route", route)
        .putString("group", group)
        .putString("contact", contact)
        .putLong("started", System.currentTimeMillis())
        .putLong("return", System.currentTimeMillis() + hours * 3_600_000L)
        .apply();
    resume();
  }

  public void resume()
  {
    // Do not infer immobility across a process/service interruption.
    mEngine = new SafetyEngine(15 * 60_000L, 2 * 60_000L);
    clearNotification();
  }

  public void finish()
  {
    mPrefs.edit().putBoolean("active", false).apply();
    mEngine = null;
    clearNotification();
  }

  public void onServiceStarted()
  {
    mServiceRunning = true;
    mHandler.removeCallbacks(mTick);
    mHandler.post(mTick);
  }

  public void onServiceStopped()
  {
    mServiceRunning = false;
    mEngine = null;
    mHandler.removeCallbacks(mTick);
    clearNotification();
  }

  public void onLocation(Location location)
  {
    if (!active() || mEngine == null || !location.hasAccuracy())
      return;
    final long now = SystemClock.elapsedRealtime();
    if (!mEngine.accept(location.getLatitude(), location.getLongitude(), location.getAccuracy(),
                        location.getElapsedRealtimeNanos() / 1_000_000, now))
      return;
    // The complete track is saved by Organic Maps; this snapshot is for the offline SOS card.
    mPrefs.edit()
        .putString("lat", Double.toString(location.getLatitude()))
        .putString("lon", Double.toString(location.getLongitude()))
        .putFloat("accuracy", location.getAccuracy())
        .putLong("measured", location.getTime())
        .apply();
    updateNotification();
  }

  public void onLocationUnavailable()
  {
    if (mEngine != null)
      mEngine.locationUnavailable();
  }

  public void acknowledge()
  {
    if (mEngine != null)
      mEngine.acknowledge();
    clearNotification();
  }

  public void rest()
  {
    if (mEngine != null)
      mEngine.rest(SystemClock.elapsedRealtime(), 15 * 60_000L);
    clearNotification();
  }

  public int status()
  {
    if (!active())
      return R.string.areamap_idle;
    if (!running())
      return R.string.areamap_interrupted;
    return stateText(mEngine.state(SystemClock.elapsedRealtime()));
  }

  public static int stateText(SafetyEngine.State state)
  {
    switch (state)
    {
    case NO_LOCATION: return R.string.areamap_no_location;
    case RESTING: return R.string.areamap_resting;
    case CHECK_IN: return R.string.areamap_check_in;
    case UNANSWERED: return R.string.areamap_unanswered;
    default: return R.string.areamap_monitoring;
    }
  }

  public String coordinates()
  {
    if (!mPrefs.contains("lat"))
      return mContext.getString(R.string.areamap_no_fix);
    final long measured = mPrefs.getLong("measured", 0);
    final long age = System.currentTimeMillis() - measured;
    final String ageText = age < 0 ? mContext.getString(R.string.areamap_clock_changed)
                                   : mContext.getString(R.string.areamap_age, age / 60_000);
    return mContext.getString(R.string.areamap_fix,
                              String.format(Locale.US, "%.6f, %.6f", Double.parseDouble(mPrefs.getString("lat", "0")),
                                            Double.parseDouble(mPrefs.getString("lon", "0"))),
                              Math.round(mPrefs.getFloat("accuracy", 0)),
                              DateFormat.getDateTimeInstance().format(new Date(measured)), ageText);
  }

  public String card()
  {
    final String due = mPrefs.contains("return")
                         ? DateFormat.getDateTimeInstance().format(new Date(mPrefs.getLong("return", 0)))
                         : "—";
    return mContext.getString(R.string.areamap_card, route(), group(), contact(), due, coordinates(),
                              mContext.getString(status()));
  }

  private void clearNotification()
  {
    NotificationManagerCompat.from(mContext).cancel(NOTIFICATION_ID);
    mNotifiedState = null;
  }

  @android.annotation.SuppressLint("MissingPermission")
  private void updateNotification()
  {
    if (!running())
      return;
    final SafetyEngine.State state = mEngine.state(SystemClock.elapsedRealtime());
    if ((state != SafetyEngine.State.CHECK_IN && state != SafetyEngine.State.UNANSWERED) || state == mNotifiedState)
      return;
    final NotificationManagerCompat manager = NotificationManagerCompat.from(mContext);
    if (!manager.areNotificationsEnabled())
      return;
    final int immutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    final PendingIntent open =
        PendingIntent.getActivity(mContext, NOTIFICATION_ID, new Intent(mContext, TripSafetyActivity.class),
                                  PendingIntent.FLAG_UPDATE_CURRENT | immutable);
    manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(mContext, CHANNEL)
                                        .setSmallIcon(R.drawable.ic_track_recording_off)
                                        .setContentTitle(mContext.getString(stateText(state)))
                                        .setContentText(mContext.getString(R.string.areamap_notification_body))
                                        .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                            mContext.getString(R.string.areamap_notification_body)))
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                                        .setContentIntent(open)
                                        .setAutoCancel(false)
                                        .build());
    mNotifiedState = state;
  }
}
