package app.organicmaps.safety;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import app.organicmaps.util.WindowInsetUtils.PaddingInsetsListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class TripSafetyFragment extends BaseMwmFragment
{
  private TripSafety mSos;

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
    mSos = TripSafety.get(requireContext());

    view.findViewById(R.id.trip_sos).setOnClickListener(v -> showSos());
    refreshLastLocation(view);
    if (requireActivity().getIntent().getBooleanExtra(TripSafetyActivity.EXTRA_SHOW_SOS, false))
    {
      requireActivity().getIntent().removeExtra(TripSafetyActivity.EXTRA_SHOW_SOS);
      view.post(this::showSos);
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();
    final View view = getView();
    if (view != null)
      refreshLastLocation(view);
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
    final Location last = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (last != null)
      mSos.save(last);

    new MaterialAlertDialogBuilder(requireContext())
        .setTitle(R.string.areamap_sos)
        .setMessage(mSos.card())
        .setNegativeButton(R.string.areamap_close, null)
        .setPositiveButton(R.string.areamap_dial,
                           (dialog, which) -> openIntent(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))))
        .setNeutralButton(R.string.areamap_share,
                          (dialog, which) -> {
                            final Intent send = new Intent(Intent.ACTION_SEND)
                                                    .setType("text/plain")
                                                    .putExtra(Intent.EXTRA_TEXT, mSos.card());
                            openIntent(Intent.createChooser(send, getString(R.string.areamap_share)));
                          })
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
