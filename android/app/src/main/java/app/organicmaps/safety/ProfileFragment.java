package app.organicmaps.safety;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;

public class ProfileFragment extends BaseMwmFragment
{
  // AreaMap redesign profile entry point.
  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_areamap_profile, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    requireActivity().setTitle(R.string.areamap_nav_profile);

    view.findViewById(R.id.profile_safety).setOnClickListener(v ->
        startActivity(new Intent(requireContext(), TripSafetyActivity.class)));
    view.findViewById(R.id.profile_routes).setOnClickListener(v -> simpleMessage());
    view.findViewById(R.id.profile_favorites).setOnClickListener(v -> simpleMessage());
    view.findViewById(R.id.profile_achievements).setOnClickListener(v -> simpleMessage());
    view.findViewById(R.id.profile_history).setOnClickListener(v -> simpleMessage());
    view.findViewById(R.id.profile_content).setOnClickListener(v -> simpleMessage());
    view.findViewById(R.id.profile_edit).setOnClickListener(v -> simpleMessage());
  }

  private void simpleMessage()
  {
    Toast.makeText(requireContext(), R.string.areamap_title, Toast.LENGTH_SHORT).show();
  }
}
