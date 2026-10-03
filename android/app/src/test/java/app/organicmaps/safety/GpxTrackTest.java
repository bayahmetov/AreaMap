package app.organicmaps.safety;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GpxTrackTest
{
  @Test
  public void locatesProgressOnOriginalTrackGeometry()
  {
    final GpxTrack track = new GpxTrack(new double[][] {{0.0, 0.0, 100.0}, {0.0, 0.01, 120.0}});

    final GpxTrack.Position middle = track.locate(0.0, 0.005, -1.0, Double.POSITIVE_INFINITY);
    assertEquals(track.length / 2.0, middle.progress, 5.0);
    assertEquals(0.0, middle.offset, 2.0);
    assertEquals(20.0, track.ascent, 0.01);
  }

  @Test
  public void reportsOffTrackDistance()
  {
    final GpxTrack track = new GpxTrack(new double[][] {{0.0, 0.0, Double.NaN}, {0.0, 0.01, Double.NaN}});

    final GpxTrack.Position position = track.locate(0.001, 0.005, -1.0, Double.POSITIVE_INFINITY);
    assertTrue(position.offset > 100.0);
    assertTrue(position.offset < 120.0);
  }

  @Test
  public void crossesAntimeridianWithoutWorldSizedDistance()
  {
    final GpxTrack track = new GpxTrack(new double[][] {{0.0, 179.9, Double.NaN}, {0.0, -179.9, Double.NaN}});

    assertTrue(track.length > 20000.0);
    assertTrue(track.length < 25000.0);
  }
}
