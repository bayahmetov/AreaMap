package app.organicmaps.home;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class HikeRecommendationTest
{
  @Test
  public void stableIdsResolveAcrossScreenRestoration()
  {
    final Set<String> ids = new HashSet<>();
    for (HikeRecommendation item : HikeRecommendation.catalog())
    {
      assertTrue(ids.add(item.id));
      final HikeRecommendation restored = HikeRecommendation.find(item.id);
      assertNotNull(restored);
      assertEquals(item.lat, restored.lat, 0);
      assertEquals(item.lon, restored.lon, 0);
      assertEquals(item.imageAssets, restored.imageAssets);
    }
    assertNull(HikeRecommendation.find("missing_destination"));
  }

  @Test
  public void everyRoutingTargetHasRealRegionalCoordinates()
  {
    for (HikeRecommendation item : HikeRecommendation.catalog())
    {
      assertTrue(Double.isFinite(item.lat) && item.lat > 42 && item.lat < 44);
      assertTrue(Double.isFinite(item.lon) && item.lon > 76 && item.lon < 78);
    }
  }

  @Test
  public void fallbackPhotosAreDisclosedAndMissingHeightIsUnknown()
  {
    assertTrue(HikeRecommendation.find("tri_bratya").regionalPhoto);
    assertTrue(HikeRecommendation.find("tourist_peak").regionalPhoto);
    assertFalse(HikeRecommendation.find("big_almaty_lake").regionalPhoto);
    assertEquals(-1, HikeRecommendation.find("butakovsky_waterfall").altitude);
  }
}
