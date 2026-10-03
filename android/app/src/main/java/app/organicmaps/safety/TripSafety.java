package app.organicmaps.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.annotation.NonNull;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.Router;
import app.organicmaps.sdk.routing.RouteMarkData;
import app.organicmaps.sdk.routing.RouteMarkType;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.routing.RoutingInfo;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Stores the latest SOS fix, the reusable hiker profile and the currently registered trip.
 *
 * The DCHS/Telegram transport is deliberately separate. This class only persists the data that the
 * user explicitly confirmed and detects a likely return to the trip start.
 */
public final class TripSafety
{
  public static final class Profile
  {
    @NonNull public final String name;
    @NonNull public final String phone;
    public final int groupSize;
    @NonNull public final String emergencyName;
    @NonNull public final String emergencyPhone;

    public Profile(@NonNull String name, @NonNull String phone, int groupSize,
                   @NonNull String emergencyName, @NonNull String emergencyPhone)
    {
      this.name = name.trim();
      this.phone = phone.trim();
      this.groupSize = Math.max(1, groupSize);
      this.emergencyName = emergencyName.trim();
      this.emergencyPhone = emergencyPhone.trim();
    }
  }

  private static final String PREFS = "areamap_sos";
  private static final double LEAVE_START_RADIUS_M = 400.0;
  private static final double RETURN_RADIUS_M = 150.0;
  private static final long MIN_RETURN_TIME_MS = 10 * 60 * 1000L;

  private static TripSafety sInstance;
  private final Context mContext;
  private final SharedPreferences mPrefs;

