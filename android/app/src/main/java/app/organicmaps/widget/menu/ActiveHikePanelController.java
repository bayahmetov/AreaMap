package app.organicmaps.widget.menu;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.routing.HikePanelProgress;
import app.organicmaps.safety.GpxNavigation;
import app.organicmaps.safety.GpxTrack;
import app.organicmaps.safety.HikingTiming;
import app.organicmaps.safety.SosConfirmationFlow;
import app.organicmaps.safety.TripPlan;
import app.organicmaps.safety.TripReportSender;
import app.organicmaps.safety.TripSafety;
import app.organicmaps.safety.TripWeatherNotifier;
import app.organicmaps.safety.TripWeatherRepository;
import app.organicmaps.sdk.routing.RouteAltitudeData;
import app.organicmaps.sdk.routing.RoutingInfo;
import app.organicmaps.sdk.util.concurrency.ThreadPool;
import app.organicmaps.widget.placepage.ElevationChartUtils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * View binding inside the existing NavMenu. GPS matching, route/weather generation and delivery stay in their owners.
 */
final class ActiveHikePanelController implements DefaultLifecycleObserver
{
  private final AppCompatActivity mActivity;
  private final View mRoot;
  private final TripSafety mSafety;
  private final Runnable mRefresh = this::refresh;
  private final SharedPreferences.OnSharedPreferenceChangeListener mStateListener = (prefs, key) -> requestRefresh();
  private boolean mEnabled;
  private boolean mStarted;
  private boolean mLoading;
  private RoutingInfo mInfo;
  private Object mGraphSource;
  private GpxTrack mWeatherTrack;
  private double mWeatherLat;
  private double mWeatherLon;
  private int mWeatherAltitude = -1;
  private boolean mHighest;
  private long mRenderedForecast = -1;
  private boolean mRenderedCached;

  ActiveHikePanelController(AppCompatActivity activity, View root, Runnable stop)
  {
    mActivity = activity;
    mRoot = root;
    mSafety = TripSafety.get(activity);
    activity.getLifecycle().addObserver(this);
    root.findViewById(R.id.hike_stop).setOnClickListener(v -> stop.run());
    root.findViewById(R.id.hike_finish).setOnClickListener(v -> ((MwmActivity) activity).confirmFinishRegisteredTrip());
    root.findViewById(R.id.hike_ok).setOnClickListener(v -> {
      if (!mSafety.hasActiveTrip())
        return;
      mSafety.markImOk();
      TripReportSender.send(activity, "ok", mSafety.okReport());
      refresh();
    });
    root.findViewById(R.id.hike_break).setOnClickListener(v -> {
      if (!mSafety.hasActiveTrip())
        return;
      mSafety.addBreakMinutes(20);
      TripReportSender.send(activity, "break", mSafety.breakReport(20));
      refresh();
    });
    root.findViewById(R.id.hike_sos).setOnClickListener(v -> SosConfirmationFlow.show(activity, this::sendSos));
    root.findViewById(R.id.hike_weather_refresh).setOnClickListener(v -> refreshWeather(true));
    ((TextView) root.findViewById(R.id.hike_sos)).setTextColor(color(R.color.areamap_on_green));
    root.findViewById(R.id.hike_sos)
        .setBackgroundTintList(android.content.res.ColorStateList.valueOf(color(R.color.areamap_red)));
  }

  private void sendSos()
  {
    final Location last = location();
    if (last != null)
      mSafety.save(last);
    TripReportSender.send(mActivity, "sos", mSafety.sosReport());
    new MaterialAlertDialogBuilder(mActivity)
        .setTitle(R.string.areamap_sos)
        .setMessage(mSafety.card() + "\n\n" + TripReportSender.summary(mActivity))
        .setPositiveButton(R.string.areamap_close, null)
        .show();
    refresh();
  }

  void setEnabled(boolean enabled)
  {
    final boolean changed = mEnabled != enabled;
    mEnabled = enabled;
    if (enabled)
    {
      refresh();
      if (changed && mStarted)
        refreshWeather(false);
    }
  }

  void update(@Nullable RoutingInfo info)
  {
    mInfo = info;
    if (mEnabled)
      refresh();
  }

