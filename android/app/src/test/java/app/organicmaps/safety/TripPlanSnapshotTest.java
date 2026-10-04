package app.organicmaps.safety;

import static org.junit.Assert.*;

import app.organicmaps.sdk.routing.RouteAltitudeData;
import java.util.List;
import org.junit.Test;

public class TripPlanSnapshotTest
{
  @Test
  public void retainsThePreviewPointsAndProfileAcrossRestart()
  {
    final RouteAltitudeData profile =
        new RouteAltitudeData(new double[] {0, 1000, 2000}, new int[] {1000, 1400, 1200}, 400, 200, 1000, 1400);
    final TripPlan original = new TripPlan("Trailhead", "Finish", 43.0, 77.0, 43.1, 77.1, 2000, 3600, 4200,
                                           List.of(new TripPlan.Checkpoint(1000, 1400, 1800, 43.05, 77.05, "Pass", 0)),
                                           profile, 43.05, 77.05, 1400, 1800, true);
    final TripPlan restored = TripPlan.fromJson(original.toJson());
    assertNotNull(restored);
    assertEquals(original.finishTitle, restored.finishTitle);
    assertEquals(original.distanceMeters, restored.distanceMeters, 0);
    assertEquals(original.plannedSeconds, restored.plannedSeconds);
    assertEquals(original.returnSeconds, restored.returnSeconds);
    assertEquals(original.routeCheckpoints.size(), restored.routeCheckpoints.size());
    for (int i = 0; i < original.routeCheckpoints.size(); i++)
    {
      final TripPlan.Checkpoint a = original.routeCheckpoints.get(i), b = restored.routeCheckpoints.get(i);
      assertEquals(a.title, b.title);
      assertEquals(a.role, b.role);
      assertEquals(a.distanceMeters, b.distanceMeters, 0);
      assertEquals(a.etaSeconds, b.etaSeconds);
      assertEquals(a.altitudeMeters, b.altitudeMeters);
      assertEquals(a.lat, b.lat, 0);
      assertEquals(a.lon, b.lon, 0);
    }
    assertNotNull(restored.altitude);
    assertEquals(3, restored.altitude.getSize());
    assertEquals(1400, restored.altitude.getAltitude(1));
    assertEquals(400, restored.altitude.getTotalAscent());
    assertTrue(restored.weatherAtHighestPoint);
    assertEquals(original.weatherLat, restored.weatherLat, 0);
  }

  @Test
  public void missingProfileAndInvalidSnapshotDoNotInventData()
  {
    final TripPlan plan =
        new TripPlan("", "", 43, 77, 43.1, 77.1, 2000, 3600, 3600, List.of(), null, 43.1, 77.1, -1, 3600, false);
    final TripPlan restored = TripPlan.fromJson(plan.toJson());
    assertNotNull(restored);
    assertNull(restored.altitude);
    assertTrue(restored.checkpoints.isEmpty());
    assertEquals(-1, restored.routeCheckpoints.get(0).altitudeMeters);
    assertNull(TripPlan.fromJson(""));
    assertNull(TripPlan.fromJson("{}"));
  }
}
