package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragmentActivity;

public class GuideArticleActivity extends BaseMwmFragmentActivity
{
  public static final String EXTRA_ARTICLE_INDEX = "areamap.extra.ARTICLE_INDEX";
  @Override protected Class<? extends Fragment> getFragmentClass() { return GuideArticleFragment.class; }
  @Override protected int getContentLayoutResId() { return R.layout.activity_areamap_guides; }
  @Override protected int getFragmentContentResId() { return R.id.fragment_container; }
}
