package app.organicmaps.safety;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;

/** Local offline notifications for hiking schedule drift. */
public final class TripScheduleNotifier
{
  private static final String CHANNEL_ID = "AREAMAP_TRIP_SCHEDULE";
  private static final int NOTIFICATION_ID = 71842;

  private TripScheduleNotifier() {}

  public static void createNotificationChannel(@NonNull Context context)
  {
    final NotificationChannelCompat channel =
        new NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_HIGH)
            .setName(context.getString(R.string.areamap_schedule_channel))
            .setDescription(context.getString(R.string.areamap_schedule_channel_description))
            .setVibrationEnabled(true)
            .build();
    NotificationManagerCompat.from(context).createNotificationChannel(channel);
  }

  public static void notifyDelay(@NonNull Context context, int delayMinutes, int checkpointNumber,
                                 @NonNull String checkpointTime, @NonNull String projectedReturnTime)
  {
    final String title = context.getString(R.string.areamap_schedule_delay_title, delayMinutes);
    final String checkpoint = checkpointNumber > 0
        ? context.getString(R.string.areamap_schedule_checkpoint_target, checkpointNumber, checkpointTime)
        : context.getString(R.string.areamap_schedule_finish_target, checkpointTime);
    final String text = context.getString(R.string.areamap_schedule_delay_body, checkpoint, projectedReturnTime);
    post(context, title, text);
  }

  public static boolean postDemo(@NonNull Context context)
  {
    if (!canNotify(context))
    {
      Toast.makeText(context, R.string.areamap_schedule_demo_permission, Toast.LENGTH_LONG).show();
      return false;
    }

    final String checkpoint = context.getString(R.string.areamap_schedule_checkpoint_target, 2, "13:25");
    final String text = context.getString(R.string.areamap_schedule_delay_body, checkpoint, "18:28");
    post(context, context.getString(R.string.areamap_schedule_delay_title, 18), text);
    Toast.makeText(context, R.string.areamap_schedule_demo_sent, Toast.LENGTH_SHORT).show();
    return true;
  }

  private static void post(@NonNull Context context, @NonNull String title, @NonNull String text)
  {
    if (!canNotify(context))
      return;

    final int immutable = Build.VERSION.SDK_INT < Build.VERSION_CODES.M ? 0 : PendingIntent.FLAG_IMMUTABLE;
    final Intent open = new Intent(context, MwmActivity.class)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    final PendingIntent pendingIntent =
        PendingIntent.getActivity(context, 71842, open, PendingIntent.FLAG_UPDATE_CURRENT | immutable);

    final NotificationCompat.Builder notification =
        new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.warning_icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent);

    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification.build());
  }

  private static boolean canNotify(@NonNull Context context)
  {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        || ActivityCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PERMISSION_GRANTED;
  }
}
