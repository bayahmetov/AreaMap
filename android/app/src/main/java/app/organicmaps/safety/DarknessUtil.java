package app.organicmaps.safety;

import androidx.annotation.NonNull;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Lightweight sunset approximation for offline route warnings.
 * Accuracy is sufficient for a safety hint; it is not an astronomical service.
 */
public final class DarknessUtil
{
  private DarknessUtil() {}

  public static boolean finishesAfterDark(long nowEpochMs, int etaSeconds, double lat, double lon)
  {
    final ZonedDateTime finish =
        Instant.ofEpochMilli(nowEpochMs).atZone(ZoneId.systemDefault()).plusSeconds(Math.max(0, etaSeconds));
    final ZonedDateTime sunset = sunset(finish.toLocalDate(), lat, lon, finish.getZone());
    return sunset != null && finish.isAfter(sunset);
  }

  private static ZonedDateTime sunset(@NonNull LocalDate date, double latitude, double longitude, @NonNull ZoneId zone)
  {
    // NOAA-style approximation.
    final int day = date.getDayOfYear();
    final double lngHour = longitude / 15.0;
    final double t = day + ((18.0 - lngHour) / 24.0);
    final double m = 0.9856 * t - 3.289;
    double l = m + 1.916 * Math.sin(Math.toRadians(m)) + 0.020 * Math.sin(Math.toRadians(2 * m)) + 282.634;
    l = normalize(l, 360.0);

    double ra = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(l))));
    ra = normalize(ra, 360.0);
    final double lQuadrant = Math.floor(l / 90.0) * 90.0;
    final double raQuadrant = Math.floor(ra / 90.0) * 90.0;
    ra = (ra + lQuadrant - raQuadrant) / 15.0;

    final double sinDec = 0.39782 * Math.sin(Math.toRadians(l));
    final double cosDec = Math.cos(Math.asin(sinDec));
    final double cosH =
        (Math.cos(Math.toRadians(90.833)) - sinDec * Math.sin(Math.toRadians(latitude)))
        / (cosDec * Math.cos(Math.toRadians(latitude)));

    if (cosH < -1.0 || cosH > 1.0)
      return null;

    final double h = Math.toDegrees(Math.acos(cosH)) / 15.0;
    final double localMeanTime = h + ra - 0.06571 * t - 6.622;
    final double utcHour = normalize(localMeanTime - lngHour, 24.0);

    final int hour = (int) Math.floor(utcHour);
    final int minute = (int) Math.floor((utcHour - hour) * 60.0);
    final int second = (int) Math.round((((utcHour - hour) * 60.0) - minute) * 60.0);

    return ZonedDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute,
                            Math.min(second, 59), 0, ZoneId.of("UTC"))
        .withZoneSameInstant(zone);
  }

  private static double normalize(double value, double max)
  {
    value %= max;
    return value < 0 ? value + max : value;
  }
}
