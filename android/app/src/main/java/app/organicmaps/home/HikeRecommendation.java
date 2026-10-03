package app.organicmaps.home;

import androidx.annotation.StringRes;
import app.organicmaps.R;
import java.util.List;

/** Offline destination catalog. Missing editorial/review/route metadata is not invented. */
public final class HikeRecommendation
{
  public final String id;
  @StringRes
  public final int title;
  @StringRes
  public final int type;
  public final int altitude;
  @StringRes
  public final int description;
  public final double lat;
  public final double lon;
  public final List<String> imageAssets;
  public final String imageAsset;
  public final boolean photoShowsApproach;
  public final boolean regionalPhoto;

  private HikeRecommendation(String id, int title, int type, int altitude, double lat, double lon,
                             boolean regionalPhoto)
  {
    this.id = id;
    this.title = title;
    this.type = type;
    this.altitude = altitude;
    description = 0;
    this.lat = lat;
    this.lon = lon;
    this.regionalPhoto = regionalPhoto;
    imageAsset = "areamap/hikes/" + (regionalPhoto ? "furmanov_peak" : id) + ".webp";
    imageAssets = List.of(imageAsset);
    photoShowsApproach = id.equals("furmanov_peak");
  }

  public static HikeRecommendation find(String id)
  {
    for (HikeRecommendation item : catalog())
      if (item.id.equals(id))
        return item;
    return null;
  }

  public static List<HikeRecommendation> catalog()
  {
    // Coordinate provenance and representative points: docs/AREAMAP_DESTINATIONS.md.
    return List.of(new HikeRecommendation("tri_bratya", R.string.home_tri_bratya, R.string.destination_peak, 2860,
                                          43.127825, 77.013213, true),
                   new HikeRecommendation("big_almaty_lake", R.string.home_bao, R.string.destination_lake, 2511,
                                          43.05060, 76.98530, false),
                   new HikeRecommendation("butakovsky_waterfall", R.string.home_butakovka,
                                          R.string.destination_waterfall, -1, 43.17231, 77.11401, false),
                   new HikeRecommendation("kok_zhailau", R.string.home_kok_zhailau, R.string.destination_plateau, 2200,
                                          43.139722, 76.997778, false),
                   new HikeRecommendation("furmanov_peak", R.string.home_furmanov, R.string.destination_peak, 3053,
                                          43.149454, 77.116494, false),
                   new HikeRecommendation("medeu_shymbulak", R.string.home_medeu, R.string.destination_resort, -1,
                                          43.12827, 77.08142, false),
                   new HikeRecommendation("tourist_peak", R.string.home_tourist, R.string.destination_peak, 3954,
                                          43.028769, 76.952072, true));
  }
}