  @Override
  public void onStart(LifecycleOwner owner)
  {
    mStarted = true;
    mSafety.addStateListener(mStateListener);
    TripWeatherRepository.addCacheListener(mActivity, mStateListener);
    mActivity.getSharedPreferences("areamap_delivery", Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(mStateListener);
    requestRefresh();
    if (mEnabled)
      refreshWeather(false);
  }

  @Override
  public void onStop(LifecycleOwner owner)
  {
    mStarted = false;
    mSafety.removeStateListener(mStateListener);
    TripWeatherRepository.removeCacheListener(mActivity, mStateListener);
    mActivity.getSharedPreferences("areamap_delivery", Context.MODE_PRIVATE)
        .unregisterOnSharedPreferenceChangeListener(mStateListener);
    mRoot.removeCallbacks(mRefresh);
  }

  private void requestRefresh()
  {
    if (!mStarted || !mEnabled)
      return;
    mRoot.removeCallbacks(mRefresh);
    mRoot.post(mRefresh);
  }

  private Location location()
  {
    return MwmApplication.from(mActivity).getLocationHelper().getSavedLocation();
  }

  private int color(int id)
  {
    return ContextCompat.getColor(mActivity, id);
  }
  private int dp(float value)
  {
    return Math.round(value * mActivity.getResources().getDisplayMetrics().density);
  }
  private String unknown()
  {
    return mActivity.getString(R.string.hike_unknown);
  }
  private void text(int id, CharSequence value)
  {
    ((TextView) mRoot.findViewById(id)).setText(value);
  }
  private String clock(long time)
  {
    return time <= 0 ? unknown() : DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(time));
  }
  private String duration(int seconds)
  {
    return seconds < 0 ? unknown() : mActivity.getString(R.string.hike_duration, seconds / 3600, seconds / 60 % 60);
  }

  private void refresh()
  {
    if (!mEnabled)
      return;
    final GpxNavigation gpx =
        app.organicmaps.sdk.routing.RoutingController.get().isNavigating() ? null : GpxNavigation.current;
    final TripPlan plan = mSafety.activePlan();
    final Location location = location();
    final long now = System.currentTimeMillis();
    final boolean freshLocation = location != null && location.hasAccuracy() && location.getAccuracy() <= 100
                               && now - location.getTime() < 120000;
    final double total = gpx != null ? gpx.track.length : plan != null ? plan.distanceMeters : Double.NaN;
    final double fraction = gpx != null   ? Math.max(0, Math.min(1, 1 - gpx.remaining / total))
                          : mInfo != null ? HikePanelProgress.fraction(mInfo.completionPercent)
                                          : Double.NaN;
    final boolean known = Double.isFinite(fraction) && (gpx != null ? gpx.hasFix : mInfo != null && freshLocation);
    final double completed = total * fraction;
    final double remaining = gpx != null   ? gpx.remaining
                           : mInfo != null ? HikingTiming.toMeters(mInfo.distToTarget)
                                           : Double.NaN;
    final int seconds = gpx != null            ? (known ? gpx.seconds : -1)
                      : known && mInfo != null ? mInfo.totalTimeInSeconds
                                               : -1;
    text(R.id.hike_route_title,
         plan != null && !plan.finishTitle.isEmpty()
             ? plan.finishTitle
             : mActivity.getString(gpx != null ? R.string.hike_gpx_source : R.string.hike_title));
    text(R.id.hike_progress_summary,
         !known ? mActivity.getString(R.string.hike_waiting)
         : Double.isFinite(total)
             ? mActivity.getString(R.string.hike_progress, completed / 1000, total / 1000, fraction * 100)
             : mActivity.getString(R.string.hike_percent, fraction * 100));
    text(R.id.hike_remaining_summary, !known ? unknown()
                                             : mActivity.getString(R.string.hike_remaining, remaining / 1000,
                                                                   duration(seconds), clock(now + seconds * 1000L)));
    ((com.google.android.material.progressindicator.LinearProgressIndicator) mRoot.findViewById(
         R.id.navigation_progress))
        .setProgressCompat(known && Double.isFinite(fraction) ? (int) (fraction * 100) : 0, true);
    renderCheckpoints(plan, completed, remaining, seconds, known, now);
    final String altitude = freshLocation && location.hasAltitude()
                              ? mActivity.getString(R.string.hike_altitude, location.getAltitude())
                              : unknown();
    final double ascent = gpx != null && gpx.track.hasElevation ? gpx.track.ascent
                        : plan != null && plan.altitude != null ? plan.altitude.getTotalAscent()
                                                                : Double.NaN;
    final double speed = freshLocation && location.hasSpeed() ? location.getSpeed() : Double.NaN;
    text(R.id.hike_stats_value,
         mActivity.getString(
             R.string.hike_stats_value, altitude,
             Double.isFinite(ascent) ? mActivity.getString(R.string.hike_altitude, ascent) : unknown(),
             Double.isFinite(speed) ? mActivity.getString(R.string.hike_speed, speed * 3.6) : unknown(),
             speed > 0.1 ? mActivity.getString(R.string.hike_pace, HikingTiming.formatPace(1000 / speed)) : unknown()));
    final long drift = mSafety.scheduleDriftSeconds(known ? fraction * 100 : Double.NaN, now);
    String schedule =
        drift == Long.MIN_VALUE
            ? mActivity.getString(mSafety.hasActiveTrip() ? R.string.hike_waiting : R.string.hike_schedule_none)
            : mActivity.getString(Math.abs(drift) < 60 ? R.string.hike_schedule_plan
                                  : drift > 0          ? R.string.hike_schedule_late
                                                       : R.string.hike_schedule_early,
                                  Math.abs(drift) / 60);
    if (mSafety.hasActiveTrip())
      schedule += "\n"
                + mActivity.getString(R.string.hike_return,
                                      clock(mSafety.projectedReturnAtMillis(known ? fraction * 100 : Double.NaN, now)));
    text(R.id.hike_schedule_value, schedule);
    text(R.id.hike_active_status, mSafety.hasActiveTrip()
                                      ? mSafety.activeTripSummary()
                                      : mActivity.getString(R.string.areamap_nav_no_registered_trip));
    text(R.id.hike_delivery_status, TripReportSender.summary(mActivity));
    mRoot.findViewById(R.id.hike_ok).setEnabled(mSafety.hasActiveTrip());
    mRoot.findViewById(R.id.hike_break).setEnabled(mSafety.hasActiveTrip());
    mRoot.findViewById(R.id.hike_finish).setVisibility(mSafety.hasActiveTrip() ? View.VISIBLE : View.GONE);
    configureWeatherPoint(plan, gpx);
    renderWeather(now);
    renderGraph(plan, gpx, known ? fraction : Double.NaN);
    if (gpx != null && known && gpx.arrived)
      text(R.id.hike_remaining_summary, mActivity.getString(R.string.areamap_track_arrived));
    if (gpx != null && known && gpx.offset > 50)
      text(R.id.hike_remaining_summary, mActivity.getString(R.string.areamap_track_off_course, gpx.offset) + "\n"
                                            + ((TextView) mRoot.findViewById(R.id.hike_remaining_summary)).getText());
  }

