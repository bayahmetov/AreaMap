package app.organicmaps.routing;

import static org.junit.Assert.*;

import org.junit.Test;

public class HikePanelProgressTest
{
  @Test
  public void retainsEnginePercentAndRejectsUnknownProgress()
  {
    assertEquals(0.42, HikePanelProgress.fraction(42), 0.0001);
    assertEquals(0, HikePanelProgress.fraction(-5), 0);
    assertEquals(1, HikePanelProgress.fraction(105), 0);
    assertTrue(Double.isNaN(HikePanelProgress.fraction(Double.NaN)));
  }

  @Test
  public void advancesExistingCheckpointsWithSafetyTolerance()
  {
    final double[] points = {1000, 2000, 3000};
    assertEquals(0, HikePanelProgress.nextCheckpoint(points, 0, true));
    assertEquals(0, HikePanelProgress.nextCheckpoint(points, 949, true));
    assertEquals(1, HikePanelProgress.nextCheckpoint(points, 950, true));
    assertEquals(2, HikePanelProgress.nextCheckpoint(points, 1950, true));
    assertEquals(-1, HikePanelProgress.nextCheckpoint(points, 3000, true));
  }

  @Test
  public void missingFixDoesNotMarkCheckpointsPassed()
  {
    assertEquals(0, HikePanelProgress.nextCheckpoint(new double[] {1000, 2000}, 2000, false));
    assertEquals(0, HikePanelProgress.nextCheckpoint(new double[] {1000}, Double.NaN, true));
    assertEquals(-1, HikePanelProgress.nextCheckpoint(new double[0], 0, true));
  }

  @Test
  public void nextEtaUsesExistingRemainingEtaAndDistance()
  {
    assertEquals(600, HikePanelProgress.checkpointSeconds(2000, 1000, 3000, 1800));
    assertEquals(0, HikePanelProgress.checkpointSeconds(900, 1000, 3000, 1800));
    assertEquals(-1, HikePanelProgress.checkpointSeconds(2000, Double.NaN, 3000, 1800));
    assertEquals(-1, HikePanelProgress.checkpointSeconds(2000, 1000, 0, 0));
  }

  @Test
  public void breaksAdjustSignedScheduleWithoutChangingStart()
  {
    assertEquals(600, HikePanelProgress.scheduleDriftSeconds(3600000, 600000, 4800, 0, 0.5));
    assertEquals(-600, HikePanelProgress.scheduleDriftSeconds(3600000, 600000, 4800, 1200, 0.5));
    assertEquals(Long.MIN_VALUE, HikePanelProgress.scheduleDriftSeconds(3600000, 0, 4800, 0, 0.5));
    assertEquals(Long.MIN_VALUE, HikePanelProgress.scheduleDriftSeconds(3600000, 600000, 4800, 0, Double.NaN));
  }
}
