package app.organicmaps.routing;

/** Presentation of existing engine/GPX progress; never computes a new route or GPS match. */
public final class HikePanelProgress
{
  private HikePanelProgress() {}

  public static double fraction(double completionPercent)
  {
    return Double.isFinite(completionPercent) ? Math.max(0, Math.min(1, completionPercent / 100)) : Double.NaN;
  }

  public static int nextCheckpoint(double[] distances, double completed, boolean hasProgress)
  {
    if (!hasProgress || !Double.isFinite(completed))
      return distances.length == 0 ? -1 : 0;
    for (int i = 0; i < distances.length; i++)
      if (distances[i] > completed + 50.0) // Same arrival tolerance as TripSafety's checkpoint summary.
        return i;
    return -1;
  }

  public static int checkpointSeconds(double checkpoint, double completed, double remaining, int remainingSeconds)
  {
    if (!Double.isFinite(completed) || remaining <= 0 || remainingSeconds < 0)
      return -1;
    return (int) Math.round(Math.max(0, Math.min(1, (checkpoint - completed) / remaining)) * remainingSeconds);
  }

  public static long scheduleDriftSeconds(long now, long started, int planned, int breakSeconds, double fraction)
  {
    if (started <= 0 || planned <= 0 || !Double.isFinite(fraction))
      return Long.MIN_VALUE;
    final long elapsed = Math.max(0, (now - started) / 1000 - breakSeconds);
    return elapsed - Math.round(planned * Math.max(0, Math.min(1, fraction)));
  }
}
