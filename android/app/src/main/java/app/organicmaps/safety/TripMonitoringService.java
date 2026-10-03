package app.organicmaps.safety;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import app.organicmaps.R;
import app.organicmaps.sdk.location.LocationUtils;

/** Trip/GPX monitoring has its own lifetime, independent of the map and track-recording screens. */
public final class TripMonitoringService extends Service implements LocationListener
{
  private static final String CHANNEL = "AREAMAP_ACTIVE_HIKE";
  private static final int NOTIFICATION = 71844;
  private static final String SOS = "AREAMAP_SEND_SOS";
  @Nullable
  private LocationManager mLocationManager;

  public static boolean needed(Context context)
  {
    final boolean hasGpx = GpxNavigation.restore(context) != null;
    return TripSafety.get(context).hasActiveTrip() || hasGpx;
  }

  public static void start(Context context)
  {
    if (needed(context) && LocationUtils.checkFineLocationPermission(context))
      ContextCompat.startForegroundService(context, new Intent(context, TripMonitoringService.class));
  }

  public static void stopIfUnused(Context context)
  {
    if (!needed(context))
      context.stopService(new Intent(context, TripMonitoringService.class));
  }

  public static void createNotificationChannel(Context context)
  {
    NotificationManagerCompat.from(context).createNotificationChannel(
        new NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(context.getString(R.string.areamap_monitor_channel))
            .setVibrationEnabled(false)
            .build());
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent)
  {
    return null;
  }

  @Override
  public void onCreate()
  {
    super.onCreate();
    createNotificationChannel(this);
  }

  @Override
  public int onStartCommand(@Nullable Intent intent, int flags, int startId)
  {
    if (intent != null && SOS.equals(intent.getAction()))
    {
      final TripSafety safety = TripSafety.get(this);
      TripReportSender.enqueue(this, "sos", safety.tripId(), safety.sosReport());
    }
    if (!needed(this) || !LocationUtils.checkFineLocationPermission(this))
    {
      stopSelf();
      return START_NOT_STICKY;
    }
    try
    {
      ServiceCompat.startForeground(
          this, NOTIFICATION, notification(),
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION : 0);
      // A sticky restart can receive a null intent. It never registers or sends a start report.
      if (mLocationManager == null)
      {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (mLocationManager != null)
        {
          if (mLocationManager.getAllProviders().contains(LocationManager.GPS_PROVIDER))
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 2, this);
          if (mLocationManager.getAllProviders().contains(LocationManager.NETWORK_PROVIDER))
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 5, this);
        }
      }
      return START_STICKY;
    }
    catch (SecurityException e)
    {
      // The OS can revoke location permission while a hike is saved. Keep its data for reopening.
      stopSelf();
      return START_NOT_STICKY;
    }
  }

  private android.app.Notification notification()
  {
    final int immutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
    final PendingIntent open = PendingIntent.getActivity(
        this, NOTIFICATION, new Intent(this, TripSafetyActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT | immutable);
    final PendingIntent sos =
        PendingIntent.getService(this, NOTIFICATION + 1, new Intent(this, TripMonitoringService.class).setAction(SOS),
                                 PendingIntent.FLAG_UPDATE_CURRENT | immutable);
    final TripSafety safety = TripSafety.get(this);
    final GpxNavigation gpx = GpxNavigation.current;
    final String body = safety.hasActiveTrip()    ? safety.navigationReturnSummary()
                      : gpx != null && gpx.hasFix ? getString(R.string.areamap_track_progress, gpx.remaining / 1000.0,
                                                              gpx.seconds / 3600, gpx.seconds / 60 % 60)
                                                  : getString(R.string.areamap_track_waiting);
    return new NotificationCompat.Builder(this, CHANNEL)
        .setSmallIcon(app.organicmaps.branding.R.drawable.ic_splash)
        .setContentTitle(getString(R.string.areamap_monitor_title))
        .setContentText(body)
        .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(open)
        .addAction(0, getString(R.string.areamap_sos), sos)
        .build();
  }

  @Override
  public void onLocationChanged(@NonNull Location location)
  {
    TripSafety.get(this).onLocation(location);
    final GpxNavigation gpx = GpxNavigation.current;
    if (gpx != null)
    {
      gpx.update(location);
      gpx.saveProgress(this);
    }
    // NotificationManager handles user-disabled channels; do not require permission for monitoring itself.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
               == android.content.pm.PackageManager.PERMISSION_GRANTED)
      NotificationManagerCompat.from(this).notify(NOTIFICATION, notification());
  }

  @Override
  public void onProviderEnabled(@NonNull String provider)
  {}
  @Override
  public void onProviderDisabled(@NonNull String provider)
  {}
  @Override
  public void onStatusChanged(String provider, int status, Bundle extras)
  {}

  @Override
  public void onDestroy()
  {
    if (mLocationManager != null)
      mLocationManager.removeUpdates(this);
    super.onDestroy();
  }
}
