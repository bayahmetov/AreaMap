package app.organicmaps.safety;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import app.organicmaps.MwmActivity;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.location.TrackRecordingService;
import app.organicmaps.sdk.location.TrackRecorder;
import app.organicmaps.sdk.routing.RoutingController;
import app.organicmaps.util.WindowInsetUtils.PaddingInsetsListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TripSafetyFragment extends BaseMwmFragment
{
  private TripSafety mSos;
  private long mLastBreakTap;
  private final SharedPreferences.OnSharedPreferenceChangeListener mDeliveryListener = (prefs, key) ->
  {
    final View view = getView();
    if (view != null && isAdded())
      ((TextView) view.findViewById(R.id.trip_delivery_status)).setText(TripReportSender.summary(requireContext()));
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
    requireActivity().setTitle(R.string.areamap_nav_trip);
    mSos = TripSafety.get(requireContext());
    view.findViewById(R.id.trip_open_profile)
        .setOnClickListener(
            v
            -> startActivity(new Intent(requireContext(), app.organicmaps.profile.AreaMapProfileActivity.class)
                                 .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)));

    view.findViewById(R.id.guide_import)
        .setOnClickListener(v -> startActivity(new Intent(requireContext(), RouteImportActivity.class)));
    view.findViewById(R.id.trip_sos).setOnClickListener(v -> showSos());
    view.findViewById(R.id.active_trip_resend)
        .setOnClickListener(v -> TripReportSender.recover(requireContext(), true));
    view.findViewById(R.id.trip_delivery_retry)
        .setOnClickListener(v -> TripReportSender.recover(requireContext(), true));
    view.findViewById(R.id.active_trip_continue)
        .setOnClickListener(
            v
            -> startActivity(new Intent(requireContext(), MwmActivity.class)
                                 .putExtra("areamap_resume_trip", true)
                                 .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)));
    view.findViewById(R.id.active_trip_finish).setOnClickListener(v -> confirmManualReturn());
    view.findViewById(R.id.active_trip_ok).setOnClickListener(v -> {
      mSos.markImOk();
      TripReportSender.send(requireActivity(), "ok", mSos.okReport());
      Toast.makeText(requireContext(), R.string.areamap_trip_ok_saved, Toast.LENGTH_SHORT).show();
      refreshActiveTrip(view);
    });
    view.findViewById(R.id.active_trip_break).setOnClickListener(v -> {
      final long now = android.os.SystemClock.elapsedRealtime();
      if (now - mLastBreakTap < 1000)
        return;
      mLastBreakTap = now;
      mSos.addBreakMinutes(20);
      TripReportSender.send(requireActivity(), "break", mSos.breakReport(20));
      Toast.makeText(requireContext(), R.string.areamap_trip_break_added, Toast.LENGTH_SHORT).show();
      refreshActiveTrip(view);
    });
    view.findViewById(R.id.active_trip_weather_refresh).setOnClickListener(v -> {
      Toast.makeText(requireContext(), R.string.areamap_weather_refresh_queued, Toast.LENGTH_SHORT).show();
      final android.content.Context appContext = requireContext().getApplicationContext();
      new Thread(() -> {
        if (mSos.hasActiveTrip())
          TripWeatherRepository.refreshForPoint(appContext, mSos.weatherLat(), mSos.weatherLon(),
                                                mSos.weatherAltitudeMeters(), true);
        TripWeatherNotifier.evaluate(appContext, mSos);
        final android.app.Activity activity = getActivity();
        if (activity == null)
          return;
        activity.runOnUiThread(() -> {
          final View current = getView();
          if (current != null)
            refreshActiveTrip(current);
        });
      }, "AreaMapWeatherRefresh").start();
    });
    view.findViewById(R.id.schedule_demo_notification).setOnClickListener(v -> showScheduleDemo());
    view.findViewById(R.id.language_change).setOnClickListener(v -> AreaMapLocale.showPicker(requireActivity()));
    ((TextView) view.findViewById(R.id.language_current))
        .setText(getString(R.string.areamap_language_current, AreaMapLocale.selectedLabel(requireContext())));

    refreshLastLocation(view);
    refreshActiveTrip(view);

    if (requireActivity().getIntent().getBooleanExtra(TripSafetyActivity.EXTRA_SHOW_SOS, false))
    {
      requireActivity().getIntent().removeExtra(TripSafetyActivity.EXTRA_SHOW_SOS);
      view.post(this::showSos);
    }
  }

  private void showScheduleDemo()
  {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
               != android.content.pm.PackageManager.PERMISSION_GRANTED)
    {
      ActivityCompat.requestPermissions(requireActivity(), new String[] {Manifest.permission.POST_NOTIFICATIONS}, 9042);
      Toast.makeText(requireContext(), R.string.areamap_schedule_demo_permission_request, Toast.LENGTH_LONG).show();
      return;
    }
    TripScheduleNotifier.postDemo(requireContext());
  }

  @Override
  public void onResume()
  {
    super.onResume();
    requireContext()
        .getSharedPreferences("areamap_delivery", android.content.Context.MODE_PRIVATE)
        .registerOnSharedPreferenceChangeListener(mDeliveryListener);
    TripReportSender.recover(requireContext(), false);
    final View view = getView();
    if (view != null)
    {
      refreshLastLocation(view);
      refreshActiveTrip(view);
      ((TextView) view.findViewById(R.id.language_current))
          .setText(getString(R.string.areamap_language_current, AreaMapLocale.selectedLabel(requireContext())));
    }
  }

  @Override
  public void onPause()
  {
    requireContext()
        .getSharedPreferences("areamap_delivery", android.content.Context.MODE_PRIVATE)
        .unregisterOnSharedPreferenceChangeListener(mDeliveryListener);
    super.onPause();
  }

  private void refreshActiveTrip(@NonNull View view)
  {
    ((TextView) view.findViewById(R.id.trip_delivery_status)).setText(TripReportSender.summary(requireContext()));
    final View section = view.findViewById(R.id.active_trip_section);
    if (!mSos.hasActiveTrip())
    {
      section.setVisibility(View.GONE);
      return;
    }
    section.setVisibility(View.VISIBLE);
    ((TextView) view.findViewById(R.id.active_trip_summary)).setText(mSos.activeTripSummary());
    ((TextView) view.findViewById(R.id.active_trip_weather))
        .setText(TripWeatherNotifier.summary(requireContext(), mSos));
  }

  private void confirmManualReturn()
  {
    if (!mSos.hasActiveTrip())
      return;
    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_manual_return_title)
        .setMessage(R.string.areamap_manual_return_message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.areamap_finish_and_send, (dialog, which) -> finishTrip())
        .show();
  }

  private void finishTrip()
  {
    final boolean stopOwnedRecording = mSos.ownsTrackRecording();
    final String report = mSos.returnReport();
    if (!TripReportSender.send(requireActivity(), "finish", report))
      return;
    mSos.completeTrip();
    TripMonitoringService.stopIfUnused(requireContext());
    if (RoutingController.get().isNavigating())
      RoutingController.get().cancel();
    if (stopOwnedRecording && TrackRecorder.nativeIsTrackRecordingEnabled())
    {
      TrackRecorder.saveAndStop();
      TrackRecordingService.stopService(requireContext());
    }
    Toast.makeText(requireContext(), R.string.areamap_trip_completed, Toast.LENGTH_LONG).show();
    final View view = getView();
    if (view != null)
      refreshActiveTrip(view);
  }

  private void refreshLastLocation(@NonNull View view)
  {
    final Location last = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (last != null)
      mSos.save(last);
    ((TextView) view.findViewById(R.id.trip_coordinates)).setText(mSos.coordinates());
  }

  private void showSos()
  {
    if (isAdded() && getView() != null)
      SosConfirmationFlow.show(requireActivity(), this::sendSos);
  }

  private void sendSos()
  {
    final Location last = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (last != null)
      mSos.save(last);
    TripReportSender.send(requireActivity(), "sos", mSos.sosReport());

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_sos)
        .setMessage(mSos.card() + "\n\n" + TripReportSender.summary(requireContext()))
        .setNegativeButton(R.string.areamap_close, null)
        .setPositiveButton(R.string.areamap_dial,
                           (dialog, which) -> openIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))))
        .show();
  }

  private void openIntent(@NonNull Intent intent)
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
}
