package app.organicmaps.routing;

import app.organicmaps.R;
import app.organicmaps.home.HikeRecommendation.PreparationContent;
import java.util.ArrayList;
import java.util.List;

/** Small presentation policy using native route metrics and the preview's existing forecast. */
final class RoutePreparation
{
  enum Gear
  {
    WATER(R.string.preparation_water, R.drawable.guide_icon_water),
    SHOES(R.string.preparation_shoes, R.drawable.ic_areamap_hike),
    FOOD(R.string.preparation_food, R.drawable.ic_category_food),
    WARM(R.string.preparation_warm, R.drawable.ic_preparation_layer),
    RAIN(R.string.preparation_rain, R.drawable.ic_preview_rain),
    SUN(R.string.preparation_sun, R.drawable.ic_preview_sun),
    LIGHT_POWER(R.string.preparation_light_power, R.drawable.ic_preparation_battery),
    FIRST_AID(R.string.preparation_first_aid, R.drawable.ic_preparation_first_aid);

    final int label;
    final int icon;

    Gear(int label, int icon)
    {
      this.label = label;
      this.icon = icon;
    }
  }

  record Weather(double temperatureC, int code, double precipitationMm, double windKmh, double gustKmh)
  {
  }
  record Advice(List<Gear> packing, List<Integer> notes)
  {
  }

  static Advice create(double distanceMeters, int seconds, int ascent, int maxAltitude,
                       PreparationContent content, Weather weather)
  {
    final boolean longRoute = distanceMeters >= 10000 || seconds >= 4 * 3600 || ascent >= 800;
    final boolean high = maxAltitude >= 2500;
    final boolean cold = weather != null && weather.temperatureC() <= 10;
    final boolean windy = weather != null && (weather.windKmh() >= 30 || weather.gustKmh() >= 50);
    final boolean wet = weather != null && (weather.precipitationMm() > 0 || weather.code() >= 51);
    final boolean hot = weather != null && weather.temperatureC() >= 25;
    final List<Gear> packing = new ArrayList<>(List.of(Gear.WATER, Gear.SHOES));
    if (distanceMeters >= 5000 || seconds >= 2 * 3600 || longRoute)
      packing.add(Gear.FOOD);
    if (high || cold || windy || content != null && content.warm())
      packing.add(Gear.WARM);
    if (wet || content != null && content.waterproof())
      packing.add(Gear.RAIN);
    if (high || hot || content != null && content.sun())
      packing.add(Gear.SUN);
    if (longRoute)
    {
      packing.add(Gear.LIGHT_POWER);
      packing.add(Gear.FIRST_AID);
    }
    final List<Integer> notes = new ArrayList<>();
    if (content != null)
      notes.addAll(content.advisories());
    if (longRoute)
      notes.add(R.string.preparation_early);
    // One weather note, with priority for conditions that most affect departure.
    if (weather != null && weather.code() >= 95)
      notes.add(R.string.preparation_storm);
    else if (wet)
      notes.add(R.string.preparation_wet);
    else if (windy)
      notes.add(R.string.preparation_wind);
    else if (cold)
      notes.add(R.string.preparation_cold);
    else if (hot)
      notes.add(R.string.preparation_heat);
    else if (content == null && !longRoute)
      notes.add(R.string.preparation_check_weather);
    notes.add(R.string.preparation_return);
    return new Advice(List.copyOf(packing), List.copyOf(notes));
  }

  /** Reject forecasts outside this route's arrival hour; an old cache is not today's weather. */
  static boolean weatherApplies(long hourMillis, long arrivalMillis)
  {
    return Math.abs((double) hourMillis - arrivalMillis) <= 90 * 60 * 1000;
  }
}
