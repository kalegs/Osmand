package net.osmand.plus.dashboard;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.audionotes.DashAudioVideoNotesFragment;
import net.osmand.plus.helpers.ScreenOrientationHelper;
import net.osmand.plus.monitoring.DashTrackFragment;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.views.controls.FloatingActionButton;
import android.content.Intent;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ScrollView;

/**
 * Created by Denis
 * on 03.03.15.
 */
public class DashboardOnMap {


	private static final int LIST_ID = 1;
	private static final int DIRECTIONS_ID = 2;
	private static final int CONFIGURE_SCREEN_ID = 3;
	private static final int SETTINGS_ID = 4;
	private MapActivity mapActivity;
	FloatingActionButton fabButton;
	boolean floatingButtonVisible = true;
	private FrameLayout dashboardView;
	private boolean visible = false;
	private boolean landscape;
	private List<WeakReference<DashBaseFragment>> fragList = new LinkedList<WeakReference<DashBaseFragment>>();


	public DashboardOnMap(MapActivity ma) {
		this.mapActivity = ma;
	}


	public void createDashboardView() {
		landscape = !ScreenOrientationHelper.isOrientationPortrait(mapActivity);
		dashboardView = (FrameLayout) mapActivity.getLayoutInflater().inflate(R.layout.dashboard_over_map, null, false);
		dashboardView.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT));
		View.OnClickListener listener = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				setDashboardVisibility(false);
			}
		};
		dashboardView.findViewById(R.id.content).setOnClickListener(listener);
		dashboardView.setOnClickListener(listener);
		((FrameLayout) mapActivity.findViewById(R.id.ParentLayout)).addView(dashboardView);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			fabButton = new FloatingActionButton.Builder(mapActivity)
					.withDrawable(mapActivity.getResources().getDrawable(R.drawable.ic_action_map))
					.withButtonColor(mapActivity.getResources().getColor(R.color.color_myloc_distance))
					.withGravity(landscape ? Gravity.BOTTOM | Gravity.RIGHT : Gravity.TOP | Gravity.RIGHT)
					.withMargins(0, landscape ? 0 : 160, 16, landscape ? 16 : 0).create();
			fabButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					if (getMyApplication().accessibilityEnabled()) {
						mapActivity.getMapActions().whereAmIDialog();
					} else {
						mapActivity.getMapViewTrackingUtilities().backToLocationImpl();
					}
					setDashboardVisibility(false);
				}
			});
			fabButton.hideFloatingActionButton();
		}

		if (ScreenOrientationHelper.isOrientationPortrait(mapActivity)) {
			((NotifyingScrollView) dashboardView.findViewById(R.id.main_scroll))
					.setOnScrollChangedListener(onScrollChangedListener);
		}

	}


	protected OsmandApplication getMyApplication() {
		return mapActivity.getMyApplication();
	}


	public void setDashboardVisibility(boolean visible) {
		this.visible = visible;
		if (visible) {
			addOrUpdateDashboardFragments();
			setupActionBar();
			dashboardView.setVisibility(View.VISIBLE);
			fabButton.showFloatingActionButton();
			open(dashboardView.findViewById(R.id.animateContent));
			mapActivity.getMapActions().disableDrawer();
			mapActivity.findViewById(R.id.MapInfoControls).setVisibility(View.GONE);
			mapActivity.findViewById(R.id.MapButtons).setVisibility(View.GONE);
		} else {
			mapActivity.getMapActions().enableDrawer();
			hide(dashboardView.findViewById(R.id.animateContent));
			mapActivity.findViewById(R.id.MapInfoControls).setVisibility(View.VISIBLE);
			mapActivity.findViewById(R.id.MapButtons).setVisibility(View.VISIBLE);
			fabButton.hideFloatingActionButton();
		}
	}

	private void setupActionBar() {
		final Toolbar tb = (Toolbar) mapActivity.findViewById(R.id.bottomControls);
		tb.setTitle(null);
		tb.getMenu().clear();
		Menu menu = tb.getMenu();
		createMenuItem(menu, LIST_ID, R.string.drawer, 
				R.drawable.ic_dashboard_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, DIRECTIONS_ID, R.string.get_directions, 
				R.drawable.ic_action_gdirections_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, CONFIGURE_SCREEN_ID, R.string.layer_map_appearance,
				R.drawable.ic_configure_screen_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		createMenuItem(menu, SETTINGS_ID, R.string.settings_activity, 
				R.drawable.ic_action_settings_enabled_dark, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
	
	public MenuItem createMenuItem(Menu m, int id, int titleRes, int icon, int menuItemType) {
		int r = icon;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		MenuItemCompat.setShowAsAction(menuItem, menuItemType);
		return menuItem;
	}


	protected boolean onOptionsItemSelected(MenuItem item) {
		setDashboardVisibility(false);
		if(item.getItemId() == LIST_ID) {
			getMyApplication().getSettings().USE_DASHBOARD_INSTEAD_OF_DRAWER.set(false);
			mapActivity.getMapActions().toggleDrawer();
		} else if(item.getItemId() == DIRECTIONS_ID) {
			RoutingHelper routingHelper = mapActivity.getRoutingHelper();
			if(!routingHelper.isFollowingMode() && !routingHelper.isRoutePlanningMode()) {
				mapActivity.getMapActions().enterRoutePlanningMode(null, null, false);
			} else {
				mapActivity.getMapViewTrackingUtilities().switchToRoutePlanningMode();
				mapActivity.refreshMap();
			}
		} else if(item.getItemId() == CONFIGURE_SCREEN_ID) {
			mapActivity.getMapActions().prepareConfigureScreen();
			mapActivity.getMapActions().toggleDrawer();
			return false;	
		} else if(item.getItemId() == SETTINGS_ID) {
			final Intent settings = new Intent(mapActivity, getMyApplication().getAppCustomization().getSettingsActivity());
			mapActivity.startActivity(settings);
		} else {
			return false;
		}
		return true;
	}


	// To animate view slide out from right to left
	private void open(View view){
		TranslateAnimation animate = new TranslateAnimation(-mapActivity.findViewById(R.id.ParentLayout).getWidth(),0,0,0);
		animate.setDuration(500);
		animate.setFillAfter(true);
		view.startAnimation(animate);
		view.setVisibility(View.VISIBLE);
	}

	private void hide(View view) {
		TranslateAnimation animate = new TranslateAnimation(0, -mapActivity.findViewById(R.id.ParentLayout).getWidth(), 0, 0);
		animate.setDuration(500);
		animate.setFillAfter(true);
		animate.setAnimationListener(new AnimationListener() {
			
			@Override
			public void onAnimationStart(Animation animation) {
				
			}
			
			@Override
			public void onAnimationRepeat(Animation animation) {
				
			}
			
			@Override
			public void onAnimationEnd(Animation animation) {
				dashboardView.setVisibility(View.GONE);
			}
		});
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	

	private void addOrUpdateDashboardFragments() {
		FragmentManager manager = mapActivity.getSupportFragmentManager();
		FragmentTransaction fragmentTransaction = manager.beginTransaction();
		showFragment(manager, fragmentTransaction, DashErrorFragment.TAG, DashErrorFragment.class,
				mapActivity.getMyApplication().getAppInitializer().checkPreviousRunsForExceptions(mapActivity));
		showFragment(manager, fragmentTransaction, DashSearchFragment.TAG, DashSearchFragment.class);
		showFragment(manager, fragmentTransaction, DashRecentsFragment.TAG, DashRecentsFragment.class);
		showFragment(manager, fragmentTransaction, DashFavoritesFragment.TAG, DashFavoritesFragment.class);
		showFragment(manager, fragmentTransaction, DashAudioVideoNotesFragment.TAG, DashAudioVideoNotesFragment.class);
		showFragment(manager, fragmentTransaction, DashTrackFragment.TAG, DashTrackFragment.class);
//		showFragment(manager, fragmentTransaction, DashUpdatesFragment.TAG, DashUpdatesFragment.class);
		showFragment(manager, fragmentTransaction, DashPluginsFragment.TAG, DashPluginsFragment.class);
		fragmentTransaction.commit();
	}



	private <T extends Fragment> void showFragment(FragmentManager manager, FragmentTransaction fragmentTransaction,
			String tag, Class<T> cl) {
		showFragment(manager, fragmentTransaction, tag, cl, true);
	}

	private <T extends Fragment> void showFragment(FragmentManager manager, FragmentTransaction fragmentTransaction,
			String tag, Class<T> cl, boolean cond) {
		try {
			if (manager.findFragmentByTag(tag) == null && cond) {
				T ni = cl.newInstance();
				fragmentTransaction.add(R.id.content, ni, tag);
			}
		} catch (Exception e) {
			getMyApplication().showToastMessage("Error showing dashboard");
			e.printStackTrace();
		}
	}




	private NotifyingScrollView.OnScrollChangedListener onScrollChangedListener = new NotifyingScrollView.OnScrollChangedListener() {
		public void onScrollChanged(ScrollView who, int l, int t, int oldl, int oldt) {
			int sy = who.getScrollY();
			double scale = who.getContext().getResources().getDisplayMetrics().density;
			FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) fabButton.getLayoutParams();
			lp.topMargin = (int) Math.max(30 * scale, 160 * scale - sy);
			((FrameLayout) fabButton.getParent()).updateViewLayout(fabButton, lp);
		}
	};

	public boolean isVisible() {
		return visible;
	}


	public void onDetach(DashBaseFragment dashBaseFragment) {
		Iterator<WeakReference<DashBaseFragment>> it = fragList.iterator();
		while(it.hasNext()) {
			WeakReference<DashBaseFragment> wr = it.next();
			if(wr.get() == dashBaseFragment) {
				it.remove();
			}
		}
	}


	public void onAttach(DashBaseFragment dashBaseFragment) {
		fragList.add(new WeakReference<DashBaseFragment>(dashBaseFragment));
	}
}