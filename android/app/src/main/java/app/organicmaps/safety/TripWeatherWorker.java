package app.organicmaps.safety;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.concurrent.TimeUnit;

/** Periodically refreshes the active-trip weather and evaluates cached hazards offline. */
public final class TripWeatherWorker extends Worker
{
  private static final String UNIQUE_NOW = "areamap-weather-now";
  private static final String UNIQUE_PERIODIC = "areamap-weather-periodic";

  public TripWeatherWorker(@NonNull Context context, @NonNull WorkerParameters params)
  {
    super(context, params);
  }

  @NonNull
  @Override
  public Result doWork()
  {
    final Context context = getApplicationContext();
    final TripSafety safety = TripSafety.get(context);
    if (!safety.hasActiveTrip())
      return Result.success();

    TripWeatherRepository.refreshIfNeeded(context, safety);
    TripWeatherNotifier.evaluate(context, safety);
    return Result.success();
  }

  public static void start(@NonNull Context context)
  {
    final WorkManager manager = WorkManager.getInstance(context);
    manager.enqueueUniqueWork(UNIQUE_NOW, ExistingWorkPolicy.REPLACE,
        new OneTimeWorkRequest.Builder(TripWeatherWorker.class)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build());
    manager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
        new PeriodicWorkRequest.Builder(TripWeatherWorker.class, 1, TimeUnit.HOURS).build());
  }

  public static void refreshNow(@NonNull Context context)
  {
    WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_NOW, ExistingWorkPolicy.REPLACE,
        new OneTimeWorkRequest.Builder(TripWeatherWorker.class).build());
  }

  public static void stop(@NonNull Context context)
  {
    final WorkManager manager = WorkManager.getInstance(context);
    manager.cancelUniqueWork(UNIQUE_NOW);
    manager.cancelUniqueWork(UNIQUE_PERIODIC);
  }
}
