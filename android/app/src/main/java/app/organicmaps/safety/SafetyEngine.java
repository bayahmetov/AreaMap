package app.organicmaps.safety;

/** Local check-in policy. All times are elapsed realtime milliseconds, never wall-clock time. */
public final class SafetyEngine
{
  public enum State
  {
    NO_LOCATION,
    MOVING,
    RESTING,
    CHECK_IN,
    UNANSWERED
  }

  public static final long FRESH_MS = 120_000;
  private final long mStopMs;
  private final long mReplyMs;
  private long mLastFix = -1;
  private long mAnchorTime = -1;
  private double mAnchorLat;
  private double mAnchorLon;
  private double mAnchorAccuracy;
  private long mRestUntil;
  private long mQuestionAt = -1;

  public SafetyEngine(long stopMs, long replyMs)
  {
    if (stopMs <= 0 || replyMs <= 0)
      throw new IllegalArgumentException("Check-in intervals must be positive");
    mStopMs = stopMs;
    mReplyMs = replyMs;
  }

  public boolean accept(double lat, double lon, double accuracy, long measuredAt, long now)
  {
    if (Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lon) || Double.isInfinite(lon)
        || Double.isNaN(accuracy) || Double.isInfinite(accuracy) || Math.abs(lat) > 90 || Math.abs(lon) > 180
        || accuracy <= 0 || accuracy > 50 || measuredAt < 0 || measuredAt > now || now - measuredAt > FRESH_MS
        || measuredAt <= mLastFix)
      return false;

    final boolean gap = mLastFix < 0 || measuredAt - mLastFix > FRESH_MS;
    mLastFix = measuredAt;
    if (gap || mAnchorTime < 0 || measuredAt < mRestUntil
        || distanceMeters(lat, lon, mAnchorLat, mAnchorLon) > Math.max(50, accuracy + mAnchorAccuracy))
    {
      mAnchorLat = lat;
      mAnchorLon = lon;
      mAnchorAccuracy = accuracy;
      mAnchorTime = measuredAt;
    }
    if (mQuestionAt < 0 && measuredAt >= mRestUntil && measuredAt - mAnchorTime >= mStopMs)
      mQuestionAt = now;
    return true;
  }

  public State state(long now)
  {
    if (mQuestionAt >= 0)
      return now - mQuestionAt >= mReplyMs ? State.UNANSWERED : State.CHECK_IN;
    if (now < mRestUntil)
      return State.RESTING;
    if (mLastFix < 0 || now - mLastFix > FRESH_MS)
      return State.NO_LOCATION;
    return State.MOVING;
  }

  public void acknowledge()
  {
    mQuestionAt = -1;
    mAnchorTime = -1;
    mRestUntil = 0;
  }

  public void rest(long now, long duration)
  {
    acknowledge();
    mRestUntil = now + duration;
  }

  public void locationUnavailable()
  {
    mLastFix = -1;
    mAnchorTime = -1;
  }

  private static double distanceMeters(double lat1, double lon1, double lat2, double lon2)
  {
    final double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                   + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                         * Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
    return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
  }
}
