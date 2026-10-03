package app.organicmaps.home;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.maplayer.MapButtonsController;
import app.organicmaps.sdk.Framework;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import java.util.HashMap;
import java.util.Map;

/** Floating HOME surfaces. Search, place selection and routing retain their existing controllers. */
public final class AreaMapHomeController
{
  private static final String SAVED_STATE = "areamap_home_sheet_state";
  private final MwmActivity mActivity;
  private final View mHeader;
  private final View mControls;
  private final View mInfo;
  private final View mSheet;
  private final View mRoot;
  private final BottomSheetBehavior<View> mBehavior;
  private final Map<View, Integer> mHiddenLegacyViews = new HashMap<>();
  private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = this::updateLayout;
  private Insets mInsets = Insets.NONE;
  private boolean mVisible;

  public AreaMapHomeController(MwmActivity activity, Bundle state, Runnable openSettings, Runnable openMaps)
  {
    mActivity = activity;
    mRoot = activity.findViewById(R.id.coordinator);
    mHeader = activity.findViewById(R.id.home_header);
    mControls = activity.findViewById(R.id.home_controls);
    mInfo = activity.findViewById(R.id.home_info);
    mSheet = activity.findViewById(R.id.home_sheet);
    mBehavior = BottomSheetBehavior.from(mSheet);
    mBehavior.setFitToContents(false);
    mBehavior.setHideable(false);
    mBehavior.setDraggable(true);
    mBehavior.setSkipCollapsed(false);
    mBehavior.setState(state != null && state.getInt(SAVED_STATE) == BottomSheetBehavior.STATE_EXPANDED
        ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
    activity.findViewById(R.id.home_search).setOnClickListener(v -> activity.showSearch(""));
    activity.findViewById(R.id.home_settings).setOnClickListener(v -> openSettings.run());
    activity.findViewById(R.id.home_layers).setOnClickListener(
        v -> activity.onMapButtonClick(MapButtonsController.MapButtons.toggleMapLayer));
    activity.findViewById(R.id.home_location).setOnClickListener(
        v -> activity.onMapButtonClick(MapButtonsController.MapButtons.myPosition));
    activity.findViewById(R.id.home_offline_action).setOnClickListener(v -> openMaps.run());
    activity.findViewById(R.id.home_all).setOnClickListener(v -> {
      mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      activity.findViewById(R.id.home_recommendations).requestFocus();
    });
    activity.findViewById(R.id.home_handle).setOnClickListener(v -> mBehavior.setState(
        mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
            ? BottomSheetBehavior.STATE_COLLAPSED : BottomSheetBehavior.STATE_EXPANDED));
    final RecyclerView carousel = activity.findViewById(R.id.home_recommendations);
    carousel.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
    final HikeRecommendationAdapter adapter = new HikeRecommendationAdapter(activity,
        HikeRecommendation.demoCatalog(), item -> activity.showSearch(activity.getString(item.title)));
    carousel.setAdapter(adapter);
    activity.findViewById(R.id.home_photo_credits).setOnClickListener(v -> HomePhotoCredits.show(activity));
    activity.findViewById(R.id.home_photos_missing).setVisibility(adapter.hasMissingPhotos() ? View.VISIBLE : View.GONE);
    ((TextView) activity.findViewById(R.id.home_weather)).setText(HomeInfoRepository.demoWeather(activity));
    ((TextView) activity.findViewById(R.id.home_wind)).setText(HomeInfoRepository.demoWind(activity));
    mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View view, int state) { updateLayout(); }
      @Override
      public void onSlide(@NonNull View view, float offset) { updateLayout(); }
    });
    mRoot.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
  }

  public void setInsets(Insets insets)
  {
    mInsets = insets;
    updateLayout();
  }

  public void setVisible(boolean visible)
  {
    final boolean changed = mVisible != visible;
    mVisible = visible;
    for (View view : new View[] {mHeader, mControls, mSheet})
      view.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (!visible)
    {
      mInfo.setVisibility(View.GONE);
      for (Map.Entry<View, Integer> entry : mHiddenLegacyViews.entrySet())
        entry.getKey().setVisibility(entry.getValue());
      mHiddenLegacyViews.clear();
    }
    else if (changed)
      refreshInfo();
    mSheet.post(this::updateLayout);
  }

  public boolean isVisible() { return mVisible; }

  public void refreshInfo()
  {
    ((TextView) mActivity.findViewById(R.id.home_offline_status))
        .setText(HomeInfoRepository.offlineStatus(mActivity));
  }

  private int dimension(int id) { return mActivity.getResources().getDimensionPixelSize(id); }

  private void margins(View view, int left, int top, int right, int bottom)
  {
    final ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    if (p.leftMargin == left && p.topMargin == top && p.rightMargin == right && p.bottomMargin == bottom)
      return;
    p.setMargins(left, top, right, bottom);
    view.setLayoutParams(p);
  }

  private void updateLayout()
  {
    if (!mVisible || mRoot.getHeight() == 0)
      return;
    final int edge = dimension(R.dimen.home_edge);
    final int headerHeight = Math.round(dimension(R.dimen.home_header_height)
        * Math.max(1f, mActivity.getResources().getConfiguration().fontScale));
    if (mHeader.getLayoutParams().height != headerHeight)
    {
      mHeader.getLayoutParams().height = headerHeight;
      mHeader.requestLayout();
    }
    ((TextView) mActivity.findViewById(R.id.home_offline_status)).setMaxWidth(
        Math.max(1, mRoot.getWidth() - mInsets.left - mInsets.right - edge * 2
            - dimension(R.dimen.home_control) - dimension(R.dimen.home_padding) * 2));
    final View nav = mActivity.findViewById(R.id.areamap_bottom_nav);
    final int bottom = mInsets.bottom + Math.max(nav.getHeight(), dimension(R.dimen.home_nav_height));
    margins(mHeader, mInsets.left + edge, mInsets.top + edge, mInsets.right + edge, 0);
    margins(mControls, 0, mInsets.top + headerHeight + edge * 2, mInsets.right + edge, 0);
    margins(mSheet, mInsets.left, 0, mInsets.right, bottom);
    margins(mInfo, mInsets.left + edge, 0, mInsets.right + edge, 0);
    final int expandedTop = mInsets.top + headerHeight + edge * 2;
    mBehavior.setExpandedOffset(expandedTop);
    // Keep at least 38% of the usable map visible on short portrait screens; landscape uses a compact peek.
    final int usable = Math.max(1, mRoot.getHeight() - bottom - expandedTop);
    final int peek = HomeLayoutPolicy.peekHeight(dimension(R.dimen.home_peek), usable);
    if (mBehavior.getPeekHeight() != peek)
      mBehavior.setPeekHeight(peek);
    mBehavior.setHalfExpandedRatio(0.7f);
    final int infoTop = (int) mSheet.getY() - mInfo.getHeight() - edge;
    final boolean showInfo = infoTop > expandedTop + edge;
    if (mInfo.getVisibility() != (showInfo ? View.VISIBLE : View.INVISIBLE))
      mInfo.setVisibility(showInfo ? View.VISIBLE : View.INVISIBLE);
    mInfo.setTranslationY(infoTop);
    final View mapButtons = mActivity.findViewById(R.id.map_buttons);
    if (mapButtons != null)
    {
      // Preserve zoom and map gestures. Replace only controls represented by the new HOME surfaces.
      for (int id : new int[] {R.id.layers_button, R.id.my_position, R.id.btn_search, R.id.menu_button,
                               R.id.btn_bookmarks})
      {
        final View old = mapButtons.findViewById(id);
        if (old != null)
        {
          mHiddenLegacyViews.putIfAbsent(old, old.getVisibility());
          if (old.getVisibility() != View.INVISIBLE)
            old.setVisibility(View.INVISIBLE);
        }
      }
      margins(mapButtons, 0, 0, 0, Math.max(bottom, mRoot.getHeight() - (int) mSheet.getY()));
    }
    // Feed the existing map engine the unobstructed viewport rather than dimming or blocking the map.
    if (mSheet.getY() > expandedTop)
      Framework.nativeSetVisibleRect(mInsets.left, expandedTop, mRoot.getWidth() - mInsets.right,
                                     (int) mSheet.getY());
    // On narrow phones the logo identifies the app; give its text width back to the real search action.
    mActivity.findViewById(R.id.home_brand).setVisibility(
        mActivity.getResources().getConfiguration().screenWidthDp < 360 ? View.GONE : View.VISIBLE);
  }

  public void saveState(Bundle state)
  {
    state.putInt(SAVED_STATE, mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
        ? BottomSheetBehavior.STATE_EXPANDED : BottomSheetBehavior.STATE_COLLAPSED);
  }

  public void destroy()
  {
    if (mRoot.getViewTreeObserver().isAlive())
      mRoot.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
  }
}
