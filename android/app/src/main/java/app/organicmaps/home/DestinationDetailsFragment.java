package app.organicmaps.home;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.safety.TripPlan;
import app.organicmaps.safety.TripWeatherNotifier;
import app.organicmaps.safety.TripWeatherRepository;
import app.organicmaps.util.SharingUtils;
import app.organicmaps.util.Utils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** HOME-owned detail presentation; routing, saved places and forecast remain in existing systems. */
public final class DestinationDetailsFragment extends BottomSheetDialogFragment
{
  private static final String TAG = "areamap.destination.details";
  private static final ExecutorService WEATHER = Executors.newSingleThreadExecutor();
  private final Handler mMain = new Handler(Looper.getMainLooper());
  private HikeRecommendation mItem;
  private View mRoot;
  private boolean mLoading;
  private boolean mDescriptionExpanded;
  private int mPage;
  private Bitmap mRoutePhoto;

  public static void open(MwmActivity activity, String id)
  {
    if (activity.getSupportFragmentManager().findFragmentByTag(TAG) != null)
      return;
    final DestinationDetailsFragment fragment = new DestinationDetailsFragment();
    final Bundle args = new Bundle();
    args.putString("destination_id", id);
    fragment.setArguments(args);
    fragment.show(activity.getSupportFragmentManager(), TAG);
  }

  @Override
  public void onCreate(@Nullable Bundle state)
  {
    super.onCreate(state);
    mItem = HikeRecommendation.find(requireArguments().getString("destination_id", ""));
    if (state != null)
    {
      mDescriptionExpanded = state.getBoolean("description_expanded");
      mPage = state.getInt("gallery_page");
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state)
  {
    return inflater.inflate(R.layout.areamap_destination_details, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View root, @Nullable Bundle state)
  {
    super.onViewCreated(root, state);
    mRoot = root;
    if (mItem == null)
    {
      dismiss();
      return;
    }
    ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
      final Insets bars =
          insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
      return insets;
    });
    ViewCompat.requestApplyInsets(root);
    text(R.id.destination_title, getString(mItem.title));
    text(R.id.destination_type, getString(mItem.type));
    root.findViewById(R.id.destination_back).setOnClickListener(v -> dismiss());
    root.findViewById(R.id.destination_share)
        .setOnClickListener(
            v -> SharingUtils.shareDestination(requireContext(), getString(mItem.title), mItem.lat, mItem.lon));
    DestinationBookmarks.migrate(requireContext(), mItem);
    updateFavorite();
    root.findViewById(R.id.destination_favorite).setOnClickListener(v -> {
      DestinationBookmarks.toggle(requireContext(), mItem);
      updateFavorite();
    });
    gallery();
    final View hero = root.findViewById(R.id.destination_hero);
    hero.addOnLayoutChangeListener((view, l, t, r, b, oldL, oldT, oldR, oldB) -> {
      final int height = Math.round((r - l) * 10f / 16f);
      if (height > 0 && view.getLayoutParams().height != height)
      {
        view.getLayoutParams().height = height;
        view.requestLayout();
      }
    });
    text(R.id.destination_source, getString(R.string.destination_source, mItem.sourceName));
    root.findViewById(R.id.destination_source)
        .setOnClickListener(v -> Utils.openUrl(requireContext(), mItem.sourceUrl));
    final LinearLayout info = root.findViewById(R.id.destination_info);
    if (mItem.altitude >= 0)
    {
      final TextView altitude = new TextView(requireContext());
      altitude.setText(getString(R.string.destination_altitude_label, mItem.altitude));
      altitude.setTextSize(16);
      altitude.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.areamap_text));

