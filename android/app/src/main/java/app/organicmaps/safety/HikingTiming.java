package app.organicmaps.safety;

import androidx.annotation.NonNull;
import app.organicmaps.sdk.util.Distance;
import java.util.Locale;

/**
 * Small offline timing model for hiking routes.
 *
 * The route engine remains the primary source of ETA. We also apply a conservative
 * hiking model (4.5 km/h on flat terrain + 10 minutes per 100 m ascent) and use
 * whichever estimate is longer, so steep routes are not presented as unrealistically fast.
 */
public final class HikingTiming
{
  private static final double FLAT_SPEED_MPS = 4.5 / 3.6;
  private static final double ASCENT_SECONDS_PER_METER = 6.0;

  private HikingTiming() {}

  public static double toMeters(@NonNull Distance distance)
  {
    switch (distance.mUnits)
    {
    case Kilometers: return distance.mDistance * 1000.0;
    case Feet: return distance.mDistance * 0.3048;
    case Miles: return distance.mDistance * 1609.344;
    case Meters:
    default: return distance.mDistance;
    }
  }

  public static int conservativeSeconds(int engineSeconds, double distanceMeters, double ascentMeters)
  {
    final int hikingSeconds =
        (int) Math.round(distanceMeters / FLAT_SPEED_MPS + Math.max(0.0, ascentMeters) * ASCENT_SECONDS_PER_METER);
    return Math.max(engineSeconds, hikingSeconds);
  }

  @NonNull
  public static String formatPace(double secondsPerKm)
  {
    if (!Double.isFinite(secondsPerKm) || secondsPerKm <= 0)
      return "—";
    final int total = (int) Math.round(secondsPerKm);
    return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60);
  }

  public static double secondsPerKm(int seconds, double distanceMeters)
  {
    if (seconds <= 0 || distanceMeters < 100.0)
      return Double.NaN;
    return seconds / (distanceMeters / 1000.0);
  }

  public static int scheduleDeltaSeconds(long elapsedMs, double completedMeters, double plannedSecondsPerKm)
  {
    if (elapsedMs <= 0 || completedMeters < 100.0 || !Double.isFinite(plannedSecondsPerKm))
      return 0;
    final double actualSeconds = elapsedMs / 1000.0;
    final double plannedSeconds = plannedSecondsPerKm * (completedMeters / 1000.0);
    return (int) Math.round(actualSeconds - plannedSeconds);
  }
}
