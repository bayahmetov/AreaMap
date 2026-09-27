package app.organicmaps.safety;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import java.util.List;

public class GuideListFragment extends BaseMwmFragment
{
  private LinearLayout mArticles;
  private List<GuideArticles.Article> mAllArticles;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_guide_list, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    requireActivity().setTitle(R.string.areamap_nav_guides);

    mArticles = view.findViewById(R.id.guide_articles);
    mAllArticles = GuideArticles.all(requireContext());
    render("");

    final EditText search = view.findViewById(R.id.guide_search);
    search.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
      @Override public void afterTextChanged(Editable s) {}
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count)
      {
        render(s == null ? "" : s.toString());
      }
    });
  }

  private void render(@NonNull String query)
  {
    mArticles.removeAllViews();
    for (int i = 0; i < mAllArticles.size(); i++)
    {
      final GuideArticles.Article article = mAllArticles.get(i);
      if (!article.matches(query))
        continue;

      final int articleIndex = i;
      final View card = getLayoutInflater().inflate(R.layout.areamap_guide_card, mArticles, false);
      ((ImageView) card.findViewById(R.id.article_image)).setImageResource(article.imageResId);
      ((TextView) card.findViewById(R.id.article_title)).setText(article.title);
      ((TextView) card.findViewById(R.id.article_summary)).setText(article.tags);
      card.setOnClickListener(v -> startActivity(
          new Intent(requireContext(), GuideArticleActivity.class)
              .putExtra(GuideArticleActivity.EXTRA_ARTICLE_INDEX, articleIndex)));
      mArticles.addView(card);
    }
  }
}
