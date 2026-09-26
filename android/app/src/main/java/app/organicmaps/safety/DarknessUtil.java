package app.organicmaps.safety;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Offline sunrise/sunset approximation used only for an early safety warning.
 * It intentionally warns when any planned part of the hike overlaps darkness.
 */
public final class DarknessUtil
{
  private DarknessUtil() {}

  public static boolean routeTouchesDarkness(long nowEpochMs, int etaSeconds, double lat, double lon)
  {
    final ZoneId zone = ZoneId.systemDefault();
    final ZonedDateTime start = Instant.ofEpochMilli(nowEpochMs).atZone(zone);
    final ZonedDateTime finish = start.plusSeconds(Math.max(0, etaSeconds));

    if (!finish.toLocalDate().equals(start.toLocalDate()))
      return true;

    final ZonedDateTime sunrise = solarEvent(start.toLocalDate(), lat, lon, zone, true);
    final ZonedDateTime sunset = solarEvent(start.toLocalDate(), lat, lon, zone, false);
    if (sunrise == null || sunset == null)
      return false;

    return start.isBefore(sunrise) || !start.isBefore(sunset) || finish.isAfter(sunset);
  }

  @Nullable
  private static ZonedDateTime solarEvent(@NonNull LocalDate date, double latitude, double longitude,
                                          @NonNull ZoneId zone, boolean sunrise)
  {
    // NOAA-style approximation.
    final int day = date.getDayOfYear();
    final double lngHour = longitude / 15.0;
    final double t = day + (((sunrise ? 6.0 : 18.0) - lngHour) / 24.0);
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

    final double hDegrees = sunrise ? 360.0 - Math.toDegrees(Math.acos(cosH))
                                    : Math.toDegrees(Math.acos(cosH));
    final double h = hDegrees / 15.0;
    final double localMeanTime = h + ra - 0.06571 * t - 6.622;
    final double utcHour = normalize(localMeanTime - lngHour, 24.0);

    int hour = (int) Math.floor(utcHour);
    int minute = (int) Math.floor((utcHour - hour) * 60.0);
    int second = (int) Math.round((((utcHour - hour) * 60.0) - minute) * 60.0);
    if (second == 60)
    {
      second = 0;
      minute++;
    }
    if (minute == 60)
    {
      minute = 0;
      hour = (hour + 1) % 24;
    }

    return ZonedDateTime.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth(), hour, minute, second, 0,
                            ZoneId.of("UTC"))
        .withZoneSameInstant(zone);
  }

  private static double normalize(double value, double max)
  {
    value %= max;
    return value < 0 ? value + max : value;
  }
}
