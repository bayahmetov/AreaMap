package app.organicmaps.safety;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import java.util.List;

public class GuideArticleFragment extends BaseMwmFragment
{
  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_guide_article, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    final List<GuideArticles.Article> articles = GuideArticles.all();
    int index = requireActivity().getIntent().getIntExtra(GuideArticleActivity.EXTRA_ARTICLE_INDEX, 0);
    if (index < 0 || index >= articles.size())
      index = 0;

    final GuideArticles.Article article = articles.get(index);
    requireActivity().setTitle(article.title);
    ((TextView) view.findViewById(R.id.article_title)).setText(article.title);
    ((TextView) view.findViewById(R.id.article_tags)).setText(article.tags);
    ((TextView) view.findViewById(R.id.article_body)).setText(article.body);
  }
}
