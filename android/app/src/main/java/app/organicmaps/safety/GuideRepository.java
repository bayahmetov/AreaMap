package app.organicmaps.safety;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One localized catalog. Views select canonical articles; navigation carries only stable IDs. */
public final class GuideRepository
{
  private final List<GuideArticles.Article> mArticles;
  private final Map<String, String> mCategoryLabels;

  public static GuideRepository create(Context context)
  {
    final Map<String, String> labels = new HashMap<>();
    for (GuideCategory category : GuideCategory.all()) labels.put(category.id, context.getString(category.title));
    return new GuideRepository(GuideArticles.catalog(context), labels);
  }

  GuideRepository(List<GuideArticles.Article> articles, Map<String, String> labels)
  {
    final Set<String> ids = new HashSet<>();
    for (GuideArticles.Article article : articles)
      if (!ids.add(article.id)) throw new IllegalArgumentException("Duplicate guide ID: " + article.id);
    mArticles = Collections.unmodifiableList(new ArrayList<>(articles));
    mCategoryLabels = new HashMap<>(labels);
  }

  public List<GuideArticles.Article> all() { return mArticles; }

  public GuideArticles.Article find(String id)
  {
    for (GuideArticles.Article article : mArticles) if (article.id.equals(id)) return article;
    return null;
  }

  public List<GuideArticles.Article> search(String query, String category)
  {
    final List<GuideArticles.Article> result = new ArrayList<>();
    final String q = query.trim().toLowerCase(java.util.Locale.ROOT);
    for (GuideArticles.Article article : mArticles)
      if ((category.isEmpty() || category.equals(article.category))
          && (article.matches(q) || mCategoryLabels.getOrDefault(article.category, "")
              .toLowerCase(java.util.Locale.ROOT).contains(q))) result.add(article);
    return result;
  }

  public List<GuideArticles.Article> featured()
  {
    final List<GuideArticles.Article> result = new ArrayList<>();
    for (GuideArticles.Article article : mArticles) if (article.featured) result.add(article);
    result.sort(Comparator.comparing(a -> a.sourceType.equals("migrated")));
    return result;
  }

  // Matching route tags outrank universal safety topics; tie order is stable across locales.
  public List<GuideArticles.Article> forRoute(RouteGuideContext context)
  {
    final List<GuideArticles.Article> result = new ArrayList<>();
    if (!context.present) return result;
    for (GuideArticles.Article article : mArticles) if (score(article, context) > 0) result.add(article);
    result.sort(Comparator.comparingInt((GuideArticles.Article a) -> score(a, context)).reversed()
        .thenComparing(a -> a.id));
    return result;
  }

  static int score(GuideArticles.Article article, RouteGuideContext context)
  {
    int priority = article.emergency || article.category.equals("weather") || article.category.equals("water") ? 10 : 0;
    for (String tag : article.routeTags) if (context.tags.contains(tag)) priority += 100;
    return priority;
  }
}