      final int p = getResources().getDimensionPixelSize(R.dimen.home_padding);
      altitude.setPadding(0, p / 2, p, p / 2);
      info.addView(altitude);
    }
    text(R.id.destination_description, mItem.description == 0 ? "" : getString(mItem.description));
    root.findViewById(R.id.destination_description_heading)
        .setVisibility(mItem.description == 0 ? View.GONE : View.VISIBLE);
    root.findViewById(R.id.destination_description).setVisibility(mItem.description == 0 ? View.GONE : View.VISIBLE);
    ((TextView) root.findViewById(R.id.destination_description))
        .setMaxLines(mDescriptionExpanded ? Integer.MAX_VALUE : 4);
    root.findViewById(R.id.destination_description_expand)
        .setVisibility(mItem.description == 0 ? View.GONE : View.VISIBLE);
    text(R.id.destination_description_expand,
         getString(mDescriptionExpanded ? R.string.destination_read_less : R.string.destination_read_more));
    root.findViewById(R.id.destination_description_expand).setOnClickListener(v -> {
      mDescriptionExpanded = !mDescriptionExpanded;
      ((TextView) root.findViewById(R.id.destination_description))
          .setMaxLines(mDescriptionExpanded ? Integer.MAX_VALUE : 4);
      text(R.id.destination_description_expand,
           getString(mDescriptionExpanded ? R.string.destination_read_less : R.string.destination_read_more));
    });
    text(R.id.destination_location, getString(R.string.destination_location_value, mItem.lat, mItem.lon));
    root.findViewById(R.id.destination_location).setOnClickListener(v -> {
      final MwmActivity activity = (MwmActivity) requireActivity();
      dismiss();
      activity.showDestinationOnMap(mItem.lat, mItem.lon);
    });
    root.findViewById(R.id.destination_route).setOnClickListener(v -> {
      final MwmActivity activity = (MwmActivity) requireActivity();
      if (activity.buildDestinationRoute(mItem))
        dismiss();
    });
    root.findViewById(R.id.destination_weather_refresh).setOnClickListener(v -> loadWeather(true));
    relatedRoutes();
    renderWeather();
    loadWeather(false);
  }

  @Override
  public void onStart()
  {
    super.onStart();
    if (mRoot == null)
      return;
    final android.view.Window window = requireDialog().getWindow();
    if (window != null)
    {
      androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);
      final androidx.core.view.WindowInsetsControllerCompat controller =
          androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView());
      final boolean light = !app.organicmaps.util.ThemeUtils.isDarkTheme(requireContext());
      controller.setAppearanceLightStatusBars(light);
      controller.setAppearanceLightNavigationBars(light);
      ViewCompat.requestApplyInsets(mRoot);
    }
    final View sheet = (View) mRoot.getParent();
    sheet.getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
    sheet.requestLayout();
    final BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
    behavior.setSkipCollapsed(true);
    behavior.setDraggable(false);
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
  }

  private void text(int id, String value)
  {
    ((TextView) mRoot.findViewById(id)).setText(value);
  }

  private void updateFavorite()
  {
    final boolean saved = DestinationBookmarks.find(mItem) != null;
    final android.widget.ImageButton favorite = mRoot.findViewById(R.id.destination_favorite);
    favorite.setSelected(saved);
    favorite.setColorFilter(androidx.core.content.ContextCompat.getColor(
        requireContext(), saved ? R.color.areamap_green : R.color.home_photo_text));
    favorite.setContentDescription(
        getString(saved ? R.string.home_unfavorite : R.string.home_favorite, getString(mItem.title)));
  }

  private void gallery()
  {
    final List<Bitmap> photos = new ArrayList<>();
    for (String path : mItem.imageAssets)
      try (InputStream input = requireContext().getAssets().open(path))
      {
        final Bitmap photo = BitmapFactory.decodeStream(input);
        if (photo != null)
          photos.add(photo);
      }
      catch (IOException ignored)
      { /* Use the local artwork if decoding fails. */
      }
    mRoutePhoto = photos.isEmpty() ? null : photos.get(0);
    text(R.id.destination_photo_caption,
         getString(mItem.photoShowsApproach ? R.string.home_photo_approach : R.string.home_photo_credits));
    mRoot.findViewById(R.id.destination_photo_caption).setVisibility(photos.isEmpty() ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.destination_photo_caption).setOnClickListener(v -> HomePhotoCredits.show(requireContext()));
    mRoot.findViewById(R.id.destination_artwork).setVisibility(photos.isEmpty() ? View.VISIBLE : View.GONE);
    final RecyclerView gallery = mRoot.findViewById(R.id.destination_gallery);
    final LinearLayoutManager manager = new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false);
    gallery.setLayoutManager(manager);
    gallery.setAdapter(new RecyclerView.Adapter<PhotoHolder>() {
      @NonNull
      @Override
      public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int type)
      {
        final ImageView image = new ImageView(parent.getContext());
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return new PhotoHolder(image);
      }
      @Override
      public void onBindViewHolder(@NonNull PhotoHolder holder, int position)
      {
        holder.image.setLayoutParams(
            new RecyclerView.LayoutParams(Math.max(1, gallery.getWidth()), ViewGroup.LayoutParams.MATCH_PARENT));
        holder.image.setImageBitmap(photos.get(position));
      }
      @Override
      public int getItemCount()
      {
        return photos.size();
      }
    });
    final PagerSnapHelper snap = new PagerSnapHelper();
    snap.attachToRecyclerView(gallery);
    gallery.addOnLayoutChangeListener((v, l, t, right, b, oldL, oldT, oldR, oldB) -> {
      if (right - l != oldR - oldL)
        gallery.post(() -> {
          if (gallery.getAdapter() != null)
            gallery.getAdapter().notifyDataSetChanged();
        });
    });
    gallery.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrollStateChanged(@NonNull RecyclerView recycler, int state)
      {
        final View page = snap.findSnapView(manager);
        if (page == null)
          return;
        mPage = manager.getPosition(page);
        text(R.id.destination_page_indicator, (mPage + 1) + " / " + photos.size());
      }
    });
    mRoot.findViewById(R.id.destination_page_indicator).setVisibility(photos.size() > 1 ? View.VISIBLE : View.GONE);
    if (!photos.isEmpty())
      gallery.scrollToPosition(Math.min(mPage, photos.size() - 1));
  }

  private void relatedRoutes()
  {
    final TripPlan plan = TripPlan.current();
    boolean matches = false;
    if (plan != null)
      for (TripPlan.Checkpoint point : plan.routeCheckpoints)
        if (DestinationRouteMatch.near(mItem.lat, mItem.lon, point.lat, point.lon))
        {
          matches = true;
          break;
        }
    final boolean hasRoutes = matches || !mItem.relatedRoutes.isEmpty();
    mRoot.findViewById(R.id.destination_related_empty).setVisibility(hasRoutes ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.destination_related_scroll).setVisibility(hasRoutes ? View.VISIBLE : View.GONE);
    final LinearLayout container = mRoot.findViewById(R.id.destination_related);
    for (RelatedRoute source : mItem.relatedRoutes)
    {
      final LinearLayout sourceCard = new LinearLayout(requireContext());
      sourceCard.setOrientation(LinearLayout.VERTICAL);
      sourceCard.setBackgroundResource(R.drawable.destination_card);
      final int p = getResources().getDimensionPixelSize(R.dimen.home_padding);
      sourceCard.setPadding(p, p, p, p);
      final LinearLayout.LayoutParams params =
          new LinearLayout.LayoutParams(p * 18, ViewGroup.LayoutParams.WRAP_CONTENT);
      params.setMarginEnd(p / 2);
      sourceCard.setLayoutParams(params);
      final ImageView photo = new ImageView(requireContext());
      photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
      if (mRoutePhoto == null)
        photo.setImageResource(R.drawable.destination_artwork);
      else
        photo.setImageBitmap(mRoutePhoto);
      photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      sourceCard.addView(photo, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, p * 8));
      addRouteLabel(sourceCard, getString(source.title), 18);
      if (source.sourceDistanceKm > 0)
      {
        addRouteLabel(sourceCard, getString(R.string.destination_related_source), 14);
        addRouteLabel(sourceCard,
                      getString(R.string.destination_source_figures, source.sourceDistanceKm,
                                getString(source.sourceDuration), source.sourceDifficulty),
                      14);
        if (source.sourceAscent > 0)
          addRouteLabel(sourceCard, getString(R.string.destination_source_ascent, source.sourceAscent), 14);
      }
      addRouteLabel(sourceCard, getString(R.string.destination_route_variant), 14);
      sourceCard.setClickable(true);
      sourceCard.setFocusable(true);
      sourceCard.setOnClickListener(v -> {
        final MwmActivity activity = (MwmActivity) requireActivity();
        if (activity.buildRelatedDestinationRoute(source))
          dismiss();
      });
      container.addView(sourceCard);
    }
    if (!matches)
      return;
    final LinearLayout card = new LinearLayout(requireContext());
    card.setOrientation(LinearLayout.VERTICAL);
    card.setBackgroundResource(R.drawable.destination_card);
    final int padding = getResources().getDimensionPixelSize(R.dimen.home_edge);
    card.setPadding(padding, padding, padding, padding);
    card.setLayoutParams(new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.home_card_width),
                                                       ViewGroup.LayoutParams.WRAP_CONTENT));
    if (mRoutePhoto != null)
    {
      final ImageView photo = new ImageView(requireContext());
      photo.setImageBitmap(mRoutePhoto);
      photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
      photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      card.addView(photo,
                   new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                                 getResources().getDimensionPixelSize(R.dimen.home_card_height) / 2));
    }
    final TextView route = new TextView(requireContext());
    route.setText(getString(R.string.destination_route_current, plan.startTitle, plan.finishTitle,
                            plan.distanceMeters / 1000, plan.plannedSeconds / 60));
    route.setTextSize(16);
    route.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.areamap_text));
    card.addView(route);
    card.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.home_control));
    card.setFocusable(true);
    card.setContentDescription(route.getText());
    card.setOnClickListener(v -> {
      final MwmActivity activity = (MwmActivity) requireActivity();
      dismiss();
      activity.showAreaMapRoutePanel(plan);
    });
    ((LinearLayout) mRoot.findViewById(R.id.destination_related)).addView(card);
  }

  private void addRouteLabel(LinearLayout card, String value, int size)
  {
    final TextView label = new TextView(requireContext());
    label.setText(value);
    label.setTextSize(size);
    label.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.areamap_text));
    label.setPadding(0, getResources().getDimensionPixelSize(R.dimen.home_gap), 0, 0);
    card.addView(label);
  }

  private void renderWeather()
  {
    if (mRoot == null || mItem == null)
      return;
    final List<TripWeatherRepository.Hour> hours =
        TripWeatherRepository.destinationHours(requireContext(), mItem.id, mItem.lat, mItem.lon, mItem.altitude);
    if (hours.isEmpty())
      text(R.id.destination_weather,
           getString(mLoading ? R.string.destination_weather_loading : R.string.destination_weather_missing));
    else
    {
      final TripWeatherRepository.Hour hour = hours.get(0);
      text(R.id.destination_weather, getString(R.string.destination_weather_value, hour.temperatureC,
                                               TripWeatherNotifier.weatherLabel(requireContext(), hour.weatherCode)));
      final long fetched = TripWeatherRepository.destinationFetchedAt(requireContext(), mItem.id);
      text(R.id.destination_weather_age, getString(R.string.destination_weather_updated,
                                                   DateUtils.getRelativeTimeSpanString(
                                                       fetched, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
                                                       DateUtils.FORMAT_ABBREV_RELATIVE)));
      ((TextView) mRoot.findViewById(R.id.destination_weather))
          .setCompoundDrawablesWithIntrinsicBounds(hour.weatherCode == 0    ? R.drawable.ic_preview_sun
                                                   : hour.weatherCode <= 48 ? R.drawable.ic_preview_cloud
                                                                            : R.drawable.ic_preview_rain,
                                                   0, 0, 0);
    }
    mRoot.findViewById(R.id.destination_weather_age).setVisibility(hours.isEmpty() ? View.GONE : View.VISIBLE);
    mRoot.findViewById(R.id.destination_weather_refresh).setEnabled(!mLoading);
  }

  private void loadWeather(boolean force)
  {
    if (mLoading || mItem == null)
      return;
    mLoading = true;
    renderWeather();
    final Context context = requireContext().getApplicationContext();
    final HikeRecommendation item = mItem;
    WEATHER.execute(() -> {
      TripWeatherRepository.refreshDestination(context, item.id, item.lat, item.lon, item.altitude, force);
      mMain.post(() -> {
        mLoading = false;
        if (isAdded() && mRoot != null)
          renderWeather();
      });
    });
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle state)
  {
    super.onSaveInstanceState(state);
    state.putBoolean("description_expanded", mDescriptionExpanded);
    state.putInt("gallery_page", mPage);
  }

  @Override
  public void onDismiss(@NonNull DialogInterface dialog)
  {
    super.onDismiss(dialog);
    if (getActivity() instanceof MwmActivity activity)
      activity.refreshDestinationFavorites();
  }

  @Override
  public void onDestroyView()
  {
    mRoot = null;
    mRoutePhoto = null;
    super.onDestroyView();
  }

  private static final class PhotoHolder extends RecyclerView.ViewHolder
  {
    final ImageView image;
    PhotoHolder(ImageView image)
    {
      super(image);
      this.image = image;
    }
  }
}
