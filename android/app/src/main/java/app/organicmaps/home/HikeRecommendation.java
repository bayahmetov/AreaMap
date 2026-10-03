package app.organicmaps.home;

import androidx.annotation.StringRes;
import app.organicmaps.R;
import java.util.List;

/** Demo catalog: ratings and walking times are illustrative, not live safety assessments. */
public final class HikeRecommendation
{
  public final String id;
  @StringRes public final int title;
  public final int altitude;
  public final String hours;
  @StringRes public final int difficulty;
  public final double rating;
  public final String imageAsset;

  private HikeRecommendation(String id, int title, int altitude, String hours, int difficulty, double rating)
  {
    this.id = id;
    this.title = title;
    this.altitude = altitude;
    this.hours = hours;
    this.difficulty = difficulty;
    this.rating = rating;
    imageAsset = "areamap/hikes/" + id + ".webp";
  }

  public static List<HikeRecommendation> demoCatalog()
  {
    return List.of(
        new HikeRecommendation("tri_bratya", R.string.home_tri_bratya, 2860, "6–8", R.string.home_medium, 4.8),
        new HikeRecommendation("big_almaty_lake", R.string.home_bao, 2511, "3–5", R.string.home_easy, 4.9),
        new HikeRecommendation("butakovsky_waterfall", R.string.home_butakovka, 1400, "2–3", R.string.home_easy, 4.7),
        new HikeRecommendation("kok_zhailau", R.string.home_kok_zhailau, 2250, "4–6", R.string.home_medium, 4.8),
        new HikeRecommendation("furmanov_peak", R.string.home_furmanov, 3053, "6–8", R.string.home_medium, 4.8),
        new HikeRecommendation("medeu_shymbulak", R.string.home_medeu, 2260, "2–4", R.string.home_easy, 4.7),
        new HikeRecommendation("tourist_peak", R.string.home_tourist, 3954, "8–10", R.string.home_medium, 4.9));
  }
}
