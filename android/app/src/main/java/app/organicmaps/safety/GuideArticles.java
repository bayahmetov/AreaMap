package app.organicmaps.safety;

import android.content.Context;
import androidx.annotation.NonNull;
import app.organicmaps.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class GuideArticles
{
  public static final class Article
  {
    public final String title;
    public final String tags;
    public final String body;
    public final String id;
    public final String category;
    public final String imageAsset;
    public final int readingTime;
    public final String[] routeTags;
    public final boolean featured;
    public final boolean emergency;
    public final String sourceType;
    // No real review dataset exists. UI explicitly marks its rating badge as demo.
    public final double rating = 4.8;

    Article(String id, String title, String tags, String body)
    {
      this.title = title;
      this.tags = tags;
      this.body = body;
      this.id = id;
      this.category = id.equals("legacy_8") ? "weather" : id.equals("legacy_7") ? "lost" : "survival";
      this.imageAsset = "areamap/guides/guide_" + (category.equals("weather") ? "weather_storm"
          : category.equals("lost") ? "no_signal" : "hero_almaty") + ".webp";
      this.readingTime = Math.max(1, (body.length() + 899) / 900);
      this.routeTags = legacyTags(id);
      this.featured = id.equals("legacy_7") || id.equals("legacy_3");
      this.emergency = true;
      this.sourceType = "migrated";
    }

    Article(String id, String category, String image, String title, String body)
    {
      this.id = id; this.category = category; this.title = title; this.body = body;
      this.tags = category; this.imageAsset = "areamap/guides/guide_" + image + ".webp";
      this.readingTime = Math.max(1, (body.length() + 899) / 900); this.routeTags = new String[] {"hiking"};
      this.featured = id.equals("route") || id.equals("weather");
      this.emergency = id.equals("signal") || id.equals("plants");
      this.sourceType = "editorial";
    }

    public boolean matches(@NonNull String query)
    {
      final String q = query.trim().toLowerCase(Locale.ROOT);
      if (q.isEmpty())
        return true;
      return (title + " " + tags + " " + java.util.Arrays.toString(routeTags) + " " + body).toLowerCase(Locale.ROOT).contains(q);
    }
  }

  private GuideArticles() {}

  static List<Article> catalog(Context context)
  {
    final List<Article> result = new ArrayList<>();
    result.add(new Article("legacy_0", context.getString(R.string.g_legacy_0_title),
        context.getString(R.string.g_legacy_0_tags), context.getString(R.string.g_legacy_0_body)));
    result.add(new Article("legacy_1", context.getString(R.string.g_legacy_1_title),
        context.getString(R.string.g_legacy_1_tags), context.getString(R.string.g_legacy_1_body)));
    result.add(new Article("legacy_2", context.getString(R.string.g_legacy_2_title),
        context.getString(R.string.g_legacy_2_tags), context.getString(R.string.g_legacy_2_body)));
    result.add(new Article("legacy_3", context.getString(R.string.g_legacy_3_title),
        context.getString(R.string.g_legacy_3_tags), context.getString(R.string.g_legacy_3_body)));
    result.add(new Article("legacy_4", context.getString(R.string.g_legacy_4_title),
        context.getString(R.string.g_legacy_4_tags), context.getString(R.string.g_legacy_4_body)));
    result.add(new Article("legacy_5", context.getString(R.string.g_legacy_5_title),
        context.getString(R.string.g_legacy_5_tags), context.getString(R.string.g_legacy_5_body)));
    result.add(new Article("legacy_6", context.getString(R.string.g_legacy_6_title),
        context.getString(R.string.g_legacy_6_tags), context.getString(R.string.g_legacy_6_body)));
    result.add(new Article("legacy_7", context.getString(R.string.g_legacy_7_title),
        context.getString(R.string.g_legacy_7_tags), context.getString(R.string.g_legacy_7_body)));
    result.add(new Article("legacy_8", context.getString(R.string.g_legacy_8_title),
        context.getString(R.string.g_legacy_8_tags), context.getString(R.string.g_legacy_8_body)));
    addNew(context, result);
    return result;
  }

  private static void addNew(Context context, List<Article> result)
  {
    final String[] ids = {"route", "weather", "gear", "signal", "water", "wildlife", "plants"};
    final String[] categories = {"route", "weather", "weather", "lost", "water", "wildlife", "plants"};
    final String[] images = {"kok_zhailau", "weather_storm", "hero_almaty", "no_signal",
                            "butakovsky_waterfall", "wildlife_bear", "plants"};
    final int[] titles = {R.string.g_a_route, R.string.g_a_weather, R.string.g_a_gear, R.string.g_a_signal,
                          R.string.g_a_water, R.string.g_a_wildlife, R.string.g_a_plants};
    final int[] bodies = {R.string.g_b_route, R.string.g_b_weather, R.string.g_b_gear, R.string.g_b_signal,
                          R.string.g_b_water, R.string.g_b_wildlife, R.string.g_b_plants};
    for (int i = 0; i < ids.length; i++)
      result.add(new Article(ids[i], categories[i], images[i], context.getString(titles[i]),
                             context.getString(bodies[i])));
  }

  private static String[] legacyTags(String id)
  {
    if (id.equals("legacy_3")) return new String[] {"hiking", "cold"};
    if (id.equals("legacy_4")) return new String[] {"hiking", "hot"};
    if (id.equals("legacy_6")) return new String[] {"high_altitude"};
    if (id.equals("legacy_7")) return new String[] {"hiking", "offline", "gpx"};
    return new String[] {"hiking"};
  }
}
