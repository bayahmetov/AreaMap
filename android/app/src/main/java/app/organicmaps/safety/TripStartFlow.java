package app.organicmaps.safety;

import android.app.Activity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import app.organicmaps.R;
import app.organicmaps.sdk.location.TrackRecorder;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.DateFormat;
import java.util.Date;

public final class TripStartFlow
{
  private TripStartFlow() {}

  public static void show(@NonNull Activity activity, @NonNull TripPlan plan,
                          @NonNull Runnable startDemo, @NonNull Runnable startRegistered)
  {
    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.areamap_start_mode_title)
        .setMessage(R.string.areamap_start_mode_message)
        .setNegativeButton(R.string.cancel, null)
        .setNeutralButton(R.string.areamap_start_demo, (dialog, which) -> {
          Toast.makeText(activity, R.string.areamap_demo_notice, Toast.LENGTH_LONG).show();
          startDemo.run();
        })
        .setPositiveButton(R.string.areamap_start_registered,
                           (dialog, which) -> showRegistration(activity, plan, startRegistered))
        .show();
  }

  private static void showRegistration(@NonNull Activity activity, @NonNull TripPlan plan,
                                       @NonNull Runnable startRegistered)
  {
    final View content = LayoutInflater.from(activity).inflate(R.layout.areamap_trip_registration, null);
    final TextView summary = content.findViewById(R.id.trip_registration_summary);
    final EditText name = content.findViewById(R.id.trip_registration_name);
    final EditText phone = content.findViewById(R.id.trip_registration_phone);
    final EditText group = content.findViewById(R.id.trip_registration_group_size);
    final EditText emergencyName = content.findViewById(R.id.trip_registration_emergency_name);
    final EditText emergencyPhone = content.findViewById(R.id.trip_registration_emergency_phone);
    final CheckBox consent = content.findViewById(R.id.trip_registration_consent);

    final TripSafety safety = TripSafety.get(activity);
    final TripSafety.Profile saved = safety.getProfile();
    name.setText(saved.name);
    phone.setText(saved.phone);
    group.setText(Integer.toString(saved.groupSize));
    emergencyName.setText(saved.emergencyName);
    emergencyPhone.setText(saved.emergencyPhone);
    summary.setText(planSummary(activity, plan, System.currentTimeMillis()));

    final AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.areamap_trip_registration_title)
        .setView(content)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.areamap_continue, null)
        .create();
    dialog.setOnShowListener(ignored -> dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
      final String personName = name.getText().toString().trim();
      final String personPhone = phone.getText().toString().trim();
      final String contactName = emergencyName.getText().toString().trim();
      final String contactPhone = emergencyPhone.getText().toString().trim();
      final int groupSize = parseGroupSize(group.getText().toString());

      boolean valid = true;
      if (personName.isEmpty())
      {
        name.setError(activity.getString(R.string.areamap_required_field));
        valid = false;
      }
      if (!validPhone(personPhone))
      {
        phone.setError(activity.getString(R.string.areamap_phone_error));
        valid = false;
      }
      if (groupSize < 1)
      {
        group.setError(activity.getString(R.string.areamap_group_error));
        valid = false;
      }
      if (contactName.isEmpty())
      {
        emergencyName.setError(activity.getString(R.string.areamap_required_field));
        valid = false;
      }
      if (!validPhone(contactPhone))
      {
        emergencyPhone.setError(activity.getString(R.string.areamap_phone_error));
        valid = false;
      }
      if (!consent.isChecked())
      {
        Toast.makeText(activity, R.string.areamap_consent_required, Toast.LENGTH_LONG).show();
        valid = false;
      }
      if (!valid)
        return;

      dialog.dismiss();
      final TripSafety.Profile profile =
          new TripSafety.Profile(personName, personPhone, groupSize, contactName, contactPhone);
      showConfirmation(activity, plan, profile, startRegistered);
    }));
    dialog.show();
  }

  private static void showConfirmation(@NonNull Activity activity, @NonNull TripPlan plan,
                                       @NonNull TripSafety.Profile profile, @NonNull Runnable startRegistered)
  {
    final long now = System.currentTimeMillis();
    final String preview = activity.getString(R.string.areamap_confirm_send_body,
                                              profile.name, profile.phone, profile.groupSize,
                                              profile.emergencyName, profile.emergencyPhone)
        + "\n\n" + planSummary(activity, plan, now);

    new MaterialAlertDialogBuilder(activity)
        .setTitle(R.string.areamap_confirm_send_title)
        .setMessage(preview)
        .setNegativeButton(R.string.areamap_back, (dialog, which) ->
            showRegistration(activity, plan, startRegistered))
        .setPositiveButton(R.string.areamap_confirm_and_start, (dialog, which) -> {
          final TripSafety safety = TripSafety.get(activity);
          safety.startMonitoredTrip(plan, profile, !TrackRecorder.nativeIsTrackRecordingEnabled());
          startRegistered.run();
          TripReportSender.shareToTelegram(activity, safety.startReport());
        })
        .show();
  }

  @NonNull
  private static String planSummary(@NonNull Activity activity, @NonNull TripPlan plan, long startMillis)
  {
    final StringBuilder out = new StringBuilder();
    out.append(activity.getString(R.string.areamap_trip_route_summary,
                                  TextUtils.isEmpty(plan.startTitle) ? activity.getString(R.string.areamap_route_start)
                                                                    : plan.startTitle,
                                  TextUtils.isEmpty(plan.finishTitle) ? activity.getString(R.string.areamap_route_finish)
                                                                     : plan.finishTitle,
                                  plan.distanceMeters / 1000.0,
                                  duration(activity, plan.plannedSeconds),
                                  time(startMillis + plan.plannedSeconds * 1000L),
                                  duration(activity, plan.returnSeconds),
                                  time(startMillis + (plan.plannedSeconds + plan.returnSeconds) * 1000L)));
    if (!plan.checkpoints.isEmpty())
    {
      out.append("\n\n").append(activity.getString(R.string.areamap_checkpoints_title));
      int number = 1;
      for (TripPlan.Checkpoint checkpoint : plan.checkpoints)
      {
        final String altitude = checkpoint.altitudeMeters >= 0
            ? activity.getString(R.string.areamap_report_checkpoint_altitude, checkpoint.altitudeMeters) : "";
        out.append("\n").append(activity.getString(
            R.string.areamap_trip_checkpoint_preview, number++,
            time(startMillis + checkpoint.etaSeconds * 1000L),
            duration(activity, checkpoint.etaSeconds), checkpoint.distanceMeters / 1000.0, altitude));
      }
    }
    return out.toString();
  }

  private static int parseGroupSize(@NonNull String value)
  {
    try
    {
      return Integer.parseInt(value.trim());
    }
    catch (NumberFormatException e)
    {
      return 0;
    }
  }

  private static boolean validPhone(@NonNull String value)
  {
    int digits = 0;
    for (int i = 0; i < value.length(); i++)
      if (Character.isDigit(value.charAt(i)))
        digits++;
    return digits >= 7;
  }

  @NonNull
  private static String time(long millis)
  {
    return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(millis));
  }

  @NonNull
  private static String duration(@NonNull Activity activity, int seconds)
  {
    final int minutes = Math.max(0, seconds / 60);
    if (minutes < 60)
      return activity.getString(R.string.areamap_duration_minutes, minutes);
    return activity.getString(R.string.areamap_duration_hours_minutes, minutes / 60, minutes % 60);
  }
}
