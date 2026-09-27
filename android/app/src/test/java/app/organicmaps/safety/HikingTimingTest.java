package app.organicmaps.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import app.organicmaps.sdk.util.Distance;
import org.junit.Test;

public class HikingTimingTest
{
  @Test
  public void conservativeEstimateAddsAscentPenalty()
  {
    final int flat = HikingTiming.conservativeSeconds(0, 4500.0, 0.0);
    final int climb = HikingTiming.conservativeSeconds(0, 4500.0, 500.0);
    assertTrue(climb > flat);
    assertEquals(3600, flat);
    assertEquals(6600, climb);
  }

  @Test
  public void engineEstimateIsNeverShortened()
  {
    assertEquals(7200, HikingTiming.conservativeSeconds(7200, 4500.0, 0.0));
  }

  @Test
  public void paceAndScheduleDeltaAreStable()
  {
    assertEquals("20:00", HikingTiming.formatPace(HikingTiming.secondsPerKm(3600, 3000.0)));
    assertEquals(600, HikingTiming.scheduleDeltaSeconds(4200000L, 3000.0, 1200.0));
  }

  @Test
  public void convertsFormattedDistanceUnitsToMeters()
  {
    assertEquals(5000.0, HikingTiming.toMeters(new Distance(5.0, "5", (byte) 1)), 0.01);
    assertEquals(1609.344, HikingTiming.toMeters(new Distance(1.0, "1", (byte) 3)), 0.01);
  }
}
