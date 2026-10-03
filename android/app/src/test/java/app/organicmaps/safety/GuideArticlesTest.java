package app.organicmaps.safety;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class GuideArticlesTest
{
  private final GuideArticles.Article route = new GuideArticles.Article(
      "route", "route", "route_planning", "План маршрута", "Check your compass before leaving.");
  private final GuideArticles.Article altitude = new GuideArticles.Article(
      "legacy_6", "Высотная болезнь", "altitude", "Descend when symptoms worsen.");
  private final GuideArticles.Article lost = new GuideArticles.Article(
      "legacy_7", "Если потерялись", "navigation", "Stop and check the track.");
  private final GuideRepository repository = new GuideRepository(List.of(route, altitude, lost),
      Map.of("route", "Подготовка маршрута", "survival", "Выживание в горах", "lost", "Нет связи"));

  @Test public void searchFindsLocalizedCategoryTitleTagsAndBody()
  {
    assertEquals(route, repository.search("  ПЛАН  ", "").get(0));
    assertEquals(route, repository.search("compass", "").get(0));
    assertEquals(route, repository.search("подготовка", "").get(0));
    assertEquals(lost, repository.search("NAVIGATION", "").get(0));
    assertTrue(repository.search("nothing like this", "").isEmpty());
  }

  @Test public void categoryFilterAndUnknownIdAreSafe()
  {
    assertEquals(List.of(route), repository.search("", "route"));
    assertTrue(repository.search("compass", "lost").isEmpty());
    assertNull(repository.find("missing"));
    assertNull(repository.find(null));
    assertEquals(lost, repository.find("legacy_7"));
  }

  @Test public void emptyRouteHasNoRecommendationsAndTagsRankRelevantArticles()
  {
    assertTrue(repository.forRoute(new RouteGuideContext(false, "none", Set.of())).isEmpty());
    final RouteGuideContext high = new RouteGuideContext(true, "gpx", Set.of("high_altitude"));
    assertEquals(altitude, repository.forRoute(high).get(0));
    final RouteGuideContext offline = new RouteGuideContext(true, "gpx", Set.of("gpx", "offline"));
    assertEquals(lost, repository.forRoute(offline).get(0));
  }

  @Test public void featuredArticlesAreCanonicalAndDuplicateIdsAreRejected()
  {
    assertTrue(repository.featured().contains(route));
    assertTrue(repository.featured().contains(lost));
    assertFalse(repository.featured().contains(altitude));
    try { new GuideRepository(List.of(route, route), Map.of()); fail("Duplicate IDs accepted"); }
    catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("route")); }
  }

  @Test public void regionTagDoesNotInventSpecificPeakIdentity()
  {
    final java.util.HashSet<String> tags = new java.util.HashSet<>();
    RouteGuideContext.addLocation(tags, 43.05, 76.98);
    assertEquals(Set.of("almaty"), tags);
    tags.clear();
    RouteGuideContext.addLocation(tags, 51.17, -115.57);
    assertTrue(tags.isEmpty());
  }
}
