package app.organicmaps.safety;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.ViewCompat;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.location.TrackRecordingService;
import app.organicmaps.sdk.location.LocationUtils;
import app.organicmaps.sdk.location.TrackRecorder;
import app.organicmaps.util.WindowInsetUtils.PaddingInsetsListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TripSafetyFragment extends BaseMwmFragment
{
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private TripSafety mTrip;
  private EditText mRoute;
  private EditText mGroup;
  private EditText mContact;
  private EditText mHours;
  private final ActivityResultLauncher<String[]> mLocationPermission =
      registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
        if (LocationUtils.checkFineLocationPermission(requireContext()))
          requestNotificationsAndStart();
        else
          Toast.makeText(requireContext(), R.string.areamap_permission, Toast.LENGTH_LONG).show();
      });
  private final ActivityResultLauncher<String> mNotificationPermission =
      registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> startTrip());
  private final Runnable mRefresh = new Runnable() {
    @Override
    public void run()
    {
      refresh();
      mHandler.postDelayed(this, 1000);
    }
  };

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_trip_safety, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    ViewCompat.setOnApplyWindowInsetsListener(view, PaddingInsetsListener.excludeTop());
    mTrip = TripSafety.get(requireContext());
    mRoute = view.findViewById(R.id.trip_route);
    mGroup = view.findViewById(R.id.trip_group);
    mContact = view.findViewById(R.id.trip_contact);
    mHours = view.findViewById(R.id.trip_hours);
    if (state == null)
    {
      mRoute.setText(mTrip.route());
      mGroup.setText(mTrip.group());
      mContact.setText(mTrip.contact());
      if (mTrip.active())
        mHours.setText(mTrip.hours());
    }
    view.findViewById(R.id.trip_start).setOnClickListener(v -> {
      if (!mTrip.active() && !validate())
        return;
      if (!LocationUtils.checkFineLocationPermission(requireContext()))
        mLocationPermission.launch(
            new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION});
      else
        requestNotificationsAndStart();
    });
    view.findViewById(R.id.trip_ok).setOnClickListener(v -> {
      mTrip.acknowledge();
      refresh();
    });
    view.findViewById(R.id.trip_rest).setOnClickListener(v -> {
      mTrip.rest();
      refresh();
    });
    view.findViewById(R.id.trip_end)
        .setOnClickListener(v
                            -> new MaterialAlertDialogBuilder(requireContext())
                                   .setTitle(R.string.areamap_end)
                                   .setMessage(R.string.areamap_end_confirm)
                                   .setNegativeButton(R.string.cancel, null)
                                   .setPositiveButton(R.string.areamap_end,
                                                      (dialog, which) -> {
                                                        mTrip.finish();
                                                        if (TrackRecorder.nativeIsTrackRecordingEnabled())
                                                          TrackRecorder.saveAndStop();
                                                        TrackRecordingService.stopService(requireContext());
                                                        refresh();
                                                      })
                                   .show());
    view.findViewById(R.id.trip_sos).setOnClickListener(v -> showSos());
    view.findViewById(R.id.trip_demo).setOnClickListener(v -> showDemo());
  }

  private boolean validate()
  {
    if (mRoute.getText().toString().trim().isEmpty())
    {
      mRoute.setError(getString(R.string.areamap_required));
      return false;
    }
    try
    {
      final int group = Integer.parseInt(mGroup.getText().toString());
      final int hours = Integer.parseInt(mHours.getText().toString());
      if (group >= 1 && group <= 99 && hours >= 1 && hours <= 72)
        return true;
    }
    catch (NumberFormatException ignored)
    {}
    Toast.makeText(requireContext(), R.string.areamap_invalid_plan, Toast.LENGTH_LONG).show();
    return false;
  }

  private void requestNotificationsAndStart()
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && !NotificationManagerCompat.from(requireContext()).areNotificationsEnabled())
      mNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    else
      startTrip();
  }

  @android.annotation.SuppressLint("MissingPermission")
  private void startTrip()
  {
    if (!LocationUtils.checkFineLocationPermission(requireContext()))
      return;
    if (!mTrip.active())
    {
      if (!validate())
        return;
      if (TrackRecorder.nativeIsTrackRecordingEnabled())
      {
        Toast.makeText(requireContext(), R.string.areamap_existing_recording, Toast.LENGTH_LONG).show();
        return;
      }
      mTrip.start(mRoute.getText().toString().trim(), mGroup.getText().toString(), mContact.getText().toString().trim(),
                  Integer.parseInt(mHours.getText().toString()));
    }
    else
      mTrip.resume();
    try
    {
      TrackRecordingService.startForegroundService(requireContext());
    }
    catch (IllegalStateException | SecurityException e)
    {
      mTrip.onServiceStopped();
      Toast.makeText(requireContext(), R.string.areamap_interrupted, Toast.LENGTH_LONG).show();
    }
    refresh();
  }

  private void refresh()
  {
    final View view = getView();
    if (view == null)
      return;
    ((TextView) view.findViewById(R.id.trip_status)).setText(mTrip.status());
    ((TextView) view.findViewById(R.id.trip_coordinates)).setText(mTrip.coordinates());
    view.findViewById(R.id.trip_notification_warning)
        .setVisibility(NotificationManagerCompat.from(requireContext()).areNotificationsEnabled() ? View.GONE
                                                                                                  : View.VISIBLE);
    final Button start = view.findViewById(R.id.trip_start);
    start.setText(mTrip.active() ? R.string.areamap_resume : R.string.areamap_start);
    start.setVisibility(mTrip.running() ? View.GONE : View.VISIBLE);
    view.findViewById(R.id.trip_ok).setEnabled(mTrip.running());
    view.findViewById(R.id.trip_rest).setEnabled(mTrip.running());
    view.findViewById(R.id.trip_end).setEnabled(mTrip.active());
    mRoute.setEnabled(!mTrip.active());
    mGroup.setEnabled(!mTrip.active());
    mContact.setEnabled(!mTrip.active());
    mHours.setEnabled(!mTrip.active());
  }

  private void showSos()
  {
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_sos)
        .setMessage(mTrip.card())
        .setNegativeButton(R.string.close, null)
        .setPositiveButton(R.string.areamap_dial,
                           (dialog, which) -> openIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))))
        .setNeutralButton(
            R.string.areamap_share,
            (dialog, which) -> {
              final Intent send =
                  new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, mTrip.card());
              openIntent(Intent.createChooser(send, getString(R.string.areamap_share)));
            })
        .show();
  }

  private void openIntent(Intent intent)
  {
    try
    {
      startActivity(intent);
    }
    catch (ActivityNotFoundException e)
    {
      Toast.makeText(requireContext(), R.string.areamap_no_handler, Toast.LENGTH_LONG).show();
    }
  }

  private void showDemo()
  {
    // A separate engine: no real session, location, notification, or call is affected.
    final SafetyEngine demo = new SafetyEngine(30_000, 10_000);
    demo.accept(43.2, 77.0, 5, 1000, 1000);
    demo.accept(43.2, 77.0, 5, 31_000, 31_000);
    final int question = TripSafety.stateText(demo.state(31_000));
    final int unanswered = TripSafety.stateText(demo.state(41_000));
    demo.acknowledge();
    final int acknowledged = TripSafety.stateText(demo.state(41_000));
    demo.locationUnavailable();
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_demo)
        .setMessage(getString(R.string.areamap_demo_result, getString(question), getString(unanswered),
                              getString(acknowledged), getString(TripSafety.stateText(demo.state(42_000)))))
        .setPositiveButton(R.string.ok, null)
        .show();
  }

  @Override
  public void onResume()
  {
    super.onResume();
    mHandler.post(mRefresh);
  }

  @Override
  public void onPause()
  {
    mHandler.removeCallbacks(mRefresh);
    super.onPause();
  }
}
