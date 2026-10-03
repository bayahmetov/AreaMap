package app.organicmaps.routing;

/** Web Mercator bounds used only to present the already-built native route. */
final class RoutePreviewViewport
{
  final double lat;
  final double lon;
  final int zoom;

  private RoutePreviewViewport(double lat, double lon, int zoom)
  {
    this.lat = lat;
    this.lon = lon;
    this.zoom = zoom;
  }

  static RoutePreviewViewport fit(double[][] points, int width, int height)
  {
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minY = Double.MAX_VALUE;
    double maxY = -Double.MAX_VALUE;
    final double reference = points[0][1];
    for (double[] point : points)
    {
      double longitude = point[1];
      while (longitude - reference > 180)
        longitude -= 360;
      while (longitude - reference < -180)
        longitude += 360;
      final double x = (longitude + 180) / 360;
      final double latitude = Math.max(-85, Math.min(85, point[0]));
      final double sine = Math.sin(Math.toRadians(latitude));
      final double y = 0.5 - Math.log((1 + sine) / (1 - sine)) / (4 * Math.PI);
      minX = Math.min(minX, x);
      maxX = Math.max(maxX, x);
      minY = Math.min(minY, y);
      maxY = Math.max(maxY, y);
    }
    final double scale = Math.min(Math.max(1, width) / (256 * Math.max(1e-9, maxX - minX)),
                                  Math.max(1, height) / (256 * Math.max(1e-9, maxY - minY)));
    // Native draw scale counts the whole-world scale as 1 rather than tile zoom 0.
    final int zoom = Math.max(1, Math.min(18, (int) Math.floor(Math.log(scale) / Math.log(2)) + 1));
    double lon = (minX + maxX) / 2 * 360 - 180;
    while (lon > 180)
      lon -= 360;
    while (lon < -180)
      lon += 360;
    final double lat = Math.toDegrees(Math.atan(Math.sinh(Math.PI * (1 - minY - maxY))));
    return new RoutePreviewViewport(lat, lon, zoom);
  }
}
