package app.organicmaps.safety;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import java.util.Locale;

public class SosFragment extends BaseMwmFragment
{
  private long mHoldStartedAt;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_areamap_sos, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    requireActivity().setTitle(R.string.areamap_sos_page_title);

    final TripSafety safety = TripSafety.get(requireContext());
    final Location location = MwmApplication.from(requireContext()).getLocationHelper().getSavedLocation();
    if (location != null)
      safety.save(location);

    final String coordinates = safety.coordinates();
    ((TextView) view.findViewById(R.id.sos_coordinates)).setText(coordinates);

    final String tripSummary = safety.activeTripSummary();
    final int newline = tripSummary.indexOf('\n');
    ((TextView) view.findViewById(R.id.sos_route_value))
        .setText(newline >= 0 ? tripSummary.substring(0, newline) : tripSummary);
    ((TextView) view.findViewById(R.id.sos_route_meta))
        .setText(newline >= 0 ? tripSummary.substring(newline + 1) : "");

    ((TextView) view.findViewById(R.id.sos_altitude_value)).setText(
        location != null && location.hasAltitude()
            ? String.format(Locale.getDefault(), "%.0f м", location.getAltitude())
            : "—");

    final BatteryManager battery = (BatteryManager) requireContext().getSystemService(Context.BATTERY_SERVICE);
    final int level = battery == null ? -1 : battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    ((TextView) view.findViewById(R.id.sos_battery_value)).setText(level >= 0 ? level + "%" : "—");

    view.findViewById(R.id.sos_copy).setOnClickListener(v -> copyCoordinates(coordinates));
    view.findViewById(R.id.sos_call).setOnClickListener(v -> open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))));
    view.findViewById(R.id.sos_share).setOnClickListener(v -> {
      final Intent send = new Intent(Intent.ACTION_SEND)
          .setType("text/plain")
          .putExtra(Intent.EXTRA_TEXT, safety.card());
      open(Intent.createChooser(send, getString(R.string.areamap_sos_share_location)));
    });

    final View hold = view.findViewById(R.id.sos_hold);
    hold.setOnTouchListener((v, event) -> {
      if (event.getActionMasked() == MotionEvent.ACTION_DOWN)
      {
        mHoldStartedAt = System.currentTimeMillis();
        v.setPressed(true);
        return true;
      }
      if (event.getActionMasked() == MotionEvent.ACTION_UP)
      {
        v.setPressed(false);
        if (System.currentTimeMillis() - mHoldStartedAt >= 3000)
          open(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")));
        else
          Toast.makeText(requireContext(), R.string.areamap_sos_hold_hint, Toast.LENGTH_SHORT).show();
        return true;
      }
      if (event.getActionMasked() == MotionEvent.ACTION_CANCEL)
      {
        v.setPressed(false);
        return true;
      }
      return false;
    });
  }

  private void copyCoordinates(@NonNull String value)
  {
    final ClipboardManager clipboard =
        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null)
      clipboard.setPrimaryClip(ClipData.newPlainText("AreaMap SOS", value));
    Toast.makeText(requireContext(), R.string.areamap_sos_copied, Toast.LENGTH_SHORT).show();
  }

  private void open(@NonNull Intent intent)
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
