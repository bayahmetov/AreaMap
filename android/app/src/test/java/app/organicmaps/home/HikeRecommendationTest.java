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
  public void unphotographedPeaksUseArtworkAndEveryPlaceHasEditorialProvenance()
  {
    assertTrue(HikeRecommendation.find("tri_bratya").imageAssets.isEmpty());
    assertTrue(HikeRecommendation.find("tourist_peak").imageAssets.isEmpty());
    assertFalse(HikeRecommendation.find("big_almaty_lake").imageAssets.isEmpty());
    for (HikeRecommendation item : HikeRecommendation.catalog())
    {
      assertNotEquals(0, item.description);
      assertEquals("Zabugorshiki", item.sourceName);
      assertTrue(item.sourceUrl.startsWith("https://zabugorshiki.com/"));
    }
  }

  @Test
  public void unknownVariantMetricsDoNotBorrowAnotherHikesFigures()
  {
    for (String id : new String[] {"tri_bratya", "kok_zhailau", "medeu_shymbulak"})
    {
      final RelatedRoute route = HikeRecommendation.find(id).relatedRoutes.get(0);
      assertEquals(0, route.sourceDistanceKm, 0);
      assertEquals(0, route.sourceDuration);
      assertEquals(0, route.sourceDifficulty);
      assertEquals(0, route.sourceAscent);
    }
  }

  @Test
  public void curatedWaypointsFitNativeCapacityAndRetainPublishedRouteScope()
  {
    for (HikeRecommendation item : HikeRecommendation.catalog())
      for (RelatedRoute route : item.relatedRoutes)
      {
        assertTrue(route.pointCount() >= 2 && route.pointCount() <= 102);
        for (int i = 0; i < route.pointCount(); i++)
        {
          assertTrue(Double.isFinite(route.latitude(i)) && route.latitude(i) > 42 && route.latitude(i) < 44);
          assertTrue(Double.isFinite(route.longitude(i)) && route.longitude(i) > 76 && route.longitude(i) < 78);
        }
      }
    final RelatedRoute waterfall = HikeRecommendation.find("butakovsky_waterfall").relatedRoutes.get(0);
    assertEquals(13.5, waterfall.sourceDistanceKm, 0);
    assertEquals(901, waterfall.sourceAscent);
    assertEquals(2159, HikeRecommendation.find("butakovsky_waterfall").altitude);
    assertEquals(0, HikeRecommendation.find("big_almaty_lake").relatedRoutes.get(0).sourceAscent);
  }
}
