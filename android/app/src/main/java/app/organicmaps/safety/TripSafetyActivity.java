package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.base.BaseToolbarActivity;

public class TripSafetyActivity extends BaseToolbarActivity
{
  public static final String EXTRA_SHOW_SOS = "areamap.extra.SHOW_SOS";
  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return TripSafetyFragment.class;
  }
}
