package app.organicmaps.safety;

import androidx.annotation.NonNull;
import app.organicmaps.sdk.routing.RouteAltitudeData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RouteSafetyAnalysis
{
  public static final class Peak
  {
    public final double distanceMeters;
    public final int altitudeMeters;
    public final int etaSeconds;

    Peak(double distanceMeters, int altitudeMeters, int etaSeconds)
    {
      this.distanceMeters = distanceMeters;
      this.altitudeMeters = altitudeMeters;
      this.etaSeconds = etaSeconds;
    }
  }

  public static final class Checkpoint
  {
    public final double distanceMeters;
    public final int altitudeMeters;
    public final int etaSeconds;

    Checkpoint(double distanceMeters, int altitudeMeters, int etaSeconds)
    {
      this.distanceMeters = distanceMeters;
      this.altitudeMeters = altitudeMeters;
      this.etaSeconds = etaSeconds;
    }
  }

  public static final class Hazard
  {
    public final double distanceMeters;
    public final double gradePercent;
    public final boolean descent;

    Hazard(double distanceMeters, double gradePercent, boolean descent)
    {
      this.distanceMeters = distanceMeters;
      this.gradePercent = gradePercent;
      this.descent = descent;
    }
  }

  public static final class Result
  {
    public final int maxAltitude;
    public final int minAltitude;
    public final int totalAscent;
    public final int totalDescent;
    public final double maxGradePercent;
    @NonNull public final List<Peak> peaks;
    @NonNull public final List<Checkpoint> checkpoints;
    @NonNull public final List<Hazard> hazards;

    Result(int maxAltitude, int minAltitude, int totalAscent, int totalDescent, double maxGradePercent,
           @NonNull List<Peak> peaks, @NonNull List<Checkpoint> checkpoints, @NonNull List<Hazard> hazards)
    {
      this.maxAltitude = maxAltitude;
      this.minAltitude = minAltitude;
      this.totalAscent = totalAscent;
      this.totalDescent = totalDescent;
      this.maxGradePercent = maxGradePercent;
      this.peaks = peaks;
      this.checkpoints = checkpoints;
      this.hazards = hazards;
    }
  }

  private static final double FLAT_SPEED_MPS = 4.5 / 3.6;
  private static final double ASCENT_SECONDS_PER_METER = 6.0;

  private RouteSafetyAnalysis() {}

  @NonNull
  public static Result analyze(@NonNull RouteAltitudeData data, int plannedSeconds)
  {
    final int size = data.getSize();
    if (size < 2)
      return new Result(data.getMaxAltitude(), data.getMinAltitude(), data.getTotalAscent(), data.getTotalDescent(),
                        0.0, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

    final double[] effort = new double[size];
    for (int i = 1; i < size; i++)
    {
      final double dx = Math.max(0.0, data.getDistance(i) - data.getDistance(i - 1));
      final int dz = data.getAltitude(i) - data.getAltitude(i - 1);
      effort[i] = effort[i - 1] + dx / FLAT_SPEED_MPS + Math.max(0, dz) * ASCENT_SECONDS_PER_METER;
    }

    final List<Hazard> hazards = new ArrayList<>();
    double maxGrade = 0.0;
    for (int i = 0; i < size - 1; i++)
    {
      int j = i + 1;
      while (j < size && data.getDistance(j) - data.getDistance(i) < 100.0)
        j++;
      if (j >= size)
        break;

      final double dx = data.getDistance(j) - data.getDistance(i);
      final double grade = (data.getAltitude(j) - data.getAltitude(i)) / dx * 100.0;
      maxGrade = Math.max(maxGrade, Math.abs(grade));

      if (Math.abs(grade) >= 15.0)
      {
        final double at = (data.getDistance(i) + data.getDistance(j)) / 2.0;
        if (hazards.isEmpty() || at - hazards.get(hazards.size() - 1).distanceMeters >= 500.0)
        {
          hazards.add(new Hazard(at, Math.abs(grade), grade < 0));
          if (hazards.size() == 5)
            break;
        }
      }
    }

    final double totalEffort = Math.max(1.0, effort[size - 1]);
    final List<Integer> candidates = new ArrayList<>();
    for (int i = 1; i < size - 1; i++)
    {
      if (data.getAltitude(i) < data.getAltitude(i - 1) || data.getAltitude(i) < data.getAltitude(i + 1))
        continue;

      final double center = data.getDistance(i);
      int leftMin = data.getAltitude(i);
      int rightMin = data.getAltitude(i);
      for (int j = i; j >= 0 && center - data.getDistance(j) <= 500.0; j--)
        leftMin = Math.min(leftMin, data.getAltitude(j));
      for (int j = i; j < size && data.getDistance(j) - center <= 500.0; j++)
        rightMin = Math.min(rightMin, data.getAltitude(j));

      final int prominence = data.getAltitude(i) - Math.max(leftMin, rightMin);
      if (prominence >= 40)
        candidates.add(i);
    }

    final List<Integer> dedup = new ArrayList<>();
    for (int idx : candidates)
    {
      if (dedup.isEmpty())
      {
        dedup.add(idx);
        continue;
      }

      final int prev = dedup.get(dedup.size() - 1);
      if (data.getDistance(idx) - data.getDistance(prev) < 500.0)
      {
        if (data.getAltitude(idx) > data.getAltitude(prev))
          dedup.set(dedup.size() - 1, idx);
      }
      else
        dedup.add(idx);
    }

    final List<Checkpoint> checkpoints = new ArrayList<>();
    final int checkpointCount = Math.min(5, Math.max(1, plannedSeconds / (75 * 60)));
    for (int number = 1; number <= checkpointCount; number++)
    {
      final double targetEffort = totalEffort * number / (checkpointCount + 1.0);
      int idx = 1;
      while (idx < size - 1 && effort[idx] < targetEffort)
        idx++;
      final int eta = (int) Math.round(plannedSeconds * (effort[idx] / totalEffort));
      checkpoints.add(new Checkpoint(data.getDistance(idx), data.getAltitude(idx), Math.max(60, eta)));
    }

    final List<Peak> peaks = new ArrayList<>();
    for (int idx : dedup)
    {
      if (peaks.size() == 6)
        break;
      final int eta = (int) Math.round(plannedSeconds * (effort[idx] / totalEffort));
      peaks.add(new Peak(data.getDistance(idx), data.getAltitude(idx), Math.max(60, eta)));
    }

    return new Result(data.getMaxAltitude(), data.getMinAltitude(), data.getTotalAscent(), data.getTotalDescent(),
                      maxGrade, peaks, checkpoints, hazards);
  }
}
