package app.organicmaps.home;

import static org.junit.Assert.*;

import org.junit.Test;

public class DestinationRouteMatchTest
{
  @Test
  public void matchesSameCoordinateAndNearbyRoutePoint()
  {
    assertTrue(DestinationRouteMatch.near(43.127825, 77.013213, 43.127825, 77.013213));
    assertTrue(DestinationRouteMatch.near(43.127825, 77.013213, 43.128, 77.0135));
  }

  @Test
  public void rejectsDifferentPlace()
  {
    assertFalse(DestinationRouteMatch.near(43.127825, 77.013213, 43.139722, 76.997778));
  }

  @Test
  public void rejectsMissingCoordinates()
  {
    assertFalse(DestinationRouteMatch.near(43, 77, Double.NaN, 77));
    assertFalse(DestinationRouteMatch.near(Double.POSITIVE_INFINITY, 77, 43, 77));
  }

  @Test
  public void usesMetersAtDifferentLatitudes()
  {
    assertFalse(DestinationRouteMatch.near(0, 0, 0, 0.001));
    assertTrue(DestinationRouteMatch.near(60, 0, 60, 0.001));
  }
}