  private TripSafety(@NonNull Context context)
  {
    mContext = context.getApplicationContext();
    mPrefs = mContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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

  public synchronized void onLocation(@NonNull Location location)
  {
    if (!location.hasAccuracy() || location.getAccuracy() > 100
        || System.currentTimeMillis() - location.getTime() > 120000)
      return;
    save(location);
    updateReturnDetection(location);
    updateScheduleDelay();
  }

  public synchronized void save(@NonNull Location location)
  {
    if (!location.hasAccuracy() || location.getTime() < mPrefs.getLong("measured", 0))
      return;
    mPrefs.edit()
        .putString("lat", Double.toString(location.getLatitude()))
        .putString("lon", Double.toString(location.getLongitude()))
        .putFloat("accuracy", location.getAccuracy())
        .putLong("measured", location.getTime())
        .apply();
  }

  @NonNull
  public Profile getProfile()
  {
    return new Profile(mPrefs.getString("profile_name", ""), mPrefs.getString("profile_phone", ""),
                       mPrefs.getInt("profile_group_size", 1), mPrefs.getString("profile_emergency_name", ""),
                       mPrefs.getString("profile_emergency_phone", ""));
  }

  public void saveProfile(@NonNull Profile profile)
  {
    mPrefs.edit()
        .putString("profile_name", profile.name)
        .putString("profile_phone", profile.phone)
        .putInt("profile_group_size", profile.groupSize)
        .putString("profile_emergency_name", profile.emergencyName)
        .putString("profile_emergency_phone", profile.emergencyPhone)
        .apply();
  }

  public synchronized boolean startMonitoredTrip(@NonNull TripPlan plan, @NonNull Profile profile,
                                                 boolean ownsTrackRecording)
  {
    if (hasActiveTrip())
      return false;
    saveProfile(profile);
    final long now = System.currentTimeMillis();
    final String tripId = java.util.UUID.randomUUID().toString();
    final boolean saved = mPrefs.edit()
                              .putBoolean("trip_active", true)
                              .putString("trip_id", "AM-" + tripId)
                              .putLong("trip_started_at", now)
                              .putLong("trip_planned_finish", now + plan.plannedSeconds * 1000L)
                              .putLong("trip_planned_return", now + (plan.plannedSeconds + plan.returnSeconds) * 1000L)
                              .putInt("trip_planned_seconds", plan.plannedSeconds)
                              .putInt("trip_return_seconds", plan.returnSeconds)
                              .putString("trip_start_title", plan.startTitle)
                              .putString("trip_finish_title", plan.finishTitle)
                              .putString("trip_start_lat", Double.toString(plan.startLat))
                              .putString("trip_start_lon", Double.toString(plan.startLon))
                              .putString("trip_finish_lat", Double.toString(plan.finishLat))
                              .putString("trip_finish_lon", Double.toString(plan.finishLon))
                              .putString("trip_distance", Double.toString(plan.distanceMeters))
                              .putString("trip_route_points", encodeRoutePoints())
                              .putString("trip_checkpoints", encodeCheckpoints(plan))
                              .putString("trip_weather_lat", Double.toString(plan.weatherLat))
                              .putString("trip_weather_lon", Double.toString(plan.weatherLon))
                              .putInt("trip_weather_altitude", plan.weatherAltitudeMeters)
                              .putInt("trip_weather_eta_seconds", plan.weatherEtaSeconds)
                              .putBoolean("trip_weather_highest", plan.weatherAtHighestPoint)
                              .putBoolean("trip_departed_start", false)
                              .putBoolean("trip_return_detected", false)
                              .putBoolean("trip_return_prompt_dismissed", false)
                              .putBoolean("trip_owns_track_recording", ownsTrackRecording)
                              .putInt("trip_schedule_alert_bucket", 0)
                              .putInt("trip_timing_offset_seconds", 0)
                              .putLong("trip_last_ok", 0)
                              .commit();

    if (!saved)
      return false;
    TripWeatherRepository.clearAlertState(mContext);
    TripWeatherWorker.start(mContext);
    return true;
  }

  @NonNull
  public String tripId()
  {
    return hasActiveTrip() ? mPrefs.getString("trip_id", "unregistered") : "unregistered";
  }

  public double startLat()
  {
    return parseDouble("trip_start_lat");
  }
  public double startLon()
  {
    return parseDouble("trip_start_lon");
  }
  public double finishLat()
  {
    return parseDouble("trip_finish_lat");
  }
  public double finishLon()
  {
    return parseDouble("trip_finish_lon");
  }

  public boolean matches(@NonNull TripPlan plan)
  {
    final float[] distance = new float[1];
    Location.distanceBetween(finishLat(), finishLon(), plan.finishLat, plan.finishLon, distance);
    return hasActiveTrip() && distance[0] < 100;
  }

  private String encodeRoutePoints()
  {
    final JSONArray out = new JSONArray();
    final RouteMarkData[] points = Framework.nativeGetRoutePoints();
    if (points != null)
      for (RouteMarkData point : points)
      {
        try
        {
          out.put(new JSONObject()
                      .put("lat", point.mLat)
                      .put("lon", point.mLon)
                      .put("type", point.mPointType.name())
                      .put("title", point.mTitle == null ? "" : point.mTitle)
                      .put("subtitle", point.mSubtitle == null ? "" : point.mSubtitle));
        }
        catch (org.json.JSONException e)
        {
          throw new IllegalStateException("Invalid route point", e);
        }
      }
    return out.toString();
  }

  /** Rebuild the original ordered route points, retaining intermediate stops and the trip identity. */
  public boolean restoreSavedRoute()
  {
    if (!hasActiveTrip())
      return false;
    try
    {
      final JSONArray points = new JSONArray(mPrefs.getString("trip_route_points", "[]"));
      if (points.length() < 2)
        return false;
      final RoutingController controller = RoutingController.get();
      controller.prepare(null, null, Router.Pedestrian);
      for (int i = 0; i < points.length(); i++)
      {
        final JSONObject point = points.getJSONObject(i);
        Framework.addRoutePoint(new RouteMarkData(point.getString("title"), point.getString("subtitle"),
                                                  RouteMarkType.valueOf(point.getString("type")), i, true, false, false,
                                                  point.getDouble("lat"), point.getDouble("lon")),
                                false);
      }
      controller.checkAndBuildRoute();
      return true;
    }
    catch (org.json.JSONException e)
    {
      return false;
    }
  }

  @NonNull
  public String sosReport()
  {
    final Profile profile = getProfile();
    return mContext.getString(R.string.areamap_report_sos_header) + "\n"
  + mContext.getString(R.string.areamap_report_trip_id, tripId()) + "\n"
  + mContext.getString(R.string.areamap_report_person, profile.name, profile.phone) + "\n"
  + mContext.getString(R.string.areamap_report_group, profile.groupSize) + "\n"
  + mContext.getString(R.string.areamap_report_emergency, profile.emergencyName, profile.emergencyPhone) + "\n"
  + (hasActiveTrip() ? activeTripSummary() + "\n" : "") + card();
  }

  public boolean hasActiveTrip()
  {
    return mPrefs.getBoolean("trip_active", false);
  }

  public boolean ownsTrackRecording()
  {
    return hasActiveTrip() && mPrefs.getBoolean("trip_owns_track_recording", false);
  }

  public double weatherLat()
  {
    return parseDouble("trip_weather_lat");
  }

  public double weatherLon()
  {
    return parseDouble("trip_weather_lon");
  }

  public int weatherAltitudeMeters()
  {
    return mPrefs.getInt("trip_weather_altitude", -1);
  }

  public boolean weatherAtHighestPoint()
  {
    return mPrefs.getBoolean("trip_weather_highest", false);
  }

  public long weatherTargetAtMillis()
  {
    final long startedAt = mPrefs.getLong("trip_started_at", 0L);
    final int weatherEtaSeconds = mPrefs.getInt("trip_weather_eta_seconds",
                                                mPrefs.getInt("trip_planned_seconds", 0));
    final int offsetSeconds = mPrefs.getInt("trip_timing_offset_seconds", 0);
    return startedAt + (weatherEtaSeconds + offsetSeconds) * 1000L;
  }

  public boolean shouldSuggestReturn()
  {
    return hasActiveTrip() && mPrefs.getBoolean("trip_return_detected", false)
        && !mPrefs.getBoolean("trip_return_prompt_dismissed", false);
  }

  public void dismissReturnSuggestion()
  {
    mPrefs.edit().putBoolean("trip_return_prompt_dismissed", true).apply();
  }

  public void markImOk()
  {
    // Acknowledging an alert must not reset its delay bucket, otherwise the next GPS fix
    // can immediately post the same warning again.
    mPrefs.edit()
        .putLong("trip_last_ok", System.currentTimeMillis())
        .apply();
  }

  public void addBreakMinutes(int minutes)
  {
    if (!hasActiveTrip() || minutes <= 0)
      return;

    final int seconds = minutes * 60;
    mPrefs.edit()
        .putInt("trip_timing_offset_seconds", mPrefs.getInt("trip_timing_offset_seconds", 0) + seconds)
        .putLong("trip_planned_finish", mPrefs.getLong("trip_planned_finish", 0) + seconds * 1000L)
        .putLong("trip_planned_return", mPrefs.getLong("trip_planned_return", 0) + seconds * 1000L)
        .putInt("trip_schedule_alert_bucket", 0)
        .apply();
  }

  @NonNull
  public String navigationCheckpointSummary(double completionPercent)
  {
    if (!hasActiveTrip())
      return mContext.getString(R.string.areamap_nav_no_registered_trip);

    final double progress = Math.max(0.0, Math.min(1.0, completionPercent / 100.0));
    final NextCheckpoint next = findNextCheckpoint(progress);
    if (next.number <= 0)
      return mContext.getString(R.string.areamap_nav_destination_eta, formatClock(next.plannedAtMillis));
    return mContext.getString(R.string.areamap_nav_checkpoint_eta, next.number, formatClock(next.plannedAtMillis));
  }

  @NonNull
  public String navigationReturnSummary()
  {
    if (!hasActiveTrip())
      return mContext.getString(R.string.areamap_nav_return_unknown);
    return mContext.getString(R.string.areamap_nav_return_eta,
                              formatClock(mPrefs.getLong("trip_planned_return", 0)));
  }

  public void completeTrip()
  {
    TripWeatherWorker.stop(mContext);
    mPrefs.edit()
        .putBoolean("trip_active", false)
        .putBoolean("trip_return_detected", false)
        .putBoolean("trip_return_prompt_dismissed", false)
        .putBoolean("trip_owns_track_recording", false)
        .putInt("trip_schedule_alert_bucket", 0)
        .putInt("trip_timing_offset_seconds", 0)
        .commit();
  }

  @NonNull
  public String activeTripSummary()
  {
    if (!hasActiveTrip())
      return mContext.getString(R.string.areamap_no_active_trip);

    return mContext.getString(
        R.string.areamap_active_trip_summary,
        mPrefs.getString("trip_start_title", mContext.getString(R.string.areamap_route_start)),
        mPrefs.getString("trip_finish_title", mContext.getString(R.string.areamap_route_finish)),
        formatTime(mPrefs.getLong("trip_started_at", 0)),
        formatTime(mPrefs.getLong("trip_planned_finish", 0)),
        formatTime(mPrefs.getLong("trip_planned_return", 0)));
  }

  @NonNull
  public String startReport()
  {
    return report(false);
  }

  @NonNull
  public String returnReport()
  {
    return report(true);
  }

  @NonNull
  public String okReport()
  {
    final Profile profile = getProfile();
    final StringBuilder out = new StringBuilder();
    out.append(mContext.getString(R.string.areamap_report_ok_header)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_trip_id, mPrefs.getString("trip_id", "—"))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_person, profile.name, profile.phone)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_ok_time,
                                  formatTime(System.currentTimeMillis()))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_last_fix, coordinates())).append('\n');
    out.append(mContext.getString(R.string.areamap_nav_return_eta,
                                  formatClock(mPrefs.getLong("trip_planned_return", 0))));
    return out.toString();
  }

  @NonNull
  public String breakReport(int addedMinutes)
  {
    final Profile profile = getProfile();
    final StringBuilder out = new StringBuilder();
    out.append(mContext.getString(R.string.areamap_report_break_header)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_trip_id, mPrefs.getString("trip_id", "—"))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_person, profile.name, profile.phone)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_break_time, addedMinutes,
                                  formatClock(mPrefs.getLong("trip_planned_return", 0)))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_last_fix, coordinates()));
    return out.toString();
  }

  @NonNull
  private String report(boolean returned)
  {
    final Profile profile = getProfile();
    final StringBuilder out = new StringBuilder();
    out.append(mContext.getString(returned ? R.string.areamap_report_return_header
                                           : R.string.areamap_report_start_header)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_trip_id, mPrefs.getString("trip_id", "—"))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_person, profile.name, profile.phone)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_group, profile.groupSize)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_emergency, profile.emergencyName, profile.emergencyPhone))
       .append('\n');

    if (returned)
    {
      out.append(mContext.getString(R.string.areamap_report_return_time, formatTime(System.currentTimeMillis())))
         .append('\n');
      out.append(mContext.getString(R.string.areamap_report_last_fix, coordinates())).append('\n');
      return out.toString();
    }

    final double distanceMeters = parseDouble("trip_distance");
    out.append(mContext.getString(R.string.areamap_report_route,
                                  mPrefs.getString("trip_start_title", mContext.getString(R.string.areamap_route_start)),
                                  mPrefs.getString("trip_finish_title", mContext.getString(R.string.areamap_route_finish))))
       .append('\n');
    out.append(mContext.getString(R.string.areamap_report_start_time,
                                  formatTime(mPrefs.getLong("trip_started_at", 0)))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_finish_time,
                                  formatTime(mPrefs.getLong("trip_planned_finish", 0)),
                                  formatDuration(mPrefs.getInt("trip_planned_seconds", 0)))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_return_plan,
                                  formatDuration(mPrefs.getInt("trip_return_seconds", 0)),
                                  formatTime(mPrefs.getLong("trip_planned_return", 0)))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_distance, distanceMeters / 1000.0)).append('\n');
    out.append(mContext.getString(R.string.areamap_report_round_trip_distance, distanceMeters * 2.0 / 1000.0))
       .append('\n');
    out.append(mContext.getString(R.string.areamap_report_start_point,
                                  point("trip_start_lat", "trip_start_lon"))).append('\n');
    out.append(mContext.getString(R.string.areamap_report_finish_point,
                                  point("trip_finish_lat", "trip_finish_lon"))).append('\n');

    final String checkpoints = mPrefs.getString("trip_checkpoints", "");
    if (!checkpoints.isEmpty())
    {
      out.append(mContext.getString(R.string.areamap_report_checkpoints)).append('\n');
      final String[] rows = checkpoints.split(";");
      final long started = mPrefs.getLong("trip_started_at", 0);
      final long timingOffsetMs = mPrefs.getInt("trip_timing_offset_seconds", 0) * 1000L;
      int number = 1;
      for (String row : rows)
      {
        final String[] fields = row.split(",");
        if (fields.length != 3)
          continue;
        try
        {
          final int eta = Integer.parseInt(fields[0]);
          final double distance = Double.parseDouble(fields[1]);
          final int altitude = Integer.parseInt(fields[2]);
          final String altitudeText = altitude >= 0
              ? mContext.getString(R.string.areamap_report_checkpoint_altitude, altitude) : "";
          out.append(mContext.getString(R.string.areamap_report_checkpoint_line, number++,
                                        formatTime(started + eta * 1000L + timingOffsetMs), formatDuration(eta),
                                        distance / 1000.0, altitudeText)).append('\n');
        }
        catch (NumberFormatException ignored) {}
      }
    }
    out.append(mContext.getString(R.string.areamap_report_consent)).append('\n');
    return out.toString();
  }

  private void updateScheduleDelay()
  {
    if (!hasActiveTrip() || !MwmApplication.from(mContext).getOrganicMaps().arePlatformAndCoreInitialized()
        || !RoutingController.get().isNavigating())
      return;

    final RoutingInfo info = Framework.nativeGetRouteFollowingInfo();
    if (info == null)
      return;

    final long now = System.currentTimeMillis();
    final long startedAt = mPrefs.getLong("trip_started_at", 0);
    final int plannedSeconds = mPrefs.getInt("trip_planned_seconds", 0);
    if (startedAt <= 0 || plannedSeconds <= 0)
      return;

    final double progress = Math.max(0.0, Math.min(1.0, info.completionPercent / 100.0));
    final long elapsedSeconds = Math.max(0L, (now - startedAt) / 1000L
                                               - mPrefs.getInt("trip_timing_offset_seconds", 0));
    final long plannedElapsedSeconds = Math.round(plannedSeconds * progress);
    final int delayMinutes = (int) Math.max(0L, (elapsedSeconds - plannedElapsedSeconds) / 60L);

    final int bucket = scheduleAlertBucket(delayMinutes);
    final int lastBucket = mPrefs.getInt("trip_schedule_alert_bucket", 0);
    if (bucket == 0)
    {
      if (delayMinutes < 5 && lastBucket != 0)
        mPrefs.edit().putInt("trip_schedule_alert_bucket", 0).apply();
      return;
    }
    if (bucket <= lastBucket)
      return;

    final NextCheckpoint next = findNextCheckpoint(progress);
    final long projectedReturn = mPrefs.getLong("trip_planned_return", now) + delayMinutes * 60_000L;
    mPrefs.edit().putInt("trip_schedule_alert_bucket", bucket).apply();

    TripScheduleNotifier.notifyDelay(
        mContext, delayMinutes, next.number,
        formatClock(next.plannedAtMillis), formatClock(projectedReturn));
  }

  static int scheduleAlertBucket(int delayMinutes)
  {
    if (delayMinutes < 10)
      return 0;
    if (delayMinutes < 20)
      return 1;
    return 2 + (delayMinutes - 20) / 15;
  }

  private static final class NextCheckpoint
  {
    final int number;
    final long plannedAtMillis;

    NextCheckpoint(int number, long plannedAtMillis)
    {
      this.number = number;
      this.plannedAtMillis = plannedAtMillis;
    }
  }

  @NonNull
  private NextCheckpoint findNextCheckpoint(double progress)
  {
    final double totalDistance = parseDouble("trip_distance");
    final double completedDistance = totalDistance * progress;
    final long startedAt = mPrefs.getLong("trip_started_at", 0);
    final long timingOffsetMs = mPrefs.getInt("trip_timing_offset_seconds", 0) * 1000L;
    final String encoded = mPrefs.getString("trip_checkpoints", "");
    if (!encoded.isEmpty())
    {
      final String[] rows = encoded.split(";");
      int number = 1;
      for (String row : rows)
      {
        final String[] fields = row.split(",");
        if (fields.length != 3)
        {
          number++;
          continue;
        }
        try
        {
          final int etaSeconds = Integer.parseInt(fields[0]);
          final double distanceMeters = Double.parseDouble(fields[1]);
          if (distanceMeters > completedDistance + 50.0)
            return new NextCheckpoint(number, startedAt + etaSeconds * 1000L + timingOffsetMs);
        }
        catch (NumberFormatException ignored) {}
        number++;
      }
    }
    return new NextCheckpoint(0, mPrefs.getLong("trip_planned_finish", startedAt));
  }

  @NonNull
  private String formatClock(long millis)
  {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(millis));
  }

  private void updateReturnDetection(@NonNull Location location)
  {
    if (!hasActiveTrip())
      return;

    final long startedAt = mPrefs.getLong("trip_started_at", 0);
    final float[] distance = new float[1];
    Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                             parseDouble("trip_start_lat"), parseDouble("trip_start_lon"), distance);
    final boolean departed = mPrefs.getBoolean("trip_departed_start", false);
    if (!departed && distance[0] >= LEAVE_START_RADIUS_M)
    {
      mPrefs.edit().putBoolean("trip_departed_start", true).apply();
      return;
    }

    if (departed && System.currentTimeMillis() - startedAt >= MIN_RETURN_TIME_MS && distance[0] <= RETURN_RADIUS_M)
    {
      mPrefs.edit()
          .putBoolean("trip_return_detected", true)
          .putBoolean("trip_return_prompt_dismissed", false)
          .apply();
    }
    else if (distance[0] > RETURN_RADIUS_M * 2)
    {
      mPrefs.edit().putBoolean("trip_return_prompt_dismissed", false).apply();
    }
  }

  @NonNull
  private String encodeCheckpoints(@NonNull TripPlan plan)
  {
    final StringBuilder out = new StringBuilder();
    for (TripPlan.Checkpoint checkpoint : plan.checkpoints)
    {
      if (out.length() > 0)
        out.append(';');
      out.append(checkpoint.etaSeconds).append(',')
         .append(String.format(Locale.US, "%.1f", checkpoint.distanceMeters)).append(',')
         .append(checkpoint.altitudeMeters);
    }
    return out.toString();
  }

  private double parseDouble(@NonNull String key)
  {
    try
    {
      return Double.parseDouble(mPrefs.getString(key, "0"));
    }
    catch (NumberFormatException e)
    {
      return 0;
    }
  }

  @NonNull
  private String point(@NonNull String latKey, @NonNull String lonKey)
  {
    return String.format(Locale.US, "%.6f, %.6f", parseDouble(latKey), parseDouble(lonKey));
  }

  @NonNull
  private String formatTime(long millis)
  {
    return millis <= 0 ? "—" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(millis));
  }

  @NonNull
  private String formatDuration(int seconds)
  {
    final int minutes = Math.max(0, seconds / 60);
    if (minutes < 60)
      return mContext.getString(R.string.areamap_duration_minutes, minutes);
    return mContext.getString(R.string.areamap_duration_hours_minutes, minutes / 60, minutes % 60);
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
