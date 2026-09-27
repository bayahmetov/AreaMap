package app.organicmaps.safety;

/** Run with javac/java alongside GpxTrack.java; no Android SDK is required. */
public final class AreaMapGpxGeometryCheck
{
  private static void near(double actual, double expected, double tolerance)
  {
    if (Math.abs(actual - expected) > tolerance)
      throw new AssertionError(actual + " != " + expected);
  }

  public static void main(String[] args)
  {
    GpxTrack straight = new GpxTrack(new double[][] {{0, 0, 100}, {0, .01, 200}});
    near(straight.length, 1112, 2);
    near(straight.ascent, 100, .01);
    GpxTrack.Position mid = straight.locate(.001, .005, -1, Double.POSITIVE_INFINITY);
    near(mid.progress, straight.length / 2, 1);
    near(mid.offset, 111.2, 1);
    near(straight.locate(0, -.001, -1, Double.POSITIVE_INFINITY).progress, 0, .01);
    near(straight.locate(0, .02, -1, Double.POSITIVE_INFINITY).progress, straight.length, .01);

    GpxTrack crossing = new GpxTrack(new double[][] {
        {-.01, -.01, 0}, {.01, .01, 0}, {.01, -.01, 0}, {-.01, .01, 0}});
    double firstCrossing = crossing.distance[1] / 2;
    near(crossing.locate(0, 0, firstCrossing, 200).progress, firstCrossing, 1);
    double lastCrossing = (crossing.distance[2] + crossing.distance[3]) / 2;
    near(crossing.locate(0, 0, lastCrossing, 200).progress, lastCrossing, 1);

    GpxTrack dateline = new GpxTrack(new double[][] {{0, 179.99, 0}, {0, -179.99, 0}});
    near(dateline.length, 2224, 3);
    near(dateline.locate(0, 180, -1, Double.POSITIVE_INFINITY).progress, dateline.length / 2, 1);

    GpxTrack missing = new GpxTrack(new double[][] {{0, 0, Double.NaN}, {0, .01, Double.NaN}});
    if (missing.hasElevation || missing.ascent != 0)
      throw new AssertionError("Missing heights must remain unknown");
    try
    {
      new GpxTrack(new double[][] {{91, 0, 0}, {0, 0, 0}});
      throw new AssertionError("Invalid latitude accepted");
    }
    catch (IllegalArgumentException expected) {}
    try
    {
      new GpxTrack(new double[][] {{0, 0, 0}, {0, 0, 0}});
      throw new AssertionError("Zero-length track accepted");
    }
    catch (IllegalArgumentException expected) {}
    System.out.println("PASS: distance, ascent, projection, endpoints, crossings, dateline, missing heights, invalid input");
  }
}
