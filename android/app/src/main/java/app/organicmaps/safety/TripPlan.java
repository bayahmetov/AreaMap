package app.organicmaps.safety;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.sdk.Framework;
import app.organicmaps.sdk.routing.JunctionInfo;
import app.organicmaps.sdk.routing.RouteAltitudeData;
import app.organicmaps.sdk.routing.RouteMarkData;
import app.organicmaps.sdk.routing.RouteMarkType;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.sdk.routing.RoutingInfo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable snapshot of the route at the moment the user starts a hike. */
public final class TripPlan
{
  public static final class Checkpoint
  {
    public final String title;
    public final int role; // -1 start, 0 intermediate, 1 finish.
    public final double distanceMeters;
    public final int altitudeMeters;
    public final int etaSeconds;
    public final double lat;
    public final double lon;

    public Checkpoint(double distanceMeters, int altitudeMeters, int etaSeconds, double lat, double lon)
    {
      this(distanceMeters, altitudeMeters, etaSeconds, lat, lon, "", 0);
    }

    public Checkpoint(double distanceMeters, int altitudeMeters, int etaSeconds, double lat, double lon,
                      String title, int role)
    {
      this.title = title;
      this.role = role;
      this.distanceMeters = distanceMeters;
      this.altitudeMeters = altitudeMeters;
      this.etaSeconds = etaSeconds;
      this.lat = lat;
      this.lon = lon;
    }
  }

  @NonNull public final String startTitle;
  @NonNull public final String finishTitle;
  public final double startLat;
  public final double startLon;
  public final double finishLat;
  public final double finishLon;
  public final double distanceMeters;
  public final int plannedSeconds;
  public final int returnSeconds;
  @NonNull public final List<Checkpoint> checkpoints;
  @NonNull public final List<Checkpoint> routeCheckpoints;

  /**
   * Point used for the trip weather forecast.
   *
   * When an elevation profile is available this is the highest point on the route.
   * Otherwise AreaMap falls back to the route destination.
   */
  public final double weatherLat;
  public final double weatherLon;
  public final int weatherAltitudeMeters;
  public final int weatherEtaSeconds;
  public final boolean weatherAtHighestPoint;

  private TripPlan(@NonNull String startTitle, @NonNull String finishTitle,
                   double startLat, double startLon, double finishLat, double finishLon,
                   double distanceMeters, int plannedSeconds, int returnSeconds,
                   @NonNull List<Checkpoint> checkpoints, @Nullable RouteAltitudeData altitude,
                   double weatherLat, double weatherLon, int weatherAltitudeMeters,
                   int weatherEtaSeconds, boolean weatherAtHighestPoint)
  {
    this.startTitle = startTitle;
    this.finishTitle = finishTitle;
    this.startLat = startLat;
    this.startLon = startLon;
    this.finishLat = finishLat;
    this.finishLon = finishLon;
    this.distanceMeters = distanceMeters;
    this.plannedSeconds = plannedSeconds;
    this.returnSeconds = returnSeconds;
    this.checkpoints = Collections.unmodifiableList(new ArrayList<>(checkpoints));
    final List<Checkpoint> preview = new ArrayList<>();
    final boolean hasAltitude = altitude != null && altitude.getSize() > 0;
    preview.add(new Checkpoint(0, hasAltitude ? altitude.getAltitude(0) : -1, 0,
                              startLat, startLon, startTitle, -1));
    preview.addAll(this.checkpoints);
    preview.add(new Checkpoint(distanceMeters,
                              hasAltitude ? altitude.getAltitude(altitude.getSize() - 1) : -1,
                              plannedSeconds, finishLat, finishLon, finishTitle, 1));
    this.routeCheckpoints = Collections.unmodifiableList(preview);
    this.weatherLat = weatherLat;
    this.weatherLon = weatherLon;
    this.weatherAltitudeMeters = weatherAltitudeMeters;
    this.weatherEtaSeconds = weatherEtaSeconds;
    this.weatherAtHighestPoint = weatherAtHighestPoint;
  }

