package app.organicmaps.routing;

import static org.junit.Assert.*;

import org.junit.Test;

public class RoutePreviewViewportTest
{
  @Test
  public void fitsWholeGeometryIncludingDetour()
  {
    final RoutePreviewViewport viewport =
        RoutePreviewViewport.fit(new double[][] {{43, 77}, {44, 78}, {43.1, 77.1}}, 400, 300);
    assertEquals(77.5, viewport.lon, 0.001);
    assertTrue(viewport.lat > 43.4 && viewport.lat < 43.6);
    assertTrue(viewport.zoom < 12);
  }

  @Test
  public void smallerMapAreaZoomsOut()
  {
    final double[][] points = {{43, 77}, {43.3, 77.2}};
    assertTrue(RoutePreviewViewport.fit(points, 400, 120).zoom < RoutePreviewViewport.fit(points, 400, 480).zoom);
  }

  @Test
  public void crossesDateLineWithoutFittingWholeWorld()
  {
    final RoutePreviewViewport viewport = RoutePreviewViewport.fit(new double[][] {{0, 179.9}, {0, -179.9}}, 400, 300);
    assertEquals(180, Math.abs(viewport.lon), 0.001);
    assertTrue(viewport.zoom >= 10);
  }

  @Test
  public void singlePointAndPolarLatitudeAreFinite()
  {
    final RoutePreviewViewport viewport = RoutePreviewViewport.fit(new double[][] {{90, 10}}, 1, 1);
    assertTrue(Double.isFinite(viewport.lat));
    assertEquals(10, viewport.lon, 0.001);
    assertEquals(18, viewport.zoom);
  }
}
