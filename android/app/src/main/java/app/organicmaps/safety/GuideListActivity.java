package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.base.BaseToolbarActivity;

public class GuideListActivity extends BaseToolbarActivity
{
  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return GuideListFragment.class;
  }
}
