package app.organicmaps.safety;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import app.organicmaps.R;
import app.organicmaps.base.BaseMwmFragment;
import java.util.List;

/** The existing article destination renders offline content as readable, structured blocks. */
public class GuideArticleFragment extends BaseMwmFragment
{
  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.fragment_guide_article, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state)
  {
    super.onViewCreated(view, state);
    final List<GuideArticles.Article> articles = GuideArticles.all(requireContext());
    int index = requireActivity().getIntent().getIntExtra(GuideArticleActivity.EXTRA_ARTICLE_INDEX, 0);
    if (index < 0 || index >= articles.size()) index = 0;
    final GuideArticles.Article article = articles.get(index);
    final ImageView image = view.findViewById(R.id.article_image);
    new GuidePhotos(requireContext()).load(image, article.imageAsset);
    GuidePhotos.round(image);
    ((TextView) view.findViewById(R.id.article_title)).setText(article.title);
    ((TextView) view.findViewById(R.id.article_tags)).setText(
        getString(GuideCategory.find(article.category).title) + " · "
        + getString(R.string.g_minutes, article.readingTime, getString(R.string.g_level)));
    view.findViewById(R.id.article_back).setOnClickListener(v -> requireActivity().finish());
    view.findViewById(R.id.article_credits).setOnClickListener(v -> GuidePhotos.credits(requireContext()));
    final LinearLayout blocks = view.findViewById(R.id.article_blocks);
    for (String paragraph : article.body.split("\\n(?:\\n)?"))
    {
      if (paragraph.trim().isEmpty()) continue;
      final boolean heading = paragraph.startsWith("# ");
      final boolean checklist = paragraph.startsWith("[ ] ");
      final boolean warning = paragraph.startsWith("! ");
      final TextView block = checklist ? new CheckBox(requireContext()) : new TextView(requireContext());
      block.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                           ViewGroup.LayoutParams.WRAP_CONTENT));
      final int padding = getResources().getDimensionPixelSize(R.dimen.home_gap);
      block.setPadding(padding, padding, padding, padding);
      block.setTextColor(ContextCompat.getColor(requireContext(), warning ? R.color.areamap_amber : R.color.areamap_text));
      block.setTextSize(heading ? 21 : 16);
      block.setLineSpacing(5 * getResources().getDisplayMetrics().density, 1);
      if (heading) block.setTypeface(null, android.graphics.Typeface.BOLD);
      if (warning) block.setBackgroundResource(R.drawable.home_surface);
      block.setText(heading || warning ? paragraph.substring(2)
          : checklist ? paragraph.substring(4) : paragraph.startsWith("- ") ? "• " + paragraph.substring(2) : paragraph);
      if (!checklist) { block.setTextIsSelectable(true); android.text.util.Linkify.addLinks(block, android.text.util.Linkify.WEB_URLS); }
      blocks.addView(block);
    }
    final boolean emergency = index < 9 || article.id.equals("signal") || article.id.equals("plants");
    view.findViewById(R.id.article_emergency).setVisibility(emergency ? View.VISIBLE : View.GONE);
    view.findViewById(R.id.article_dial).setOnClickListener(v -> {
      final Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"));
      if (dial.resolveActivity(requireContext().getPackageManager()) != null) startActivity(dial);
    });
    ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
      final Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      v.setPadding(bars.left, bars.top, bars.right, bars.bottom); return insets;
    });
    ViewCompat.requestApplyInsets(view);
  }
}