  @Nullable
  public static TripPlan current()
  {
    final RoutingInfo info = RoutingController.get().getCachedRoutingInfo();
    if (info == null)
      return null;

    RouteMarkData start = null;
    RouteMarkData finish = null;
    final RouteMarkData[] points = Framework.nativeGetRoutePoints();
    if (points != null)
    {
      for (RouteMarkData point : points)
      {
        if (point.mPointType == RouteMarkType.Start)
          start = point;
        else if (point.mPointType == RouteMarkType.Finish)
          finish = point;
      }
    }
    if (start == null || finish == null)
      return null;

    final double distanceMeters = HikingTiming.toMeters(info.distToTarget);
    final RouteAltitudeData altitude = Framework.nativeGetRouteAltitudeData();
    final double ascent = altitude == null ? 0.0 : altitude.getTotalAscent();
    final double reverseAscent = altitude == null ? ascent : altitude.getTotalDescent();
    final int plannedSeconds =
        HikingTiming.conservativeSeconds(info.totalTimeInSeconds, distanceMeters, ascent);
    // Until a dedicated reverse route is built, estimate returning along the same trail.
    // The outbound descent becomes ascent on the way back.
    final int returnSeconds =
        Math.max(info.totalTimeInSeconds, HikingTiming.estimateSeconds(distanceMeters, reverseAscent));

    final List<Checkpoint> checkpoints = new ArrayList<>();
    double weatherLat = finish.mLat;
    double weatherLon = finish.mLon;
    int weatherAltitudeMeters = -1;
    int weatherEtaSeconds = plannedSeconds;
    boolean weatherAtHighestPoint = false;

    if (altitude != null && altitude.getSize() > 1)
    {
      final RouteSafetyAnalysis.Result analysis = RouteSafetyAnalysis.analyze(altitude, plannedSeconds);
      final double profileDistance = Math.max(1.0, altitude.getDistance(altitude.getSize() - 1));
      for (RouteSafetyAnalysis.Checkpoint checkpoint : analysis.checkpoints)
      {
        final double fraction = Math.max(0.0, Math.min(1.0, checkpoint.distanceMeters / profileDistance));
        final double[] point = pointAtRouteFraction(fraction);
        if (point == null)
          continue;
        final double lat = point[0];
        final double lon = point[1];
        checkpoints.add(new Checkpoint(checkpoint.distanceMeters, checkpoint.altitudeMeters,
                                       checkpoint.etaSeconds, lat, lon));
      }

      int highestIndex = 0;
      for (int i = 1; i < altitude.getSize(); i++)
      {
        if (altitude.getAltitude(i) > altitude.getAltitude(highestIndex))
          highestIndex = i;
      }

      final double highestDistance = Math.max(0.0, altitude.getDistance(highestIndex));
      final double routeFraction = Math.max(0.0, Math.min(1.0, highestDistance / profileDistance));
      final double[] highestPoint = pointAtRouteFraction(routeFraction);
      if (highestPoint != null)
      {
        weatherLat = highestPoint[0];
        weatherLon = highestPoint[1];
        weatherAltitudeMeters = altitude.getAltitude(highestIndex);
        weatherEtaSeconds = Math.max(0, Math.min(plannedSeconds,
            (int) Math.round(plannedSeconds * routeFraction)));
        weatherAtHighestPoint = true;
      }
    }
    else
    {
      final int count = Math.min(5, Math.max(1, plannedSeconds / (75 * 60)));
      for (int i = 1; i <= count; i++)
      {
        final double fraction = i / (count + 1.0);
        final double[] point = pointAtRouteFraction(fraction);
        if (point == null)
          continue;
        final double lat = point[0];
        final double lon = point[1];
        checkpoints.add(new Checkpoint(distanceMeters * fraction, -1,
                                       Math.max(60, (int) Math.round(plannedSeconds * fraction)), lat, lon));
      }
    }

    // User-selected intermediate route points take priority over generated rest stops.
    final List<RouteMarkData> intermediate = new ArrayList<>();
    for (RouteMarkData point : points)
      if (point.mPointType == RouteMarkType.Intermediate)
        intermediate.add(point);
    intermediate.sort(java.util.Comparator.comparingInt(point -> point.mIntermediateIndex));
    if (!intermediate.isEmpty())
    {
      checkpoints.clear();
      final JunctionInfo[] geometry = Framework.nativeGetRouteJunctionPoints(150.0);
      double minimumFraction = 0;
      for (RouteMarkData point : intermediate)
      {
        final double fraction = Math.max(minimumFraction, fractionAtPoint(geometry, point.mLat, point.mLon));
        minimumFraction = fraction;
        final int height = altitudeAtFraction(altitude, fraction);
        checkpoints.add(new Checkpoint(distanceMeters * fraction, height,
                                       (int) Math.round(plannedSeconds * fraction), point.mLat, point.mLon,
                                       title(point), 0));
      }
    }

    return new TripPlan(title(start), title(finish), start.mLat, start.mLon, finish.mLat, finish.mLon,
                        distanceMeters, plannedSeconds, returnSeconds, checkpoints, altitude,
                        weatherLat, weatherLon, weatherAltitudeMeters, weatherEtaSeconds,
                        weatherAtHighestPoint);
  }

