package app.organicmaps.safety;

import androidx.fragment.app.Fragment;
import app.organicmaps.base.BaseToolbarActivity;

public class GuideArticleActivity extends BaseToolbarActivity
{
  public static final String EXTRA_ARTICLE_INDEX = "areamap.extra.ARTICLE_INDEX";

  @Override
  protected Class<? extends Fragment> getFragmentClass()
  {
    return GuideArticleFragment.class;
  }
}
