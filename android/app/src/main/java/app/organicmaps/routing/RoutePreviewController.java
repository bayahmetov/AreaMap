package app.organicmaps.routing;

import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.safety.DarknessUtil;
import app.organicmaps.safety.TripPlan;
import app.organicmaps.safety.TripWeatherNotifier;
import app.organicmaps.safety.TripWeatherRepository;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.downloader.CountryItem;
import app.organicmaps.sdk.downloader.MapManager;
import app.organicmaps.sdk.routing.JunctionInfo;
import app.organicmaps.sdk.routing.RouteAltitudeData;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.util.Utils;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import java.util.ArrayList;
import java.util.List;

/** Binds existing route/weather data to the pedestrian preview inside RoutingPlanFragment. */
final class RoutePreviewController
{
  private final MwmActivity mActivity;
  private final View mRoot;
  private final TripPlan mPlan;
  private final Runnable mCollapse;
  private boolean mDisposed;

  RoutePreviewController(MwmActivity activity, View root, TripPlan plan, Runnable start, Runnable edit,
                         Runnable collapse, Runnable save)
  {
    mActivity = activity;
    mRoot = root;
    mPlan = plan;
    mCollapse = collapse;
    setText(R.id.areamap_route_v2_title, title(plan));
    setText(R.id.areamap_route_v2_distance,
            activity.getString(R.string.route_preview_distance_value, plan.distanceMeters / 1000));
    setText(R.id.areamap_route_v2_time,
            Utils.formatRoutingTime(activity, plan.plannedSeconds, R.dimen.text_size_body_3).toString());
    final RouteAltitudeData altitude = Framework.nativeGetRouteAltitudeData();
    setText(
        R.id.areamap_route_v2_ascent,
        altitude == null ? empty() : activity.getString(R.string.route_preview_altitude, altitude.getTotalAscent()));
    setText(R.id.areamap_route_v2_offline, offlineState());
    root.findViewById(R.id.areamap_route_v2_offline).setOnClickListener(v -> activity.onDownloadMapsOptionSelected());
    root.findViewById(R.id.areamap_route_v2_start).setOnClickListener(v -> start.run());
    root.findViewById(R.id.route_preview_overflow).setOnClickListener(v -> {
      final PopupMenu menu = new PopupMenu(activity, v);
      menu.getMenu()
          .add(R.string.save)
          .setEnabled(!RoutingController.get().isRouteSaved())
          .setOnMenuItemClickListener(item -> {
            save.run();
            return true;
          });
      menu.getMenu().add(R.string.areamap_rebuild_edit_route).setOnMenuItemClickListener(item -> {
        edit.run();
        return true;
      });
      // The existing reverse handler swaps endpoints; expose it only for a two-point route.
      final var points = Framework.nativeGetRoutePoints();
      if (points != null && points.length == 2)
        menu.getMenu().add(R.string.route_preview_reverse).setOnMenuItemClickListener(item -> {
          edit.run();
          RoutingController.get().swapPoints();
          return true;
        });
      menu.getMenu().add(R.string.route_preview_reset).setOnMenuItemClickListener(item -> {
        activity.handleBackPress();
        return true;
      });
      menu.show();
    });
    bindCheckpoints();
    bindGraph(altitude);
    root.findViewById(R.id.route_preview_show_map).setOnClickListener(v -> showRoute());
    final android.location.Location location = MwmApplication.from(activity).getLocationHelper().getSavedLocation();
    final View warning = root.findViewById(R.id.areamap_route_v2_warning);
    final boolean afterDark =
        location != null
        && DarknessUtil.routeTouchesDarkness(System.currentTimeMillis(), plan.plannedSeconds + plan.returnSeconds,
                                             location.getLatitude(), location.getLongitude());
    warning.setVisibility(afterDark ? View.VISIBLE : View.GONE);
    if (afterDark)
      setText(R.id.areamap_route_v2_warning, activity.getString(R.string.areamap_night_warning));
    updateWeather();
    final android.content.Context context = activity.getApplicationContext();
    new Thread(() -> {
      TripWeatherRepository.refreshForPoint(context, plan.weatherLat, plan.weatherLon, plan.weatherAltitudeMeters);
      activity.runOnUiThread(() -> {
        if (!mDisposed && !activity.isFinishing() && !activity.isDestroyed())
          updateWeather();
      });
    }, "AreaMapRouteWeather").start();
  }

