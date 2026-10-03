package app.organicmaps.home;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import app.organicmaps.MwmActivity;
import app.organicmaps.R;
import app.organicmaps.maplayer.MapButtonsController;
import app.organicmaps.routing.RoutingPlanViewModel;
import app.organicmaps.sdk.Framework;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

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
  private final ViewTreeObserver.OnGlobalLayoutListener mLayoutListener = this::updateLayout;
  private final HikeRecommendationAdapter mRecommendations;
  private Insets mInsets = Insets.NONE;
  private RecyclerView mCarousel;
  private boolean mGrid;
  private Parcelable mCarouselState;
  private Parcelable mGridState;
  private boolean mVisible;
  private boolean mPreviewVisible;
  private int mExpandedTop = -1;
  private int mSheetMaxHeight = -1;
  private float mHalfExpandedRatio = -1;

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
                           ? BottomSheetBehavior.STATE_EXPANDED
                           : BottomSheetBehavior.STATE_COLLAPSED);
    activity.findViewById(R.id.home_search).setOnClickListener(v -> activity.showSearch(""));
    activity.findViewById(R.id.home_settings).setOnClickListener(v -> openSettings.run());
    activity.findViewById(R.id.home_layers)
        .setOnClickListener(v -> activity.onMapButtonClick(MapButtonsController.MapButtons.toggleMapLayer));
    activity.findViewById(R.id.home_location)
        .setOnClickListener(v -> activity.onMapButtonClick(MapButtonsController.MapButtons.myPosition));
    activity.findViewById(R.id.home_offline_action).setOnClickListener(v -> openMaps.run());
    activity.findViewById(R.id.home_all).setOnClickListener(v -> {
      setGrid(!mGrid);
      mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
      mCarousel.requestFocus();
    });
    activity.findViewById(R.id.home_handle)
        .setOnClickListener(v
                            -> mBehavior.setState(mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                                                      ? BottomSheetBehavior.STATE_COLLAPSED
                                                      : BottomSheetBehavior.STATE_EXPANDED));
    final RecyclerView carousel = activity.findViewById(R.id.home_recommendations);
    carousel.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false));
    final HikeRecommendationAdapter adapter = new HikeRecommendationAdapter(
        activity, HikeRecommendation.catalog(), item -> DestinationDetailsFragment.open(activity, item.id));
    mRecommendations = adapter;
    carousel.setAdapter(adapter);
    mCarousel = carousel;
    if (state != null)
    {
      setGrid(state.getBoolean("destination_all"));
      mCarouselState = state.getParcelable("destination_carousel");
      mGridState = state.getParcelable("destination_grid");
      final Parcelable restored = mGrid ? mGridState : mCarouselState;
      if (restored != null)
        carousel.getLayoutManager().onRestoreInstanceState(restored);
    }
    activity.findViewById(R.id.home_photo_credits).setOnClickListener(v -> HomePhotoCredits.show(activity));
    mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
      @Override
      public void onStateChanged(@NonNull View view, int state)
      {
        if (state == BottomSheetBehavior.STATE_COLLAPSED && mGrid)
          setGrid(false);
        updateLayout();
      }
      @Override
      public void onSlide(@NonNull View view, float offset)
      {
        updateLayout();
      }
    });
    mRoot.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
    new ViewModelProvider(activity).get(RoutingPlanViewModel.class).getRoutingBottomDistanceToTop()
        .observe(activity, top -> {
          if (mPreviewVisible)
            updateLayout();
        });
  }

  private void setGrid(boolean grid)
  {
    if (mGrid == grid)
      return;
    final Parcelable previous = mCarousel.getLayoutManager().onSaveInstanceState();
    if (mGrid)
      mGridState = previous;
    else
      mCarouselState = previous;
    mGrid = grid;
    final int width = mActivity.getResources().getConfiguration().screenWidthDp;
    mCarousel.setLayoutManager(grid ? new GridLayoutManager(mActivity, width >= 600   ? 3
                                                                       : width >= 340 ? 2
                                                                                      : 1)
                                    : new LinearLayoutManager(mActivity, RecyclerView.HORIZONTAL, false));
    mRecommendations.setGrid(grid);
    final Parcelable restored = grid ? mGridState : mCarouselState;
    if (restored != null)
      mCarousel.getLayoutManager().onRestoreInstanceState(restored);
    ((TextView) mActivity.findViewById(R.id.home_all))
        .setText(grid ? R.string.home_carousel : R.string.home_all_places);
  }

  public void setInsets(Insets insets)
  {
    mInsets = insets;
    updateLayout();
  }

  public void setPresentation(boolean visible, boolean previewVisible)
  {
    final boolean wasPresented = mVisible || mPreviewVisible;
    mPreviewVisible = previewVisible;
    final boolean changed = mVisible != visible;
    mVisible = visible;
    for (View view : new View[] {mHeader, mControls})
      view.setVisibility(visible || previewVisible ? View.VISIBLE : View.GONE);
    if ((visible || previewVisible) && !wasPresented)
    {
      bindMapButtons(mActivity.findViewById(R.id.home_zoom_slot), false);
      mControls.setVisibility(View.INVISIBLE);
    }
    mSheet.setVisibility(visible ? View.VISIBLE : View.GONE);
    if (!visible)
    {
      mInfo.setVisibility(View.GONE);
      if (!previewVisible)
      {
        bindMapButtons(null, false);
        if (wasPresented)
        {
          mActivity.updateCompassOffset(mInsets.top
              + (app.organicmaps.sdk.location.TrackRecorder.nativeIsTrackRecordingEnabled()
                     ? dimension(R.dimen.map_button_size) : 0), mInsets.right);
          mActivity.updateBottomWidgetsOffset(mInsets.left);
        }
      }
    }
    else if (changed)
      refreshInfo();
    mSheet.post(this::updateLayout);
  }

  public boolean isVisible()
  {
    return mVisible;
  }

  public void refreshInfo()
  {
    mRecommendations.notifyDataSetChanged();
    ((TextView) mActivity.findViewById(R.id.home_offline_status)).setText(HomeInfoRepository.offlineStatus(mActivity));
  }

  private int dimension(int id)
  {
    return mActivity.getResources().getDimensionPixelSize(id);
  }

  private void margins(View view, int left, int top, int right, int bottom)
  {
    final ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
    if (p.leftMargin == left && p.topMargin == top && p.rightMargin == right && p.bottomMargin == bottom)
      return;
    p.setMargins(left, top, right, bottom);
    view.setLayoutParams(p);
  }

  private void bindMapButtons(LinearLayout slot, boolean horizontal)
  {
    final var fragment = mActivity.getSupportFragmentManager().findFragmentById(R.id.map_buttons);
    if (fragment instanceof MapButtonsController buttons && buttons.getView() != null)
      buttons.setHomePresentation(slot, horizontal);
  }

  private int topInRoot(View view)
  {
    final int[] root = new int[2];
    final int[] position = new int[2];
    mRoot.getLocationInWindow(root);
    view.getLocationInWindow(position);
    return position[1] - root[1];
  }

  private void updateBrand()
  {
    final TextView brand = mActivity.findViewById(R.id.home_brand);
    final View search = mActivity.findViewById(R.id.home_search);
    final ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) brand.getLayoutParams();
    final int brandWidth = (int) Math.ceil(brand.getPaint().measureText(brand.getText().toString()))
                        + brand.getPaddingLeft() + brand.getPaddingRight() + p.getMarginStart() + p.getMarginEnd();
    final int available = search.getWidth()
                        + (brand.getVisibility() == View.VISIBLE ? brand.getWidth() + p.getMarginStart()
                                                                     + p.getMarginEnd() : 0);
    final int visibility = available >= brandWidth + dimension(R.dimen.home_search_min_width)
                               ? View.VISIBLE : View.GONE;
    if (brand.getVisibility() != visibility)
      brand.setVisibility(visibility);
  }

  private void controlSpacing(View view, boolean horizontal)
  {
    final LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) view.getLayoutParams();
    final int gap = dimension(R.dimen.home_control_gap);
    final int top = horizontal ? 0 : gap;
    final int start = horizontal ? gap : 0;
    if (p.topMargin != top || p.getMarginStart() != start)
    {
      p.topMargin = top;
      p.setMarginStart(start);
      view.setLayoutParams(p);
    }
  }

  private void updateLayout()
  {
    if ((!mVisible && !mPreviewVisible) || mRoot.getHeight() == 0)
      return;
    final int edge = dimension(R.dimen.home_edge);
    final int size = dimension(R.dimen.home_control);
    final int gap = dimension(R.dimen.home_control_gap);
    final int headerHeight = Math.round(dimension(R.dimen.home_header_height)
                                        * Math.max(1f, mActivity.getResources().getConfiguration().fontScale));
    if (mHeader.getLayoutParams().height != headerHeight)
    {
      mHeader.getLayoutParams().height = headerHeight;
      mHeader.requestLayout();
    }
    margins(mHeader, mInsets.left + edge, mInsets.top + edge, mInsets.right + edge, 0);
    if (mHeader.isLayoutRequested())
    {
      mControls.setVisibility(View.INVISIBLE);
      return;
    }
    updateBrand();
    final int mapTop = mHeader.getBottom() + edge;
    final View nav = mActivity.findViewById(R.id.areamap_bottom_nav);
    final int bottom = nav.getVisibility() == View.VISIBLE
                           ? (nav.getBottom() == mRoot.getHeight() && nav.getHeight() > 0
                                  ? mRoot.getHeight() - nav.getTop()
                                  : Math.max(nav.getHeight(), mInsets.bottom + dimension(R.dimen.home_nav_height)))
                           : mInsets.bottom;
    final int safeBottom = mRoot.getHeight() - bottom;
    final LinearLayout slot = mActivity.findViewById(R.id.home_zoom_slot);
    final LinearLayout controls = mActivity.findViewById(R.id.home_primary_controls);
    // Choose a row only when the complete column would leave no room for the sheet.
    final View recording = mActivity.findViewById(R.id.track_recording_status);
    final int count = (app.organicmaps.sdk.util.Config.showZoomButtons() ? 4 : 2)
                    + (recording != null && recording.getVisibility() == View.VISIBLE ? 1 : 0);
    final int columnHeight = count * size + (count - 1) * gap;
    final boolean horizontal = safeBottom - mapTop < columnHeight + size + edge * 2;
    final int orientation = horizontal ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
    if (controls.getOrientation() != orientation)
      controls.setOrientation(orientation);
    controlSpacing(mActivity.findViewById(R.id.home_location), horizontal);
    controlSpacing(slot, horizontal);
    controlSpacing(mActivity.findViewById(R.id.home_recording_slot), false);
    bindMapButtons(slot, horizontal);
    margins(mControls, mInsets.left + edge, mapTop, mInsets.right + edge, 0);
    if (mControls.isLayoutRequested())
      return;
    final int controlsBottom = mapTop + mControls.getHeight();
    mActivity.updateCompassOffset(mapTop, mInsets.right + mControls.getWidth() + edge);
    if (mPreviewVisible)
    {
      // A portrait routing sheet can cover the entire map; do not draw controls through it.
      final View sheet = mActivity.findViewById(R.id.routing_sheet_frame);
      final boolean landscape = mActivity.getResources().getConfiguration().orientation
                               == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
      final int limit = !landscape && sheet != null && sheet.getVisibility() == View.VISIBLE
                            ? Math.min(safeBottom, topInRoot(sheet)) : safeBottom;
      mControls.setVisibility(controlsBottom + edge <= limit ? View.VISIBLE : View.INVISIBLE);
      return; // RoutingPlanFragment owns its sheet and visible map rectangle.
    }
    mControls.setVisibility(View.VISIBLE);
    final int expandedTop = Math.min(safeBottom, controlsBottom + edge);
    if (expandedTop != mExpandedTop)
    {
      mExpandedTop = expandedTop;
      mBehavior.setExpandedOffset(Math.max(mapTop, expandedTop));
      mSheet.requestLayout();
    }
    // BottomSheetBehavior positions against the full parent, ignoring a bottom margin.
    // Include the dock once in peek/height, and reserve it as content padding instead.
    margins(mSheet, mInsets.left, 0, mInsets.right, 0);
    if (mSheet.getPaddingBottom() != bottom)
      mSheet.setPadding(mSheet.getPaddingLeft(), mSheet.getPaddingTop(), mSheet.getPaddingRight(), bottom);
    final int maxHeight = Math.max(1, mRoot.getHeight() - Math.max(mapTop, expandedTop));
    if (maxHeight != mSheetMaxHeight)
    {
      mSheetMaxHeight = maxHeight;
      mBehavior.setMaxHeight(maxHeight);
      mSheet.requestLayout();
    }
    final int usable = Math.max(1, safeBottom - mapTop);
    final int visiblePeek = Math.min(HomeLayoutPolicy.peekHeight(dimension(R.dimen.home_peek), usable),
                                    Math.max(1, safeBottom - Math.max(mapTop, expandedTop)));
    final int peek = visiblePeek + bottom;
    if (mBehavior.getPeekHeight() != peek)
      mBehavior.setPeekHeight(peek);
    final int collapsedTop = safeBottom - visiblePeek;
    final float halfRatio = Math.max(0.01f, Math.min(0.99f,
        1f - ((Math.max(mapTop, expandedTop) + collapsedTop) / 2f) / mRoot.getHeight()));
    if (mHalfExpandedRatio != halfRatio)
    {
      mHalfExpandedRatio = halfRatio;
      mBehavior.setHalfExpandedRatio(halfRatio);
      mSheet.requestLayout();
    }
    final int sheetTop = Math.min(safeBottom, (int) mSheet.getY());
    final View mapButtons = mActivity.findViewById(R.id.map_buttons);
    if (mapButtons != null)
      margins(mapButtons, 0, 0, 0, Math.max(bottom, mRoot.getHeight() - sheetTop));
    int infoBottom = sheetTop - edge;
    final View help = mActivity.findViewById(R.id.help_button);
    if (help != null && help.isShown())
      infoBottom = Math.min(infoBottom, topInRoot(help) - edge);
    final int infoTop = infoBottom - mInfo.getHeight();
    final int infoWidth = Math.max(1, mRoot.getWidth() - mInsets.left - mInsets.right - edge * 2
                                      - (horizontal ? 0 : mControls.getWidth() + edge));
    if (mInfo.getLayoutParams().width != infoWidth)
    {
      mInfo.getLayoutParams().width = infoWidth;
      mInfo.requestLayout();
    }
    ((TextView) mActivity.findViewById(R.id.home_offline_status))
        .setMaxWidth(Math.max(1, infoWidth - dimension(R.dimen.home_icon) - dimension(R.dimen.home_gap)
                                    - dimension(R.dimen.home_padding) * 2));
    margins(mInfo, mInsets.left + edge, Math.max(0, infoTop), mInsets.right + edge, 0);
    mInfo.setVisibility(infoTop >= (horizontal ? controlsBottom + edge : mapTop) ? View.VISIBLE : View.INVISIBLE);
    // A compact row spans the map centre, so the usable viewport begins below it.
    final int viewportTop = mControls.getWidth() > (mRoot.getWidth() - mInsets.left - mInsets.right) / 2
                                ? controlsBottom + edge : mapTop;
    if (sheetTop > viewportTop)
      Framework.nativeSetVisibleRect(mInsets.left, viewportTop, mRoot.getWidth() - mInsets.right, sheetTop);
  }

  public void saveState(Bundle state)
  {
    final Parcelable current = mCarousel.getLayoutManager().onSaveInstanceState();
    state.putParcelable("destination_carousel", mGrid ? mCarouselState : current);
    state.putParcelable("destination_grid", mGrid ? current : mGridState);
    state.putBoolean("destination_all", mGrid);
    state.putInt(SAVED_STATE, mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED
                                  ? BottomSheetBehavior.STATE_EXPANDED
                                  : BottomSheetBehavior.STATE_COLLAPSED);
  }

  public void destroy()
  {
    bindMapButtons(null, false);
    if (mRoot.getViewTreeObserver().isAlive())
      mRoot.getViewTreeObserver().removeOnGlobalLayoutListener(mLayoutListener);
  }
}
