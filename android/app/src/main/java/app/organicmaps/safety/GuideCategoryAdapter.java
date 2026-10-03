package app.organicmaps.safety;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import java.util.List;
import java.util.function.Consumer;

final class GuideCategoryAdapter extends RecyclerView.Adapter<GuideCategoryAdapter.Holder>
{
  private final List<GuideCategory> mCategories = GuideCategory.all();
  private final List<GuideArticles.Article> mArticles;
  private final Consumer<String> mOpen;
  private final GuidePhotos mPhotos;
  GuideCategoryAdapter(Context context, List<GuideArticles.Article> articles, Consumer<String> open)
  { mArticles = articles; mOpen = open; mPhotos = new GuidePhotos(context); }

  @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type)
  {
    final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.areamap_guide_category, parent, false);
    GuidePhotos.round(view); return new Holder(view);
  }
  @Override public void onBindViewHolder(@NonNull Holder holder, int position)
  {
    final GuideCategory category = mCategories.get(position);
    final Context context = holder.itemView.getContext();
    int count = 0;
    for (GuideArticles.Article article : mArticles) if (article.category.equals(category.id)) count++;
    holder.title.setText(context.getString(category.title) + "  ›");
    holder.count.setText(context.getString(R.string.g_count, count));
    holder.icon.setImageResource(category.icon);
    holder.icon.setBackgroundTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, category.accent)));
    holder.icon.setColorFilter(androidx.core.content.ContextCompat.getColor(context, R.color.areamap_bg));
    mPhotos.load(holder.image, "areamap/guides/guide_" + category.image + ".webp");
    final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
    params.height = Math.round(context.getResources().getDimension(R.dimen.guide_category_height)
        * Math.max(1f, context.getResources().getConfiguration().fontScale));
    holder.itemView.setLayoutParams(params);
    holder.itemView.setOnClickListener(v -> mOpen.accept(category.id));
  }
  @Override public int getItemCount() { return mCategories.size(); }
  static final class Holder extends RecyclerView.ViewHolder
  {
    final TextView title, count; final ImageView image, icon;
    Holder(View view)
    {
      super(view); title = view.findViewById(R.id.category_title); count = view.findViewById(R.id.category_count);
      image = view.findViewById(R.id.category_image); icon = view.findViewById(R.id.category_icon);
    }
  }
}
