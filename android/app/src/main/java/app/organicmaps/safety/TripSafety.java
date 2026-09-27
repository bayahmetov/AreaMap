package app.organicmaps.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

  public void onLocation(@NonNull Location location)
  {
    if (!location.hasAccuracy())
      return;
    save(location);
    updateReturnDetection(location);
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

  public void startMonitoredTrip(@NonNull TripPlan plan, @NonNull Profile profile, boolean ownsTrackRecording)
  {
    saveProfile(profile);
    final long now = System.currentTimeMillis();
    final String tripId = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date(now));
    mPrefs.edit()
        .putBoolean("trip_active", true)
        .putString("trip_id", "AM-" + tripId)
        .putLong("trip_started_at", now)
        .putLong("trip_planned_finish", now + plan.plannedSeconds * 1000L)
        .putInt("trip_planned_seconds", plan.plannedSeconds)
        .putString("trip_start_title", plan.startTitle)
        .putString("trip_finish_title", plan.finishTitle)
        .putString("trip_start_lat", Double.toString(plan.startLat))
        .putString("trip_start_lon", Double.toString(plan.startLon))
        .putString("trip_finish_lat", Double.toString(plan.finishLat))
        .putString("trip_finish_lon", Double.toString(plan.finishLon))
        .putString("trip_distance", Double.toString(plan.distanceMeters))
        .putString("trip_checkpoints", encodeCheckpoints(plan))
        .putBoolean("trip_departed_start", false)
        .putBoolean("trip_return_detected", false)
        .putBoolean("trip_return_prompt_dismissed", false)
        .putBoolean("trip_owns_track_recording", ownsTrackRecording)
        .apply();
  }

  public boolean hasActiveTrip()
  {
    return mPrefs.getBoolean("trip_active", false);
  }

  public boolean ownsTrackRecording()
  {
    return hasActiveTrip() && mPrefs.getBoolean("trip_owns_track_recording", false);
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

  public void completeTrip()
  {
    mPrefs.edit()
        .putBoolean("trip_active", false)
        .putBoolean("trip_return_detected", false)
        .putBoolean("trip_return_prompt_dismissed", false)
        .putBoolean("trip_owns_track_recording", false)
        .apply();
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
        formatTime(mPrefs.getLong("trip_planned_finish", 0)));
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
    out.append(mContext.getString(R.string.areamap_report_distance, distanceMeters / 1000.0)).append('\n');
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
                                        formatTime(started + eta * 1000L), formatDuration(eta),
                                        distance / 1000.0, altitudeText)).append('\n');
        }
        catch (NumberFormatException ignored) {}
      }
    }
    out.append(mContext.getString(R.string.areamap_report_consent)).append('\n');
    return out.toString();
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