  void dispose()
  {
    mDisposed = true;
    Framework.nativeRouteRemoveElevationActivePoint();
  }

  private String empty()
  {
    return mActivity.getString(R.string.route_preview_empty);
  }

  private String title(TripPlan plan)
  {
    final String start =
        plan.startTitle.isEmpty() ? mActivity.getString(R.string.areamap_route_start) : plan.startTitle;
    final String finish =
        plan.finishTitle.isEmpty() ? mActivity.getString(R.string.areamap_route_finish) : plan.finishTitle;
    return start + " — " + finish;
  }

  private void setText(int id, String value)
  {
    ((TextView) mRoot.findViewById(id)).setText(value);
  }
  private int color(int id)
  {
    return ContextCompat.getColor(mActivity, id);
  }
  private int dp(float value)
  {
    return Math.round(value * mActivity.getResources().getDisplayMetrics().density);
  }

  private void bindCheckpoints()
  {
    final LinearLayout list = mRoot.findViewById(R.id.route_preview_checkpoints);
    list.removeAllViews();
    int number = 1;
    for (TripPlan.Checkpoint checkpoint : mPlan.routeCheckpoints)
    {
      final LinearLayout row = new LinearLayout(mActivity);
      row.setOrientation(LinearLayout.HORIZONTAL);
      row.setGravity(android.view.Gravity.CENTER_VERTICAL);
      row.setPadding(dp(9), dp(8), dp(9), dp(8));
      row.setMinimumHeight(dp(52));
      final TextView marker = new TextView(mActivity);
      marker.setGravity(android.view.Gravity.CENTER);
      marker.setText(checkpoint.role < 0   ? mActivity.getString(R.string.route_preview_start_marker)
                     : checkpoint.role > 0 ? mActivity.getString(R.string.route_preview_finish_marker)
                                           : Integer.toString(number));
      marker.setTextSize(11);
      marker.setTextColor(color(checkpoint.role > 0 ? R.color.areamap_red : R.color.areamap_green));
      final GradientDrawable badge = new GradientDrawable();
      badge.setShape(GradientDrawable.OVAL);
      badge.setColor(color(R.color.areamap_surface));
      badge.setStroke(dp(1), marker.getCurrentTextColor());
      marker.setBackground(badge);
      row.addView(marker, new LinearLayout.LayoutParams(dp(26), dp(26)));
      final LinearLayout details = new LinearLayout(mActivity);
      details.setOrientation(LinearLayout.VERTICAL);
      final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
      params.setMarginStart(dp(9));
      row.addView(details, params);
      final String label = checkpoint.role < 0 ? mActivity.getString(R.string.areamap_route_start)
                         : checkpoint.role > 0 ? mActivity.getString(R.string.areamap_route_finish)
                                               : mActivity.getString(R.string.areamap_checkpoint_map_title, number);
      final TextView name = new TextView(mActivity);
      name.setText(checkpoint.title.isEmpty() ? label : label + " — " + checkpoint.title);
      name.setTextColor(color(R.color.areamap_text));
      name.setTextSize(12);
      name.setMaxLines(1);
      name.setEllipsize(android.text.TextUtils.TruncateAt.END);
      details.addView(name);
      final TextView metadata = new TextView(mActivity);
      final String elapsed = mActivity.getString(R.string.route_preview_elapsed, checkpoint.etaSeconds / 3600,
                                                 (checkpoint.etaSeconds % 3600) / 60);
      final String altitude = checkpoint.altitudeMeters < 0
                                ? empty()
                                : mActivity.getString(R.string.route_preview_altitude, checkpoint.altitudeMeters);
      metadata.setText(elapsed + "  ·  " + altitude + "  ·  "
                       + mActivity.getString(R.string.route_preview_coordinates, checkpoint.lat, checkpoint.lon));
      metadata.setTextColor(color(R.color.areamap_text_secondary));
      metadata.setTextSize(10);
      metadata.setMaxLines(2);
      details.addView(metadata);
      row.setContentDescription(name.getText() + ", " + metadata.getText());
      row.setFocusable(true);
      row.setOnClickListener(v -> {
        mCollapse.run();
        mRoot.postDelayed(() -> {
          if (!mDisposed)
            Framework.nativeZoomToPoint(checkpoint.lat, checkpoint.lon, 15, true);
        }, 350);
      });
      list.addView(row);
      if (checkpoint.role == 0)
        number++;
    }
  }

