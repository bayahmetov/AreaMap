package app.organicmaps.safety;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import app.organicmaps.sdk.routing.RouteAltitudeData;
import org.junit.Test;

public class RouteSafetyAnalysisTest
{
  @Test
  public void findsMultipleSummitsAndSteepSections()
  {
    final double[] d = {0, 250, 500, 750, 1000, 1300, 1600, 1900, 2200, 2500, 2800, 3100};
    final int[] h = {1000, 1040, 1120, 1190, 1100, 1160, 1260, 1340, 1240, 1280, 1390, 1300};
    final RouteAltitudeData data = new RouteAltitudeData(d, h, 560, 260, 1000, 1390);

    final RouteSafetyAnalysis.Result result = RouteSafetyAnalysis.analyze(data, 4 * 3600);

    assertTrue(result.peaks.size() >= 2);
    assertFalse(result.hazards.isEmpty());
    assertTrue(result.peaks.get(0).etaSeconds < result.peaks.get(result.peaks.size() - 1).etaSeconds);
  }

  @Test
  public void flatRouteHasNoArtificialHazards()
  {
    final double[] d = {0, 500, 1000, 1500};
    final int[] h = {1000, 1002, 1001, 1003};
    final RouteAltitudeData data = new RouteAltitudeData(d, h, 3, 1, 1000, 1003);

    final RouteSafetyAnalysis.Result result = RouteSafetyAnalysis.analyze(data, 1200);

    assertTrue(result.hazards.isEmpty());
  }
}
