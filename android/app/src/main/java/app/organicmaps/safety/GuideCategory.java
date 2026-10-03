package app.organicmaps.safety;

import app.organicmaps.R;
import java.util.List;

/** Category counts come from the actual offline catalog, never from the visual reference. */
public final class GuideCategory
{
  public final String id;
  public final int title;
  public final String image;
  public final int icon;
  public final int accent;

  private GuideCategory(String id, int title, String image, int icon, int accent)
  {
    this.id = id; this.title = title; this.image = image; this.icon = icon; this.accent = accent;
  }

  public static List<GuideCategory> all()
  {
    return List.of(
        new GuideCategory("route", R.string.g_cat_route, "route_planning", R.drawable.ic_manage_route, R.color.guide_accent_route),
        new GuideCategory("weather", R.string.g_cat_weather, "weather_storm", R.drawable.guide_icon_weather, R.color.guide_accent_weather),
        new GuideCategory("survival", R.string.g_cat_survival, "survival_fire", R.drawable.guide_icon_fire, R.color.guide_accent_survival),
        new GuideCategory("wildlife", R.string.g_cat_wildlife, "wildlife_bear", R.drawable.guide_icon_paw, R.color.guide_accent_wildlife),
        new GuideCategory("plants", R.string.g_cat_plants, "plants", R.drawable.guide_icon_leaf, R.color.guide_accent_plants),
        new GuideCategory("water", R.string.g_cat_water, "water_source", R.drawable.guide_icon_water, R.color.guide_accent_water),
        new GuideCategory("lost", R.string.g_cat_lost, "no_signal", R.drawable.guide_icon_compass, R.color.guide_accent_lost));
  }

  public static GuideCategory find(String id)
  {
    for (GuideCategory category : all())
      if (category.id.equals(id)) return category;
    return all().get(0);
  }
}
