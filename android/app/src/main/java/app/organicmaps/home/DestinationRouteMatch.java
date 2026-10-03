package app.organicmaps.home;

/** Relates real route coordinates, never destination names. */
public final class DestinationRouteMatch
{
  private DestinationRouteMatch() {}

  public static boolean near(double lat, double lon, double pointLat, double pointLon)
  {
    return near(lat, lon, pointLat, pointLon, 100);
  }

  static boolean near(double lat, double lon, double pointLat, double pointLon, double radiusMeters)
  {
    if (!Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(pointLat) || !Double.isFinite(pointLon))
      return false;
    final double dLat = Math.toRadians(pointLat - lat);
    final double dLon = Math.toRadians(pointLon - lon);
    final double a =
        Math.pow(Math.sin(dLat / 2), 2)
        + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(pointLat)) * Math.pow(Math.sin(dLon / 2), 2);
    return 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a))) <= radiusMeters;
  }
}
