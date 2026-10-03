package app.organicmaps.home;

import androidx.annotation.StringRes;
import app.organicmaps.R;
import java.util.List;

/** Curated source figures describe the published hike; the existing router recalculates its planning variant. */
public final class RelatedRoute
{
  public final String id;
  @StringRes
  public final int title;
  public final double sourceDistanceKm;
  @StringRes
  public final int sourceDuration;
  public final int sourceDifficulty;
  public final int sourceAscent;
  public final String sourceUrl;
  @StringRes
  public final int startTitle;
  @StringRes
  public final int finishTitle;
  private final double[][] mWaypoints;

  private RelatedRoute(String id, int title, double distance, int duration, int difficulty, int ascent,
                       double[][] points)
  {
    this.id = id;
    this.title = title;
    sourceDistanceKm = distance;
    sourceDuration = duration;
    sourceDifficulty = difficulty;
    sourceAscent = ascent;
    sourceUrl = "https://zabugorshiki.com/" + HikeRecommendation.sourceSlug(id) + "/";
    mWaypoints = points;
    startTitle = switch (id)
    {
      case "big_almaty_lake" -> R.string.destination_barrier;
      case "butakovsky_waterfall" -> R.string.destination_butakov_gorge;
      case "furmanov_peak", "medeu_shymbulak" -> R.string.destination_medeu;
      case "tourist_peak" -> R.string.destination_cosmostation;
      case "kok_zhailau" -> R.string.destination_prosveshchenets;
      case "tri_bratya" -> R.string.home_kok_zhailau;
      default -> throw new IllegalArgumentException("Unknown route");
    };
    finishTitle = switch (id)
    {
      case "big_almaty_lake" -> R.string.home_bao;
      case "butakovsky_waterfall", "furmanov_peak", "tourist_peak" -> startTitle;
      case "kok_zhailau" -> R.string.home_kok_zhailau;
      case "tri_bratya" -> R.string.home_tri_bratya;
      case "medeu_shymbulak" -> R.string.destination_shymbulak;
      default -> throw new IllegalArgumentException("Unknown route");
    };
  }

  public int pointCount()
  {
    return mWaypoints.length;
  }
  public double latitude(int index)
  {
    return mWaypoints[index][0];
  }
  public double longitude(int index)
  {
    return mWaypoints[index][1];
  }

  static List<RelatedRoute> forDestination(String id)
  {
    return switch (id)
    {
      case "big_almaty_lake" ->
        List.of(new RelatedRoute(id, R.string.route_bao, 7, R.string.route_time_bao, 3, 0, POINTS_BIG_ALMATY_LAKE));
      case "butakovsky_waterfall" ->
        List.of(new RelatedRoute(id, R.string.route_butakovka, 13.5, R.string.route_time_butakovka, 3, 901,
                                 POINTS_BUTAKOVSKY_WATERFALL));
      case "furmanov_peak" ->
        List.of(new RelatedRoute(id, R.string.route_furmanov, 20.5, R.string.route_time_furmanov, 4, 1670,
                                 POINTS_FURMANOV_PEAK));
      case "tourist_peak" ->
        List.of(
            new RelatedRoute(id, R.string.route_tourist, 9, R.string.route_time_tourist, 3, 1059, POINTS_TOURIST_PEAK));
      case "kok_zhailau" ->
        List.of(new RelatedRoute(id, R.string.route_kok, 0, 0, 0, 0,
                                 new double[][] {{43.163047, 77.041140}, {43.139722, 76.997778}}));
      case "tri_bratya" ->
        List.of(new RelatedRoute(id, R.string.route_tri, 0, 0, 0, 0,
                                 new double[][] {{43.139722, 76.997778}, {43.127825, 77.013213}}));
      case "medeu_shymbulak" ->
        List.of(new RelatedRoute(id, R.string.route_shymbulak, 0, 0, 0, 0,
                                 new double[][] {{43.157890, 77.058890}, {43.128270, 77.081420}}));
      default -> List.of();
    };
  }

  private static final double[][] POINTS_BIG_ALMATY_LAKE = {
      {43.088970, 76.960901}, {43.084126, 76.973155}, {43.069965, 76.988448}, {43.059616, 76.986926},
      {43.059972, 76.983755}, {43.056697, 76.988140}, {43.052655, 76.989261}};

  private static final double[][] POINTS_BUTAKOVSKY_WATERFALL = {
      {43.180418, 77.094356}, {43.175992, 77.096374}, {43.172487, 77.108058}, {43.171846, 77.114728},
      {43.172891, 77.114372}, {43.171821, 77.114783}, {43.172528, 77.109142}, {43.171343, 77.107832},
      {43.166349, 77.116619}, {43.155934, 77.121408}, {43.166417, 77.116600}, {43.170541, 77.108407},
      {43.172644, 77.107997}, {43.176067, 77.096387}, {43.180128, 77.094222}};

  private static final double[][] POINTS_FURMANOV_PEAK = {
      {43.162245, 77.053743}, {43.159978, 77.058625}, {43.166211, 77.070059}, {43.162699, 77.076609},
      {43.164885, 77.087792}, {43.162098, 77.087642}, {43.162827, 77.096683}, {43.155638, 77.114008},
      {43.143918, 77.119013}, {43.142945, 77.116381}, {43.144859, 77.119025}, {43.150320, 77.115075},
      {43.155089, 77.114622}, {43.162794, 77.096664}, {43.162102, 77.087623}, {43.164288, 77.089040},
      {43.164841, 77.087394}, {43.163007, 77.074397}, {43.166247, 77.070086}, {43.159892, 77.058349},
      {43.162468, 77.053669}};

  private static final double[][] POINTS_TOURIST_PEAK = {
      {43.040620, 76.945347}, {43.035199, 76.944982}, {43.034902, 76.953244}, {43.028439, 76.952052},
      {43.033506, 76.953363}, {43.035043, 76.944989}, {43.040544, 76.945245}};
}