  private void bindGraph(RouteAltitudeData altitude)
  {
    final boolean available = altitude != null && altitude.getSize() > 1;
    mRoot.findViewById(R.id.route_preview_graph).setVisibility(available ? View.VISIBLE : View.GONE);
    mRoot.findViewById(R.id.route_preview_graph_empty).setVisibility(available ? View.GONE : View.VISIBLE);
    if (!available)
      return;
    final View graph = mRoot.findViewById(R.id.route_preview_graph);
    final RouteElevationChartController controller = new RouteElevationChartController(graph);
    controller.setData(altitude);
    controller.setListener(new RouteElevationChartController.ElevationSelectionListener() {
      @Override
      public void onElevationPointSelected(double meters)
      {
        Framework.nativeRouteSetElevationActivePoint(meters);
      }
      @Override
      public void onElevationPointDeselected()
      {
        Framework.nativeRouteRemoveElevationActivePoint();
      }
    });
    graph.findViewById(R.id.highest_altitude).setVisibility(View.GONE);
    graph.findViewById(R.id.lowest_altitude).setVisibility(View.GONE);
    setText(R.id.route_preview_start_altitude, Framework.nativeFormatAltitude(altitude.getAltitude(0)));
    setText(R.id.route_preview_finish_altitude,
            Framework.nativeFormatAltitude(altitude.getAltitude(altitude.getSize() - 1)));
    final LineChart chart = graph.findViewById(R.id.elevation_profile_chart);
    chart.setBackgroundColor(color(R.color.areamap_surface_2));
    final LineDataSet line = (LineDataSet) chart.getData().getDataSetByIndex(0);
    line.setColor(color(R.color.areamap_green));
    line.setLineWidth(1.5f);
    line.setDrawFilled(false);
    final List<Entry> marks = new ArrayList<>();
    final List<Integer> circleColors = new ArrayList<>();
    int number = 1;
    for (TripPlan.Checkpoint checkpoint : mPlan.routeCheckpoints)
    {
      final float x = (float) (mPlan.distanceMeters <= 0 ? 0
                                                         : checkpoint.distanceMeters / mPlan.distanceMeters
                                                               * altitude.getDistance(altitude.getSize() - 1));
      final String label = checkpoint.role == 0 ? Integer.toString(number++)
                         : checkpoint.role < 0  ? mActivity.getString(R.string.route_preview_start_marker)
                                                : mActivity.getString(R.string.route_preview_finish_marker);
      marks.add(new Entry(x, checkpoint.altitudeMeters, label));
      circleColors.add(color(checkpoint.role > 0 ? R.color.areamap_red : R.color.areamap_green));
    }
    final LineDataSet dots = new LineDataSet(marks, "");
    dots.setColor(android.graphics.Color.TRANSPARENT);
    dots.setCircleColors(circleColors);
    dots.setCircleHoleColor(color(R.color.areamap_surface_2));
    dots.setCircleRadius(4f);
    dots.setCircleHoleRadius(2f);
    dots.setDrawValues(true);
    dots.setValueTextColor(color(R.color.areamap_text));
    dots.setValueTextSize(9);
    dots.setHighlightEnabled(false);
    dots.setValueFormatter(new ValueFormatter() {
      @Override
      public String getPointLabel(Entry entry)
      {
        return (String) entry.getData();
      }
    });
    chart.getData().addDataSet(dots);
    chart.getXAxis().setTextColor(color(R.color.areamap_text_secondary));
    chart.getAxisLeft().setGridColor(color(R.color.areamap_stroke));
    chart.notifyDataSetChanged();
    chart.invalidate();
  }

