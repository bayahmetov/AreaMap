package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragmentActivity;

public class GuideListActivity extends BaseMwmFragmentActivity
{
  @Override protected Class<? extends Fragment> getFragmentClass() { return GuideListFragment.class; }
  @Override protected int getContentLayoutResId() { return R.layout.activity_areamap_guides; }
  @Override protected int getFragmentContentResId() { return R.id.fragment_container; }
}
