package app.organicmaps.safety;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.sdk.Framework;
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
    public final double distanceMeters;
    public final int altitudeMeters;
    public final int etaSeconds;

    public Checkpoint(double distanceMeters, int altitudeMeters, int etaSeconds)
    {
      this.distanceMeters = distanceMeters;
      this.altitudeMeters = altitudeMeters;
      this.etaSeconds = etaSeconds;
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
  @NonNull public final List<Checkpoint> checkpoints;

  private TripPlan(@NonNull String startTitle, @NonNull String finishTitle,
                   double startLat, double startLon, double finishLat, double finishLon,
                   double distanceMeters, int plannedSeconds, @NonNull List<Checkpoint> checkpoints)
  {
    this.startTitle = startTitle;
    this.finishTitle = finishTitle;
    this.startLat = startLat;
    this.startLon = startLon;
    this.finishLat = finishLat;
    this.finishLon = finishLon;
    this.distanceMeters = distanceMeters;
    this.plannedSeconds = plannedSeconds;
    this.checkpoints = Collections.unmodifiableList(new ArrayList<>(checkpoints));
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
    final int plannedSeconds =
        HikingTiming.conservativeSeconds(info.totalTimeInSeconds, distanceMeters, ascent);

    final List<Checkpoint> checkpoints = new ArrayList<>();
    if (altitude != null && altitude.getSize() > 1)
    {
      final RouteSafetyAnalysis.Result analysis = RouteSafetyAnalysis.analyze(altitude, plannedSeconds);
      for (RouteSafetyAnalysis.Checkpoint checkpoint : analysis.checkpoints)
        checkpoints.add(new Checkpoint(checkpoint.distanceMeters, checkpoint.altitudeMeters, checkpoint.etaSeconds));
    }
    else
    {
      final int count = Math.min(5, Math.max(1, plannedSeconds / (75 * 60)));
      for (int i = 1; i <= count; i++)
      {
        final double fraction = i / (count + 1.0);
        checkpoints.add(new Checkpoint(distanceMeters * fraction, -1,
                                       Math.max(60, (int) Math.round(plannedSeconds * fraction))));
      }
    }

    return new TripPlan(title(start), title(finish), start.mLat, start.mLon, finish.mLat, finish.mLon,
                        distanceMeters, plannedSeconds, checkpoints);
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
