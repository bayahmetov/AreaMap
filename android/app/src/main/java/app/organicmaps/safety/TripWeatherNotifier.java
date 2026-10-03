package app.organicmaps.safety;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Builds weather summaries and high-priority local warnings for an active hike. */
public final class TripWeatherNotifier
{
  private static final String CHANNEL_ID = "AREAMAP_TRIP_WEATHER";
  private static final int NOTIFICATION_ID = 71843;
  private static final long LOOKAHEAD_MS = 2 * 60 * 60 * 1000L;

  private TripWeatherNotifier() {}

  public static void createNotificationChannel(@NonNull Context context)
  {
    final NotificationChannelCompat channel =
        new NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.areamap_weather_channel))
            .setDescription(context.getString(R.string.areamap_weather_channel_description))
            .setVibrationEnabled(true)
            .build();
    NotificationManagerCompat.from(context).createNotificationChannel(channel);
  }

  public static void evaluate(@NonNull Context context, @NonNull TripSafety safety)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ActivityCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PERMISSION_GRANTED)
      return;
    if (!NotificationManagerCompat.from(context).areNotificationsEnabled())
      return;
    if (!safety.hasActiveTrip())
      return;
    if (!TripWeatherRepository.hasForecastForPoint(context, safety.weatherLat(), safety.weatherLon(),
                                                    safety.weatherAltitudeMeters()))
      return;

    final long now = System.currentTimeMillis();
    final long target = safety.weatherTargetAtMillis();
    if (target <= 0L || target - now > LOOKAHEAD_MS || now - target > 30 * 60 * 1000L)
      return;

    final long from = Math.max(now, target - 60 * 60 * 1000L);
    final long end = target + 60 * 60 * 1000L;
    final TripWeatherRepository.Hour hazard =
        TripWeatherRepository.firstHazard(context, from, end);
    if (hazard == null || !TripWeatherRepository.markAlertIfNew(context, hazard))
      return;

    post(context, context.getString(R.string.areamap_weather_alert_title),
         hazardText(context, safety, hazard));
  }

  @NonNull
  public static String summary(@NonNull Context context, @NonNull TripSafety safety)
  {
    if (!safety.hasActiveTrip())
      return context.getString(R.string.areamap_weather_no_trip);

    if (!TripWeatherRepository.hasForecastForPoint(context, safety.weatherLat(), safety.weatherLon(),
                                                    safety.weatherAltitudeMeters()))
      return context.getString(R.string.areamap_weather_no_cache);

    final TripWeatherRepository.Hour hour =
        TripWeatherRepository.closestHour(context, safety.weatherTargetAtMillis());
    if (hour == null)
      return context.getString(R.string.areamap_weather_no_cache);

    final String point = safety.weatherAtHighestPoint()
        ? context.getString(R.string.areamap_weather_highest_point, safety.weatherAltitudeMeters())
        : context.getString(R.string.areamap_weather_destination);
    final String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(hour.timeMillis));
    final long fetchedAt = TripWeatherRepository.fetchedAt(context);
    final String updated = fetchedAt <= 0 ? context.getString(R.string.areamap_weather_never_updated)
        : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(fetchedAt));
    return context.getString(R.string.areamap_weather_summary, point, time, weatherLabel(context, hour.weatherCode),
                             hour.temperatureC, hour.precipitationProbability, hour.gustKmh, updated);
  }

  @NonNull
  public static String summaryForPlan(@NonNull Context context, @NonNull TripPlan plan)
  {
    if (!TripWeatherRepository.hasForecastForPoint(context, plan.weatherLat, plan.weatherLon,
                                                    plan.weatherAltitudeMeters))
      return context.getString(R.string.areamap_weather_no_cache);

    final long targetMillis = System.currentTimeMillis() + plan.weatherEtaSeconds * 1000L;
    final TripWeatherRepository.Hour hour = TripWeatherRepository.closestHour(context, targetMillis);
    if (hour == null)
      return context.getString(R.string.areamap_weather_no_cache);

    final String point = plan.weatherAtHighestPoint
        ? context.getString(R.string.areamap_weather_highest_point, plan.weatherAltitudeMeters)
        : context.getString(R.string.areamap_weather_destination);
    final String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(hour.timeMillis));
    final long fetchedAt = TripWeatherRepository.fetchedAt(context);
    final String updated = fetchedAt <= 0 ? context.getString(R.string.areamap_weather_never_updated)
        : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(fetchedAt));
    return context.getString(R.string.areamap_weather_summary, point, time, weatherLabel(context, hour.weatherCode),
                             hour.temperatureC, hour.precipitationProbability, hour.gustKmh, updated);
  }

  @NonNull
  private static String hazardText(@NonNull Context context, @NonNull TripSafety safety,
                                   @NonNull TripWeatherRepository.Hour hour)
  {
    final String point = safety.weatherAtHighestPoint()
        ? context.getString(R.string.areamap_weather_highest_point, safety.weatherAltitudeMeters())
        : context.getString(R.string.areamap_weather_destination);
    final String time = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(hour.timeMillis));
    final String kind = TripWeatherRepository.hazardKind(hour);
    if ("storm".equals(kind))
      return context.getString(R.string.areamap_weather_alert_storm, point, time);
    if ("wind".equals(kind))
      return context.getString(R.string.areamap_weather_alert_wind, point, time, hour.gustKmh);
    if ("ice".equals(kind))
      return context.getString(R.string.areamap_weather_alert_ice, point, time, hour.temperatureC);
    return context.getString(R.string.areamap_weather_alert_rain, point, time, hour.precipitationProbability);
  }

  @NonNull
  private static String weatherLabel(@NonNull Context context, int code)
  {
    if (code == 0)
      return context.getString(R.string.areamap_weather_clear);
    if (code <= 3)
      return context.getString(R.string.areamap_weather_cloudy);
    if (code == 45 || code == 48)
      return context.getString(R.string.areamap_weather_fog);
    if ((code >= 51 && code <= 67) || (code >= 80 && code <= 82))
      return context.getString(R.string.areamap_weather_rain);
    if ((code >= 71 && code <= 77) || code == 85 || code == 86)
      return context.getString(R.string.areamap_weather_snow);
    if (code >= 95)
      return context.getString(R.string.areamap_weather_storm);
    return context.getString(R.string.areamap_weather_unknown);
  }

  private static void post(@NonNull Context context, @NonNull String title, @NonNull String body)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ActivityCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PERMISSION_GRANTED)
      return;

    final int immutable = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? 0 : PendingIntent.FLAG_IMMUTABLE;
    final Intent open = new Intent(context, MwmActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    final PendingIntent pendingIntent =
        PendingIntent.getActivity(context, NOTIFICATION_ID, open,
                                  PendingIntent.FLAG_UPDATE_CURRENT | immutable);
    final NotificationCompat.Builder notification =
        new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.warning_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
  }
}
