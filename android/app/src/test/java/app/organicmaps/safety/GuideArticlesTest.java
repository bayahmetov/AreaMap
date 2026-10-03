package app.organicmaps.safety;

import static org.junit.Assert.*;
import java.util.List;
import java.util.Locale;
import org.junit.Test;

public class GuideArticlesTest
{
  @Test public void legacyDestinationsAndFavoritesKeepTheirIdsAcrossLocales()
  {
    final Locale previous = Locale.getDefault();
    try
    {
      Locale.setDefault(Locale.ENGLISH);
      final List<GuideArticles.Article> english = GuideArticles.all();
      Locale.setDefault(new Locale("ru"));
      final List<GuideArticles.Article> russian = GuideArticles.all();
      assertEquals(9, english.size());
      assertEquals(english.size(), russian.size());
      for (int i = 0; i < english.size(); i++)
      {
        assertEquals(english.get(i).id, russian.get(i).id);
        assertEquals(english.get(i).category, russian.get(i).category);
        assertEquals(english.get(i).imageAsset, russian.get(i).imageAsset);
      }
    }
    finally { Locale.setDefault(previous); }
  }

  @Test public void searchFindsTitleBodyAndTagsWithWhitespaceAndCase()
  {
    final GuideArticles.Article article = new GuideArticles.Article("route", "Маршрут", "trail",
                                                                    "Проверьте карту перед выходом.");
    assertTrue(article.matches("  МАРШРУТ  "));
    assertTrue(article.matches("карту"));
    assertTrue(article.matches("TRAIL"));
    assertTrue(article.matches(" "));
    assertFalse(article.matches("unrelated"));
  }
}