  private String checkpointTitle(TripPlan.Checkpoint cp, int index)
  {
    if (!cp.title.isEmpty())
      return cp.title;
    return cp.role < 0 ? mActivity.getString(R.string.areamap_route_start)
  : cp.role > 0        ? mActivity.getString(R.string.areamap_route_finish)
                       : mActivity.getString(R.string.hike_checkpoint, index);
  }

  private void renderCheckpoints(TripPlan plan, double completed, double remaining, int seconds, boolean known,
                                 long now)
  {
    final LinearLayout list = mRoot.findViewById(R.id.hike_checkpoints);
    list.removeAllViews();
    if (plan == null || plan.checkpoints.isEmpty())
    {
      text(R.id.hike_next_value, mActivity.getString(R.string.hike_no_checkpoints));
      list.addView(label(mActivity.getString(R.string.hike_no_checkpoints), R.color.areamap_text_muted));
      return;
    }
    final List<TripPlan.Checkpoint> points = plan.routeCheckpoints;
    final double[] distances = new double[points.size() - 1];
    for (int i = 1; i < points.size(); i++)
      distances[i - 1] = points.get(i).distanceMeters;
    final int next = HikePanelProgress.nextCheckpoint(distances, completed, known);
    for (int i = 0; i < points.size(); i++)
    {
      final TripPlan.Checkpoint cp = points.get(i);
      final boolean passed = known && (i == 0 || next < 0 || i - 1 < next);
      final boolean isNext = i > 0 && i - 1 == next;
      final String name = checkpointTitle(cp, i);
      final int status = passed ? R.string.hike_passed : isNext ? R.string.hike_next : R.string.hike_ahead;
      final String detail =
          mActivity.getString(R.string.hike_checkpoint_detail, cp.distanceMeters / 1000,
                              known ? mActivity.getString(R.string.route_preview_distance_value,
                                                          Math.max(0, cp.distanceMeters - completed) / 1000)
                                    : unknown(),
                              clock(mSafety.startedAtMillis() + cp.etaSeconds * 1000L + mSafety.timingOffsetMillis()));
      final TextView row = label((passed ? "✓ " : "") + name + " · " + mActivity.getString(status) + "\n" + detail,
                                 passed   ? R.color.areamap_text_muted
                                 : isNext ? R.color.areamap_green
                                          : R.color.areamap_text);
      if (isNext)
      {
        row.setBackgroundResource(R.drawable.bg_route_preview_card);
        final int eta =
            known ? HikePanelProgress.checkpointSeconds(cp.distanceMeters, completed, remaining, seconds) : -1;
        text(R.id.hike_next_value,
             known
                 ? mActivity.getString(
                       R.string.hike_checkpoint_next, name, Math.max(0, cp.distanceMeters - completed) / 1000,
                       duration(eta) + " · " + clock(eta < 0 ? 0 : now + eta * 1000L),
                       cp.altitudeMeters >= 0 ? mActivity.getString(R.string.hike_altitude, (double) cp.altitudeMeters)
                                              : unknown())
                 : name + " · " + mActivity.getString(R.string.hike_waiting));
      }
      list.addView(row);
    }
    if (next < 0)
      text(R.id.hike_next_value, mActivity.getString(R.string.hike_all_passed));
  }