  private static double fractionAtPoint(@Nullable JunctionInfo[] geometry, double lat, double lon)
  {
    if (geometry == null || geometry.length < 2)
      return 0;
    double total = 0;
    double nearestDistance = Double.MAX_VALUE;
    double nearestAlong = 0;
    final float[] distance = new float[1];
    for (int i = 1; i < geometry.length; i++)
    {
      final JunctionInfo a = geometry[i - 1];
      final JunctionInfo b = geometry[i];
      Location.distanceBetween(a.mLat, a.mLon, b.mLat, b.mLon, distance);
      final double length = distance[0];
      final double cos = Math.cos(Math.toRadians(lat));
      final double dx = (b.mLon - a.mLon) * cos;
      final double dy = b.mLat - a.mLat;
      final double denominator = dx * dx + dy * dy;
      final double local = denominator == 0 ? 0 : Math.max(0, Math.min(1,
          (((lon - a.mLon) * cos) * dx + (lat - a.mLat) * dy) / denominator));
      Location.distanceBetween(lat, lon, a.mLat + (b.mLat - a.mLat) * local,
                               a.mLon + (b.mLon - a.mLon) * local, distance);
      if (distance[0] < nearestDistance)
      {
        nearestDistance = distance[0];
        nearestAlong = total + length * local;
      }
      total += length;
    }
    return total == 0 ? 0 : nearestAlong / total;
  }

  private static int altitudeAtFraction(@Nullable RouteAltitudeData altitude, double fraction)
  {
    if (altitude == null || altitude.getSize() == 0)
      return -1;
    final double target = altitude.getDistance(altitude.getSize() - 1) * fraction;
    for (int i = 1; i < altitude.getSize(); i++)
      if (altitude.getDistance(i) >= target)
      {
        final double span = altitude.getDistance(i) - altitude.getDistance(i - 1);
        final double local = span <= 0 ? 0 : (target - altitude.getDistance(i - 1)) / span;
        return (int) Math.round(altitude.getAltitude(i - 1)
                               + (altitude.getAltitude(i) - altitude.getAltitude(i - 1)) * local);
      }
    return altitude.getAltitude(altitude.getSize() - 1);
  }

  @Nullable
  private static double[] pointAtRouteFraction(double fraction)
  {
    final JunctionInfo[] points = Framework.nativeGetRouteJunctionPoints(150.0);
    if (points == null || points.length == 0)
      return null;
    if (points.length == 1 || fraction <= 0.0)
      return new double[] {points[0].mLat, points[0].mLon};
    if (fraction >= 1.0)
    {
      final JunctionInfo last = points[points.length - 1];
      return new double[] {last.mLat, last.mLon};
    }

    final double[] segmentMeters = new double[points.length - 1];
    double totalMeters = 0.0;
    final float[] distance = new float[1];
    for (int i = 0; i < points.length - 1; i++)
    {
      Location.distanceBetween(points[i].mLat, points[i].mLon,
                               points[i + 1].mLat, points[i + 1].mLon, distance);
      segmentMeters[i] = Math.max(0.0, distance[0]);
      totalMeters += segmentMeters[i];
    }
    if (totalMeters <= 0.0)
      return null;

    final double targetMeters = totalMeters * fraction;
    double traversed = 0.0;
    for (int i = 0; i < segmentMeters.length; i++)
    {
      final double segment = segmentMeters[i];
      if (traversed + segment < targetMeters)
      {
        traversed += segment;
        continue;
      }

      final double local = segment <= 0.0 ? 0.0 : (targetMeters - traversed) / segment;
      final double lat = points[i].mLat + (points[i + 1].mLat - points[i].mLat) * local;
      final double lon = points[i].mLon + (points[i + 1].mLon - points[i].mLon) * local;
      return new double[] {lat, lon};
    }

    final JunctionInfo last = points[points.length - 1];
    return new double[] {last.mLat, last.mLon};
  }

  @NonNull
  private static String title(@NonNull RouteMarkData point)
  {
    if (point.mTitle != null && !point.mTitle.trim().isEmpty())
      return point.mTitle.trim();
    if (point.mSubtitle != null && !point.mSubtitle.trim().isEmpty())
      return point.mSubtitle.trim();
    return "";
  }
}
