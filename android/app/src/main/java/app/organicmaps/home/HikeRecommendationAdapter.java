package app.organicmaps.home;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Outline;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.R;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class HikeRecommendationAdapter extends RecyclerView.Adapter<HikeRecommendationAdapter.Holder>
{
  private final List<HikeRecommendation> mItems;
  private final Consumer<HikeRecommendation> mOnOpen;
  private final Map<String, Bitmap> mPhotos = new HashMap<>();
  private final Context mContext;
  private boolean mGrid;

  public void setGrid(boolean grid)
  {
    mGrid = grid;
    notifyDataSetChanged();
  }

  public HikeRecommendationAdapter(Context context, List<HikeRecommendation> items, Consumer<HikeRecommendation> onOpen)
  {
    mContext = context;
    mItems = items;
    mOnOpen = onOpen;
    // Only local, optimized assets. No image URL or networking in the presentation layer.
    for (HikeRecommendation item : items)
    {
      DestinationBookmarks.migrate(context, item);
      if (item.imageAsset.isEmpty())
        continue;
      try (InputStream input = context.getAssets().open(item.imageAsset))
      {
        final Bitmap bitmap = BitmapFactory.decodeStream(input);
        if (bitmap != null)
          mPhotos.put(item.id, bitmap);
      }
      catch (IOException ignored)
      {
        // Missing photographs are disclosed in HOME; never substitute a fake photograph.
      }
    }
  }

  @NonNull
  @Override
  public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type)
  {
    final View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.areamap_hike_card, parent, false);
    card.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline)
      {
        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(),
                             view.getResources().getDimension(R.dimen.home_card_radius));
      }
    });
    card.setClipToOutline(true);
    return new Holder(card);
  }

  @Override
  public void onBindViewHolder(@NonNull Holder holder, int position)
  {
    final HikeRecommendation item = mItems.get(position);
    final String title = mContext.getString(item.title);
    holder.title.setText(title);
    holder.altitude.setText(item.altitude >= 0 ? mContext.getString(R.string.home_altitude, item.altitude) : "");
    holder.altitude.setVisibility(item.altitude >= 0 ? View.VISIBLE : View.GONE);
    holder.details.setText(mContext.getString(item.type));
    final Bitmap photo = mPhotos.get(item.id);
    for (TextView label : new TextView[] {holder.title, holder.altitude, holder.details})
      label.setTextColor(androidx.core.content.ContextCompat.getColor(mContext, R.color.home_photo_text));
    if (photo == null)
      holder.photo.setImageResource(R.drawable.destination_artwork);
    else
      holder.photo.setImageBitmap(photo);
    holder.photo.setVisibility(View.VISIBLE);
    final ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
    params.width = mGrid ? ViewGroup.LayoutParams.MATCH_PARENT
                         : mContext.getResources().getDimensionPixelSize(R.dimen.home_card_width);
    params.height = Math.round(mContext.getResources().getDimensionPixelSize(R.dimen.home_card_height)
                               * Math.max(1f, mContext.getResources().getConfiguration().fontScale));
    if (params instanceof ViewGroup.MarginLayoutParams margins)
      margins.bottomMargin = mGrid ? mContext.getResources().getDimensionPixelSize(R.dimen.home_gap) : 0;
    holder.itemView.setLayoutParams(params);
    holder.itemView.setContentDescription(title);
    holder.itemView.setOnClickListener(v -> mOnOpen.accept(item));
    updateFavorite(holder, item, title);
    holder.favorite.setOnClickListener(v -> {
      DestinationBookmarks.toggle(mContext, item);
      updateFavorite(holder, item, title);
    });
  }

  private void updateFavorite(Holder holder, HikeRecommendation item, String title)
  {
    final boolean saved = DestinationBookmarks.find(item) != null;
    holder.favorite.setColorFilter(androidx.core.content.ContextCompat.getColor(
        mContext, saved ? R.color.areamap_green : R.color.home_photo_text));
    holder.favorite.setContentDescription(
        mContext.getString(saved ? R.string.home_unfavorite : R.string.home_favorite, title));
    holder.favorite.setSelected(saved);
  }

  @Override
  public int getItemCount()
  {
    return mItems.size();
  }

  static final class Holder extends RecyclerView.ViewHolder
  {
    final ImageView photo;
    final ImageButton favorite;
    final TextView title;
    final TextView altitude;
    final TextView details;

    Holder(View view)
    {
      super(view);
      photo = view.findViewById(R.id.hike_photo);
      favorite = view.findViewById(R.id.hike_favorite);
      title = view.findViewById(R.id.hike_title);
      altitude = view.findViewById(R.id.hike_altitude);
      details = view.findViewById(R.id.hike_details);
      final View labels = (View) title.getParent();
      labels.addOnLayoutChangeListener((v, l, t, r, b, oldL, oldT, oldR, oldB) -> {
        final View gradient = view.findViewById(R.id.hike_gradient);
        final ViewGroup.LayoutParams params = gradient.getLayoutParams();
        final int height = b - t + view.getResources().getDimensionPixelSize(R.dimen.home_gap);
        if (params.height != height)
        {
          params.height = height;
          gradient.setLayoutParams(params);
        }
      });
    }
  }
}
