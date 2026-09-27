package app.organicmaps.safety;

/** Geometry for following the original GPX line, without snapping it to the road graph. */
public final class GpxTrack
{
  private static final double EARTH = 6371000.0;
  public final double[][] points;
  public final double[] distance;
  public final double length;
  public final double ascent;
  public final boolean hasElevation;

  public GpxTrack(double[][] source)
  {
    if (source.length < 2)
      throw new IllegalArgumentException("Track requires at least two points");
    points = new double[source.length][];
    distance = new double[source.length];
    double climb = 0;
    boolean elevations = true;
    for (int i = 0; i < source.length; i++)
    {
      points[i] = source[i].clone();
      final double[] p = points[i];
      if (p.length != 3 || !Double.isFinite(p[0]) || !Double.isFinite(p[1])
          || Math.abs(p[0]) > 90 || Math.abs(p[1]) > 180)
        throw new IllegalArgumentException("Invalid track coordinates");
      elevations &= Double.isFinite(p[2]);
      if (i == 0)
        continue;
      final double[] prev = points[i - 1];
      distance[i] = distance[i - 1] + meters(prev[0], prev[1], p[0], p[1]);
      if (Double.isFinite(prev[2]) && Double.isFinite(p[2]))
        climb += Math.max(0, p[2] - prev[2]);
    }
    length = distance[distance.length - 1];
    if (length < 1)
      throw new IllegalArgumentException("Empty track");
    ascent = climb;
    hasElevation = elevations;
  }

  public static final class Position
  {
    public final double progress;
    public final double offset;
    Position(double progress, double offset)
    {
      this.progress = progress;
      this.offset = offset;
    }
  }

  /** Prefer continuity at crossings; permit reacquisition after a larger movement. */
  public Position locate(double lat, double lon, double previous, double window)
  {
    double best = Double.POSITIVE_INFINITY;
    double progress = 0;
    for (int i = 1; i < points.length; i++)
    {
      if (previous >= 0 && (distance[i] < previous - window || distance[i - 1] > previous + window))
        continue;
      final double ax = Math.toRadians(wrap(points[i - 1][1] - lon)) * EARTH * Math.cos(Math.toRadians(lat));
      final double ay = Math.toRadians(points[i - 1][0] - lat) * EARTH;
      final double bx = Math.toRadians(wrap(points[i][1] - lon)) * EARTH * Math.cos(Math.toRadians(lat));
      final double by = Math.toRadians(points[i][0] - lat) * EARTH;
      final double dx = bx - ax;
      final double dy = by - ay;
      final double square = dx * dx + dy * dy;
      final double t = square == 0 ? 0 : Math.max(0, Math.min(1, -(ax * dx + ay * dy) / square));
      final double offset = Math.hypot(ax + t * dx, ay + t * dy);
      if (offset < best)
      {
        best = offset;
        progress = distance[i - 1] + t * (distance[i] - distance[i - 1]);
      }
    }
    return new Position(progress, best);
  }

  private static double wrap(double longitude)
  {
    return (longitude + 540) % 360 - 180;
  }

  private static double meters(double lat1, double lon1, double lat2, double lon2)
  {
    double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.pow(Math.sin(Math.toRadians(wrap(lon2 - lon1)) / 2), 2);
    return 2 * EARTH * Math.asin(Math.sqrt(Math.min(1, a)));
  }
}
