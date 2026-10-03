package app.organicmaps.profile;

import android.graphics.Color;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragmentActivity;

public class AreaMapProfileActivity extends BaseMwmFragmentActivity
{
  @Override protected Class<? extends Fragment> getFragmentClass() { return AreaMapProfileFragment.class; }
  @Override protected int getContentLayoutResId() { return R.layout.activity_areamap_profile; }
  @Override protected int getFragmentContentResId() { return R.id.fragment_container; }
  @NonNull @Override protected SystemBarStyle getStatusBarStyle()
  {
    return SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
  }
}
