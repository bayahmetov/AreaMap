package app.organicmaps.safety;

import android.content.Context;
import app.organicmaps.sdk.Router;
import app.organicmaps.sdk.bookmarks.data.MapObject;
import app.organicmaps.sdk.routing.RoutingController;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

final class GuideRouteState
{
  private GuideRouteState() {}

  static RouteGuideContext read(Context context)
  {
    final Set<String> tags = new HashSet<>();
    String source = "none";
    final GpxNavigation gpx = GpxNavigation.current;
    final RoutingController routing = RoutingController.get();
    final TripSafety trip = TripSafety.get(context);
    if (gpx != null)
    {
      source = "gpx"; tags.add("gpx"); tags.add("hiking"); tags.add("offline");
      for (double[] point : gpx.track.points)
      {
        RouteGuideContext.addLocation(tags, point[0], point[1]);
        if (Double.isFinite(point[2]) && point[2] >= 2500) tags.add("high_altitude");
      }
    }
    else if (routing.isBuilt() || routing.isNavigating())
    {
      source = "organic_maps";
      if (routing.getLastRouterType() == Router.Pedestrian) tags.add("hiking");
      final MapObject destination = routing.getEndPoint();
      if (destination != null) RouteGuideContext.addLocation(tags, destination.getLat(), destination.getLon());
    }
    else if (trip.hasActiveTrip())
    {
      source = "registered_trip"; tags.add("hiking");
      RouteGuideContext.addLocation(tags, trip.weatherLat(), trip.weatherLon());
      if (trip.weatherAltitudeMeters() >= 2500) tags.add("high_altitude");
    }
    final int month = Calendar.getInstance().get(Calendar.MONTH);
    if (month <= Calendar.FEBRUARY || month >= Calendar.NOVEMBER) tags.add("cold");
    if (month >= Calendar.JUNE && month <= Calendar.AUGUST) tags.add("hot");
    return new RouteGuideContext(!source.equals("none"), source, tags);
  }
}