  private TextView label(String value, int colorId)
  {
    final TextView text = new TextView(mActivity);
    text.setText(value);
    text.setTextColor(color(colorId));
    text.setTextSize(13);
    text.setPadding(dp(10), dp(10), dp(10), dp(10));
    text.setLayoutParams(
        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    return text;
  }

  private void configureWeatherPoint(TripPlan plan, GpxNavigation gpx)
  {
    if (plan != null)
    {
      mWeatherTrack = null;
      mWeatherLat = plan.weatherLat;
      mWeatherLon = plan.weatherLon;
      mWeatherAltitude = plan.weatherAltitudeMeters;
      mHighest = plan.weatherAtHighestPoint;
    }
    else if (gpx != null && mWeatherTrack != gpx.track)
    {
      mWeatherTrack = gpx.track;
      double[] target = gpx.track.points[gpx.track.points.length - 1];
      mHighest = gpx.track.hasElevation;
      if (mHighest)
        for (double[] point : gpx.track.points)
          if (point[2] > target[2])
            target = point;
      mWeatherLat = target[0];
      mWeatherLon = target[1];
      mWeatherAltitude = Double.isFinite(target[2]) ? (int) Math.round(target[2]) : -1;
    }
    text(R.id.hike_weather_point, mHighest
                                      ? mActivity.getString(R.string.areamap_weather_highest_point, mWeatherAltitude)
                                      : mActivity.getString(R.string.areamap_weather_destination));
  }

  private void refreshWeather(boolean force)
  {
    if (mLoading || (!mSafety.hasActiveTrip() && GpxNavigation.current == null))
      return;
    refresh();
    final Context context = mActivity.getApplicationContext();
    final double lat = mWeatherLat, lon = mWeatherLon;
    final int altitude = mWeatherAltitude;
    mLoading = true;
    renderWeather(System.currentTimeMillis());
    ThreadPool.getStorage().execute(() -> {
      TripWeatherRepository.refreshForPoint(context, lat, lon, altitude, force);
      mActivity.runOnUiThread(() -> {
        mLoading = false;
        if (mStarted && !mActivity.isDestroyed())
          requestRefresh();
      });
    });
  }

  private void renderWeather(long now)
  {
    final boolean cached =
        (mSafety.hasActiveTrip() || GpxNavigation.current != null)
        && TripWeatherRepository.hasForecastForPoint(mActivity, mWeatherLat, mWeatherLon, mWeatherAltitude);
    final List<TripWeatherRepository.Hour> hours = cached ? TripWeatherRepository.hours(mActivity, now, 8) : List.of();
    final long fetched = cached ? TripWeatherRepository.fetchedAt(mActivity) : 0;
    final String unavailable =
        mActivity.getString(mLoading ? R.string.hike_weather_loading : R.string.hike_weather_unavailable);
    final TripWeatherRepository.Hour first = hours.isEmpty() ? null : hours.get(0);
    text(R.id.hike_weather_summary,
         first == null ? unavailable
                       : TripWeatherNotifier.weatherLabel(mActivity, first.weatherCode) + " · "
                             + mActivity.getString(R.string.route_preview_temperature, first.temperatureC));
    text(R.id.hike_weather_age,
         first == null ? unavailable
                       : mActivity.getString(R.string.hike_weather_age, Math.max(0, (now - fetched) / 60000)));
    final LinearLayout strip = mRoot.findViewById(R.id.hike_hours);
    final long renderKey = fetched + now / 3_600_000L;
    if (mRenderedForecast != renderKey || mRenderedCached != cached)
    {
      mRenderedForecast = renderKey;
      mRenderedCached = cached;
      strip.removeAllViews();
      for (int i = 0; i < hours.size(); i++)
      {
        final TripWeatherRepository.Hour hour = hours.get(i);
        final LinearLayout card = new LinearLayout(mActivity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_route_preview_card);
        final LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(dp(100), ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(8));
        card.setLayoutParams(params);
        card.addView(label(
            i == 0 && hour.timeMillis <= now ? mActivity.getString(R.string.hike_hour_now) : clock(hour.timeMillis),
            R.color.areamap_text));
        final ImageView icon = new ImageView(mActivity);
        icon.setImageResource(hour.weatherCode == 0    ? R.drawable.ic_preview_sun
                              : hour.weatherCode <= 48 ? R.drawable.ic_preview_cloud
                                                       : R.drawable.ic_preview_rain);
        icon.setContentDescription(TripWeatherNotifier.weatherLabel(mActivity, hour.weatherCode));
        card.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        card.addView(label(mActivity.getString(R.string.hike_hour_value, hour.temperatureC,
                                               hour.precipitationProbability, hour.windKmh),
                           R.color.areamap_text_secondary));
        strip.addView(card);
      }
    }
    final TripWeatherRepository.Hour hazard =
        cached ? TripWeatherRepository.firstHazard(mActivity, now, now + 2 * 3_600_000L) : null;
    mRoot.findViewById(R.id.hike_weather_alert).setVisibility(hazard == null ? View.GONE : View.VISIBLE);
    if (hazard != null)
    {
      final String point = ((TextView) mRoot.findViewById(R.id.hike_weather_point)).getText().toString();
      text(R.id.hike_weather_alert, TripWeatherNotifier.hazardText(mActivity, point, hazard));
      ((TextView) mRoot.findViewById(R.id.hike_weather_alert)).setTextColor(color(R.color.areamap_amber));
    }
  }

  private void renderGraph(TripPlan plan, GpxNavigation gpx, double fraction)
  {
    final RouteAltitudeData profile = plan == null ? null : plan.altitude;
    final Object source = gpx != null ? gpx.track : profile;
    final boolean available = gpx != null ? gpx.track.hasElevation : profile != null && profile.getSize() > 1;
    mRoot.findViewById(R.id.hike_graph).setVisibility(available ? View.VISIBLE : View.GONE);
    mRoot.findViewById(R.id.hike_graph_empty).setVisibility(available ? View.GONE : View.VISIBLE);
    if (!available)
      return;
    final LineChart chart = mRoot.findViewById(R.id.hike_graph).findViewById(R.id.elevation_profile_chart);
    if (mGraphSource != source)
    {
      mGraphSource = source;
      ElevationChartUtils.setupRouteChart(chart, mActivity);
      chart.setTouchEnabled(false);
      final List<Entry> entries = new ArrayList<>();
      if (gpx != null)
        for (int i = 0; i < gpx.track.points.length; i++)
          entries.add(new Entry((float) gpx.track.distance[i], (float) gpx.track.points[i][2]));
      else
        for (int i = 0; i < profile.getSize(); i++)
          entries.add(new Entry((float) profile.getDistance(i), profile.getAltitude(i)));
      final LineDataSet line = new LineDataSet(entries, "");
      line.setColor(color(R.color.areamap_green));
      line.setDrawCircles(false);
      line.setDrawValues(false);
      final LineData data = new LineData(line);
      if (plan != null)
      {
        final List<Entry> points = new ArrayList<>();
        for (TripPlan.Checkpoint cp : plan.routeCheckpoints)
          if (cp.altitudeMeters >= 0)
            points.add(new Entry(
                (float) (cp.distanceMeters / Math.max(1, plan.distanceMeters) * entries.get(entries.size() - 1).getX()),
                cp.altitudeMeters));
        final LineDataSet dots = new LineDataSet(points, "");
        dots.setColor(android.graphics.Color.TRANSPARENT);
        dots.setCircleColor(color(R.color.areamap_amber));
        dots.setDrawValues(false);
        dots.setDrawCircleHole(false);
        data.addDataSet(dots);
      }
      chart.setData(data);
      chart.setBackgroundColor(color(R.color.areamap_surface_2));
      mRoot.findViewById(R.id.hike_graph).findViewById(R.id.highest_altitude).setVisibility(View.GONE);
      mRoot.findViewById(R.id.hike_graph).findViewById(R.id.lowest_altitude).setVisibility(View.GONE);
    }
    chart.getXAxis().removeAllLimitLines();
    if (Double.isFinite(fraction))
    {
      final LimitLine marker = new LimitLine((float) (chart.getData().getXMax() * fraction));
      marker.setLineColor(color(R.color.areamap_red));
      marker.setLineWidth(2);
      chart.getXAxis().addLimitLine(marker);
    }
    chart.invalidate();
  }
}
