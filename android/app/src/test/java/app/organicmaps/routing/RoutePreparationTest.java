package app.organicmaps.routing;

import static org.junit.Assert.*;

import app.organicmaps.R;
import app.organicmaps.home.HikeRecommendation;
import java.util.HashSet;
import org.junit.Test;

public class RoutePreparationTest
{
  @Test
  public void shortUnknownWalkGetsCompactHonestFallback()
  {
    final var advice = RoutePreparation.create(1500, 1800, -1, -1, null, null);
    assertEquals(2, advice.packing().size());
    assertTrue(advice.notes().contains(R.string.preparation_check_weather));
    assertFalse(advice.notes().contains(R.string.preparation_early));
    assertNull(HikeRecommendation.preparationAt(43.236621, 76.930356));
  }

  @Test
  public void boukreevUsesCoordinatesAndHasItsOwnSource()
  {
    final var content = HikeRecommendation.preparationAt(43.1669, 77.1344);
    assertNotNull(content);
    assertEquals("https://zabugorshiki.com/boukreev/", content.sourceUrl());
    assertTrue(content.advisories().contains(R.string.preparation_boukreev));
    assertTrue(content.warm());
    assertTrue(content.sun());
    assertNull(HikeRecommendation.preparationAt(43.1769, 77.1344));
    assertNull(HikeRecommendation.preparationAt(Double.NaN, 77.1344));
  }

  @Test
  public void screenshotRouteGetsLongHikeEquipmentWithoutArticleMetrics()
  {
    final var content = HikeRecommendation.preparationAt(43.1669, 77.1344);
    final var advice = RoutePreparation.create(23000, 8 * 3600 + 41 * 60, 2153, 2988, content, null);
    assertTrue(advice.packing().contains(RoutePreparation.Gear.WARM));
    assertTrue(advice.packing().contains(RoutePreparation.Gear.SUN));
    assertTrue(advice.packing().contains(RoutePreparation.Gear.LIGHT_POWER));
    assertTrue(advice.packing().contains(RoutePreparation.Gear.FIRST_AID));
    assertTrue(advice.notes().contains(R.string.preparation_early));
  }

  @Test
  public void elevationAndDurationCanRequireGearEvenForShortDistance()
  {
    assertTrue(RoutePreparation.create(3000, 1800, 800, 1200, null, null).packing()
                   .contains(RoutePreparation.Gear.LIGHT_POWER));
    assertTrue(RoutePreparation.create(3000, 4 * 3600, 200, 1200, null, null).packing()
                   .contains(RoutePreparation.Gear.FOOD));
    assertTrue(RoutePreparation.create(1000, 900, 100, 2500, null, null).packing()
                   .contains(RoutePreparation.Gear.WARM));
  }

  @Test
  public void waterfallDoesNotBorrowBoukreevOrWholeArticleDuration()
  {
    final var content = HikeRecommendation.preparationAt(43.17231, 77.11401);
    final var advice = RoutePreparation.create(2000, 3600, 200, 2159, content, null);
    assertEquals("https://zabugorshiki.com/butakovka/", content.sourceUrl());
    assertTrue(advice.packing().contains(RoutePreparation.Gear.RAIN));
    assertFalse(advice.packing().contains(RoutePreparation.Gear.LIGHT_POWER));
    assertFalse(advice.notes().contains(R.string.preparation_early));
    assertFalse(advice.notes().contains(R.string.preparation_boukreev));
  }

  @Test
  public void curatedApproachPointsMatchButDistantLoopTrailheadsDoNot()
  {
    assertEquals("https://zabugorshiki.com/big-almaty-lake/",
                 HikeRecommendation.preparationAt(43.052655, 76.989261).sourceUrl());
    assertEquals("https://zabugorshiki.com/furmanova-panorama/",
                 HikeRecommendation.preparationAt(43.150320, 77.115075).sourceUrl());
    assertNull(HikeRecommendation.preparationAt(43.162245, 77.053743));
  }

  @Test
  public void forecastAddsGearAndPrioritisesThunderstorms()
  {
    final var advice = RoutePreparation.create(23000, 8 * 3600, 2100, 3000,
        HikeRecommendation.preparationAt(43.1669, 77.1344),
        new RoutePreparation.Weather(5, 95, 3, 40, 60));
    assertTrue(advice.packing().contains(RoutePreparation.Gear.RAIN));
    assertTrue(advice.notes().contains(R.string.preparation_storm));
    assertFalse(advice.notes().contains(R.string.preparation_wind));
    assertEquals(4, advice.notes().size());
    assertEquals(new HashSet<>(advice.packing()).size(), advice.packing().size());
    assertTrue(advice.packing().size() <= 8);
  }

  @Test
  public void coldWindAndHeatAreAdaptedIndependently()
  {
    assertTrue(RoutePreparation.create(1000, 900, 100, 100, null,
        new RoutePreparation.Weather(5, 0, 0, 0, 0)).notes().contains(R.string.preparation_cold));
    assertTrue(RoutePreparation.create(1000, 900, 100, 100, null,
        new RoutePreparation.Weather(20, 0, 0, 35, 0)).packing().contains(RoutePreparation.Gear.WARM));
    assertTrue(RoutePreparation.create(1000, 900, 100, 100, null,
        new RoutePreparation.Weather(30, 0, 0, 0, 0)).packing().contains(RoutePreparation.Gear.SUN));
  }

  @Test
  public void forecastOutsideArrivalWindowCannotDriveAdvice()
  {
    final long arrival = 1700000000000L;
    assertTrue(RoutePreparation.weatherApplies(arrival + 3600000, arrival));
    assertFalse(RoutePreparation.weatherApplies(arrival - 86400000, arrival));
    assertFalse(RoutePreparation.weatherApplies(arrival + 86400000, arrival));
  }
}
