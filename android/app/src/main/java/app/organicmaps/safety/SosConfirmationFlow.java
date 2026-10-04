package app.organicmaps.safety;

import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import app.organicmaps.MwmApplication;
import app.organicmaps.R;
import app.organicmaps.sdk.routing.RoutingController;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Shared decision UI only. Callers retain their existing SOS payload and delivery actions. */
public final class SosConfirmationFlow implements DefaultLifecycleObserver
{
  @Nullable
  private static SosConfirmationFlow sCurrent;
  private final FragmentActivity mActivity;
  private final Runnable mSend;
  private final TripSafety mSafety;
  private final SosConfirmationGate mGate = new SosConfirmationGate();
  @Nullable
  private AlertDialog mDialog;
  @Nullable
  private TripSafety.Profile mDraft;

  private SosConfirmationFlow(FragmentActivity activity, Runnable send)
  {
    mActivity = activity;
    mSend = send;
    mSafety = TripSafety.get(activity);
  }

  public static void show(@NonNull FragmentActivity activity, @NonNull Runnable send)
  {
    if (sCurrent != null || !activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
      return;
    final SosConfirmationFlow flow = new SosConfirmationFlow(activity, send);
    sCurrent = flow;
    activity.getLifecycle().addObserver(flow);
    // A preview alone is not an active route. Registered hikes also remain active after navigation stops.
    if (flow.mSafety.hasActiveTrip() || RoutingController.get().isNavigating() || GpxNavigation.current != null)
      flow.showConfirmation();
    else
      flow.showDetails();
  }

  private void showDetails()
  {
    final View content = LayoutInflater.from(mActivity).inflate(R.layout.areamap_trip_registration, null);
    ((TextView) content.findViewById(R.id.trip_registration_summary)).setText(R.string.sos_details_message);
    content.findViewById(R.id.trip_registration_consent).setVisibility(View.GONE);
    final EditText name = content.findViewById(R.id.trip_registration_name);
    final EditText phone = content.findViewById(R.id.trip_registration_phone);
    final EditText group = content.findViewById(R.id.trip_registration_group_size);
    final EditText contact = content.findViewById(R.id.trip_registration_emergency_name);
    final EditText contactPhone = content.findViewById(R.id.trip_registration_emergency_phone);
    final TripSafety.Profile saved = mSafety.getProfile();
    name.setText(saved.name);
    phone.setText(saved.phone);
    group.setText(Integer.toString(saved.groupSize));
    contact.setText(saved.emergencyName);
    contactPhone.setText(saved.emergencyPhone);
    final AlertDialog dialog = new MaterialAlertDialogBuilder(mActivity)
                                   .setTitle(R.string.sos_details_title)
                                   .setView(content)
                                   .setNegativeButton(R.string.cancel, (ignored, which) -> close())
                                   .setPositiveButton(R.string.areamap_continue, null)
                                   .create();
    display(dialog);
    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
      if (sCurrent != this || mDraft != null
          || !mActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
        return;
      final String personName = name.getText().toString().trim();
      final String personPhone = phone.getText().toString().trim();
      final String contactName = contact.getText().toString().trim();
      final String emergencyPhone = contactPhone.getText().toString().trim();
      int people;
      try
      {
        people = Integer.parseInt(group.getText().toString().trim());
      }
      catch (NumberFormatException e)
      {
        people = 0;
      }
      boolean valid = required(name, personName);
      valid &= phone(phone, personPhone);
      valid &= required(contact, contactName);
      valid &= phone(contactPhone, emergencyPhone);
      if (people < 1)
      {
        group.setError(mActivity.getString(R.string.areamap_group_error));
        valid = false;
      }
      if (!valid)
        return;
      // Keep edits in memory until explicit SOS confirmation; Continue never persists or sends them.
      mDraft = new TripSafety.Profile(personName, personPhone, people, contactName, emergencyPhone);
      v.setEnabled(false);
      dialog.setOnDismissListener(null);
      dialog.dismiss();
      showConfirmation();
    });
  }

  private boolean required(EditText field, String value)
  {
    if (!value.isEmpty())
      return true;
    field.setError(mActivity.getString(R.string.areamap_required_field));
    return false;
  }

  private boolean phone(EditText field, String value)
  {
    int digits = 0;
    for (int i = 0; i < value.length(); i++)
      if (Character.isDigit(value.charAt(i)))
        digits++;
    if (digits >= 7)
      return true;
    field.setError(mActivity.getString(R.string.areamap_phone_error));
    return false;
  }

  private void showConfirmation()
  {
    String message = mActivity.getString(R.string.sos_confirm_message);
    final Location last = MwmApplication.from(mActivity).getLocationHelper().getSavedLocation();
    if (last == null || !last.hasAccuracy())
      message += "\n\n" + mActivity.getString(R.string.sos_location_unavailable);
    if (mDraft != null)
      message += "\n\n" + mActivity.getString(R.string.areamap_confirm_send_body, mDraft.name, mDraft.phone,
                                            mDraft.groupSize, mDraft.emergencyName, mDraft.emergencyPhone);
    final AlertDialog dialog = new MaterialAlertDialogBuilder(mActivity)
                                   .setTitle(R.string.sos_confirm_title)
                                   .setMessage(message)
                                   .setNegativeButton(R.string.cancel, (ignored, which) -> close())
                                   .setPositiveButton(R.string.sos_confirm_send, null)
                                   .create();
    display(dialog);
    final android.widget.Button confirm = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
    confirm.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(mActivity, R.color.areamap_red)));
    confirm.setTextColor(ContextCompat.getColor(mActivity, R.color.areamap_on_green));
    dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        .setTextColor(ContextCompat.getColor(mActivity, R.color.areamap_text_secondary));
    confirm.setOnClickListener(v -> {
      if (!mActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED) || !mGate.tryConfirm())
        return;
      confirm.setEnabled(false);
      if (mDraft != null)
        mSafety.saveProfile(mDraft);
      dialog.dismiss();
      mSend.run();
    });
  }

  private void display(AlertDialog dialog)
  {
    mDialog = dialog;
    dialog.setOnCancelListener(ignored -> close());
    dialog.setOnDismissListener(ignored -> close());
    dialog.setCanceledOnTouchOutside(false);
    dialog.show();
  }

  private void close()
  {
    mGate.cancel();
    mDraft = null;
    mDialog = null;
    mActivity.getLifecycle().removeObserver(this);
    if (sCurrent == this)
      sCurrent = null;
  }

  @Override
  public void onStop(@NonNull LifecycleOwner owner)
  {
    // Backgrounding and rotation cancel the decision. They never restore a pending send action.
    final AlertDialog dialog = mDialog;
    close();
    if (dialog != null)
    {
      dialog.setOnDismissListener(null);
      dialog.dismiss();
    }
  }
}