  private String offlineState()
  {
    final JunctionInfo[] geometry = Framework.nativeGetRouteJunctionPoints(150.0);
    if (geometry == null || geometry.length == 0)
      return mActivity.getString(R.string.route_preview_unknown);
    final java.util.Set<String> checked = new java.util.HashSet<>();
    for (JunctionInfo point : geometry)
    {
      final String country = MapManager.nativeFindCountry(point.mLat, point.mLon);
      if (country == null)
        return mActivity.getString(R.string.route_preview_unknown);
      if (!checked.add(country))
        continue;
      final int status = MapManager.nativeGetStatus(country);
      if (status != CountryItem.STATUS_DONE && status != CountryItem.STATUS_UPDATABLE)
        return mActivity.getString(R.string.route_preview_download);
    }
    return mActivity.getString(R.string.route_preview_available);
  }

  private void updateWeather()
  {
    setText(R.id.route_preview_weather_details, TripWeatherNotifier.summaryForPlan(mActivity, mPlan));
    final boolean cached = TripWeatherRepository.hasForecastForPoint(mActivity, mPlan.weatherLat, mPlan.weatherLon,
                                                                     mPlan.weatherAltitudeMeters);
    final TripWeatherRepository.Hour hour =
        cached
            ? TripWeatherRepository.closestHour(mActivity, System.currentTimeMillis() + mPlan.weatherEtaSeconds * 1000L)
            : null;
    final String temperature =
        hour == null ? empty() : mActivity.getString(R.string.route_preview_temperature, hour.temperatureC);
    setText(R.id.areamap_route_v2_weather, temperature);
    setText(R.id.route_preview_temperature, temperature);
    setText(R.id.route_preview_condition, hour == null ? mActivity.getString(R.string.areamap_weather_no_cache)
                                                       : TripWeatherNotifier.weatherLabel(mActivity, hour.weatherCode));
    setText(R.id.route_preview_wind,
            hour == null ? empty() : mActivity.getString(R.string.route_preview_wind, hour.windKmh));
    final ImageView icon = mRoot.findViewById(R.id.route_preview_weather_icon);
    icon.setVisibility(hour == null ? View.GONE : View.VISIBLE);
    if (hour != null)
      icon.setImageResource(hour.weatherCode == 0    ? R.drawable.ic_preview_sun
                            : hour.weatherCode <= 48 ? R.drawable.ic_preview_cloud
                                                     : R.drawable.ic_preview_rain);
  }

  void showRoute()
  {
    mCollapse.run();
    mRoot.postDelayed(() -> {
      if (mDisposed)
        return;
      final JunctionInfo[] geometry = Framework.nativeGetRouteJunctionPoints(150.0);
      if (geometry == null || geometry.length == 0)
        return;
      final double[][] points = new double[geometry.length + mPlan.routeCheckpoints.size()][2];
      for (int i = 0; i < geometry.length; i++)
      {
        points[i][0] = geometry[i].mLat;
        points[i][1] = geometry[i].mLon;
      }
      for (int i = 0; i < mPlan.routeCheckpoints.size(); i++)
      {
        points[geometry.length + i][0] = mPlan.routeCheckpoints.get(i).lat;
        points[geometry.length + i][1] = mPlan.routeCheckpoints.get(i).lon;
      }
      final View sheet = mRoot.findViewById(R.id.routing_sheet_frame);
      final boolean landscape = mActivity.getResources().getConfiguration().orientation
                             == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
      final RoutePreviewViewport viewport =
          RoutePreviewViewport.fit(points, Math.max(1, mRoot.getWidth() - (landscape ? sheet.getWidth() : 0) - dp(64)),
                                   Math.max(1, (landscape ? mRoot.getHeight() : sheet.getTop()) - dp(100)));
      Framework.nativeZoomToPoint(viewport.lat, viewport.lon, viewport.zoom, true);
    }, 350);
  }
}
