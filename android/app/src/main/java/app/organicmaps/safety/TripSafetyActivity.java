package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.base.BaseToolbarActivity;

public class TripSafetyActivity extends BaseToolbarActivity
{
  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return TripSafetyFragment.class;
  }
}
