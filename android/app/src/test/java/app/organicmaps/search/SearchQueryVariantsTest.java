package app.organicmaps.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class SearchQueryVariantsTest
{
  @Test
  public void expandsPopularAliasesWithoutCoordinates()
  {
    final List<String> variants = SearchQueryVariants.build("БАО");
    assertTrue(variants.contains("Большое Алматинское озеро"));
    assertTrue(variants.contains("Big Almaty Lake"));
    assertTrue(variants.contains("Үлкен Алматы көлі"));
  }

  @Test
  public void stripsAndTranslatesOutdoorType()
  {
    final List<String> variants = SearchQueryVariants.build("пик Фурманова");
    assertTrue(variants.contains("Фурманова"));
    assertTrue(variants.stream().anyMatch(v -> v.equalsIgnoreCase("Furmanova peak")));
  }

  @Test
  public void transliteratesKazakhCyrillic()
  {
    final List<String> variants = SearchQueryVariants.build("Көкжайлау");
    assertTrue(variants.stream().anyMatch(v -> v.toLowerCase().contains("kok")));
  }

  @Test
  public void detectsWhenFallbackIsUseful()
  {
    assertTrue(SearchQueryVariants.hasAlias("Шымбулак"));
    assertTrue(SearchQueryVariants.hasGenericType("озеро Иссык"));
    assertFalse(SearchQueryVariants.hasGenericType("Фурманова"));
  }
}
