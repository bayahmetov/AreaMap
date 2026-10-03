package app.organicmaps.safety;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class GuideArticleAdapter extends RecyclerView.Adapter<GuideArticleAdapter.Holder>
{
  private final List<GuideArticles.Article> mItems = new ArrayList<>();
  private final Consumer<GuideArticles.Article> mOpen;
  private final Runnable mFavoriteChanged;
  private final SharedPreferences mFavorites;
  private final GuidePhotos mPhotos;
  private final boolean mCompact;
  private boolean mListing;
  void setListing(boolean listing) { mListing = listing; }

  GuideArticleAdapter(Context context, boolean compact, Consumer<GuideArticles.Article> open, Runnable favoriteChanged)
  {
    mCompact = compact; mOpen = open; mFavoriteChanged = favoriteChanged;
    mFavorites = context.getSharedPreferences("areamap_guide_favorites", Context.MODE_PRIVATE);
    mPhotos = new GuidePhotos(context);
  }
  boolean isFavorite(GuideArticles.Article article) { return mFavorites.getBoolean(article.id, false); }
  void submit(List<GuideArticles.Article> items) { mItems.clear(); mItems.addAll(items); notifyDataSetChanged(); }
  @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type)
  {
    final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.areamap_guide_card, parent, false);
    GuidePhotos.round(view); return new Holder(view);
  }
  @Override public void onBindViewHolder(@NonNull Holder holder, int position)
  {
    final GuideArticles.Article article = mItems.get(position);
    final Context context = holder.itemView.getContext();
    holder.title.setText(article.title);
    holder.rating.setText(context.getString(R.string.g_rating, article.rating));
    holder.summary.setText(context.getString(R.string.g_minutes, article.readingTime, context.getString(R.string.g_level)));
    mPhotos.load(holder.image, article.imageAsset);
    final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
    params.width = mListing ? ViewGroup.LayoutParams.MATCH_PARENT : Math.min(context.getResources().getDimensionPixelSize(R.dimen.guide_article_width),
        context.getResources().getDisplayMetrics().widthPixels * 4 / 5);
    params.height = Math.round(context.getResources().getDimension(R.dimen.guide_article_height)
        * (mCompact ? 0.85f : 1f) * Math.max(1f, context.getResources().getConfiguration().fontScale));
    holder.itemView.setLayoutParams(params);
    holder.itemView.setOnClickListener(v -> mOpen.accept(article));
    holder.favorite.setColorFilter(ContextCompat.getColor(context,
        isFavorite(article) ? R.color.areamap_green : R.color.areamap_text));
    holder.favorite.setContentDescription(context.getString(isFavorite(article) ? R.string.g_saved : R.string.g_save,
                                                            article.title));
    holder.favorite.setOnClickListener(v -> {
      mFavorites.edit().putBoolean(article.id, !isFavorite(article)).apply();
      mFavoriteChanged.run();
    });
  }
  @Override public int getItemCount() { return mItems.size(); }
  static final class Holder extends RecyclerView.ViewHolder
  {
    final TextView title, summary, rating; final ImageView image; final ImageButton favorite;
    Holder(View view)
    {
      super(view); title = view.findViewById(R.id.article_title); summary = view.findViewById(R.id.article_summary);
      rating = view.findViewById(R.id.guide_rating);
      image = view.findViewById(R.id.article_image); favorite = view.findViewById(R.id.guide_favorite);
    }
  }
}
