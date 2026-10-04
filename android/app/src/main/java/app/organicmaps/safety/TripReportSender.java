package app.organicmaps.safety;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import app.organicmaps.BuildConfig;
import app.organicmaps.R;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Reports are committed locally before WorkManager attempts bot delivery. */
public final class TripReportSender
{
  private static ReportQueue sQueue;

  private TripReportSender() {}

  public static synchronized ReportQueue queue(@NonNull Context context)
  {
    if (sQueue == null)
      sQueue = new ReportQueue(new File(context.getApplicationContext().getFilesDir(), "areamap_reports"));
    return sQueue;
  }

  public static boolean isConfigured()
  {
    return !BuildConfig.AREAMAP_DEMO_KEY.trim().isEmpty();
  }

  public static boolean enqueue(@NonNull Context context, @NonNull String kind, @NonNull String scope,
                                @NonNull String report)
  {
    try
    {
      final ReportQueue.Entry entry = queue(context).enqueue(kind, scope, report, System.currentTimeMillis());
      if (!ReportQueue.SENT.equals(entry.state))
        schedule(context, entry.id);
      notifyStateChanged(context);
      return true;
    }
    catch (IOException e)
    {
      return false;
    }
  }

  public static boolean send(@NonNull Activity activity, @NonNull String kind, @NonNull String report)
  {
    final boolean saved = enqueue(activity, kind, TripSafety.get(activity).tripId(), report);
    Toast
        .makeText(activity,
                  saved ? (isConfigured() ? R.string.areamap_report_queued : R.string.areamap_report_bot_missing)
                        : R.string.areamap_report_storage_failed,
                  Toast.LENGTH_LONG)
        .show();
    return saved;
  }

  public static void schedule(@NonNull Context context, @NonNull String id)
  {
    final OneTimeWorkRequest work =
        new OneTimeWorkRequest.Builder(TripReportWorker.class)
            .setInputData(new Data.Builder().putString("event_id", id).build())
            .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("areamap-report-outbox")
            .build();
    WorkManager.getInstance(context).enqueueUniqueWork("areamap-report-" + id, ExistingWorkPolicy.KEEP, work);
  }

  public static void recover(@NonNull Context context, boolean retryBlocked)
  {
    try
    {
      final TripSafety safety = TripSafety.get(context);
      if (safety.hasActiveTrip())
      {
        // Recover a crash between committing the trip and scheduling its first report.
        queue(context).enqueue("start", safety.tripId(), safety.startReport(), System.currentTimeMillis());
      }
      for (ReportQueue.Entry entry : queue(context).entries())
      {
        if (retryBlocked && ReportQueue.BLOCKED.equals(entry.state))
          queue(context).setState(entry.id, ReportQueue.PENDING);
        if (ReportQueue.PENDING.equals(entry.state) || (retryBlocked && ReportQueue.BLOCKED.equals(entry.state)))
          schedule(context, entry.id);
      }
    }
    catch (IOException e)
    {
      // Leave valid snapshots in place; the delivery screen reports the storage error.
    }
    notifyStateChanged(context);
  }

  public static String summary(@NonNull Context context)
  {
    try
    {
      int pending = 0;
      int blocked = 0;
      ReportQueue.Entry lastSos = null;
      for (ReportQueue.Entry entry : queue(context).entries())
      {
        if (ReportQueue.PENDING.equals(entry.state))
          pending++;
        if (ReportQueue.BLOCKED.equals(entry.state))
          blocked++;
        if ("sos".equals(entry.kind))
          lastSos = entry;
      }
      final String sos =
          lastSos == null
              ? ""
              : "\n"
                    + context.getString(ReportQueue.SENT.equals(lastSos.state)      ? R.string.areamap_sos_delivered
                                        : ReportQueue.BLOCKED.equals(lastSos.state) ? R.string.areamap_sos_blocked
                                                                                    : R.string.areamap_sos_pending);
      return context.getString(R.string.areamap_delivery_summary, pending, blocked)
    + (!isConfigured() && pending > 0 ? "\n" + context.getString(R.string.areamap_report_bot_missing) : "") + sos;
    }
    catch (IOException e)
    {
      return context.getString(R.string.areamap_report_storage_failed);
    }
  }

  public static void notifyStateChanged(@NonNull Context context)
  {
    context.getSharedPreferences("areamap_delivery", Context.MODE_PRIVATE)
        .edit()
        .putString("changed", java.util.UUID.randomUUID().toString())
        .apply();
    updateSosNotification(context);
  }

  private static void updateSosNotification(Context context)
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
               != android.content.pm.PackageManager.PERMISSION_GRANTED)
      return;
    try
    {
      ReportQueue.Entry sos = null;
      for (ReportQueue.Entry entry : queue(context).entries())
        if ("sos".equals(entry.kind))
          sos = entry;
      if (sos == null)
        return;
      final android.content.SharedPreferences prefs =
          context.getSharedPreferences("areamap_delivery", Context.MODE_PRIVATE);
      final String signature = sos.id + ":" + sos.state;
      if (signature.equals(prefs.getString("notification_signature", "")))
        return;
      final NotificationManagerCompat manager = NotificationManagerCompat.from(context);
      if (!manager.areNotificationsEnabled())
        return;
      final String channel = "AREAMAP_SOS_DELIVERY";
      manager.createNotificationChannel(
          new NotificationChannelCompat.Builder(channel, NotificationManagerCompat.IMPORTANCE_DEFAULT)
              .setName(context.getString(R.string.areamap_sos))
              .build());
      final boolean sent = ReportQueue.SENT.equals(sos.state);
      final String body = context.getString(sent                                    ? R.string.areamap_sos_delivered
                                            : ReportQueue.BLOCKED.equals(sos.state) ? R.string.areamap_sos_blocked
                                                                                    : R.string.areamap_sos_pending);
      final int immutable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0;
      final PendingIntent open = PendingIntent.getActivity(
          context, 71845, new Intent(context, TripSafetyActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | immutable);
      manager.notify(71845, new NotificationCompat.Builder(context, channel)
                                .setSmallIcon(app.organicmaps.branding.R.drawable.ic_splash)
                                .setContentTitle(context.getString(R.string.areamap_sos))
                                .setContentText(body)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                                .setOnlyAlertOnce(true)
                                .setOngoing(!sent)
                                .setAutoCancel(sent)
                                .setContentIntent(open)
                                .build());
      prefs.edit().putString("notification_signature", signature).apply();
    }
    catch (IOException e)
    {
      // The screen reports storage failures without claiming a message has been delivered.
    }
  }
}
