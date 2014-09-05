package com.appshed.appstore.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.appshed.appstore.R;
import com.appshed.appstore.activities.AppDetailDialog;
import com.appshed.appstore.activities.LoginDialog;
import com.appshed.appstore.activities.MainActivity;
import com.appshed.appstore.adapters.AppAdapter;
import com.appshed.appstore.entities.App;
import com.appshed.appstore.tasks.RetrieveFeaturedApps;
import com.appshed.appstore.tasks.RetrieveMyApps;
import com.appshed.appstore.utils.SystemUtils;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.rightutils.rightutils.collections.RightList;

import java.util.Collections;
import java.util.Comparator;

/**
 * Created by Anton Maniskevich on 9/1/14.
 */
public class AppsCreatedByMeFragment extends Fragment implements View.OnClickListener {

	private static final String TAG = AppsCreatedByMeFragment.class.getSimpleName();
	private RightList<App> apps = new RightList<App>();
	private PullToRefreshListView pullToRefreshListView;
	private ListView actualListView;
	private AppAdapter adapter;
	private View progressBar;
	private View emptyList;

	public static AppsCreatedByMeFragment newInstance() {
		AppsCreatedByMeFragment fragment = new AppsCreatedByMeFragment();
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = View.inflate(getActivity(), R.layout.fragment_apps_created_by_me, null);
		view.findViewById(R.id.img_menu).setOnClickListener(this);
		view.findViewById(R.id.img_login_logout).setOnClickListener(this);
		emptyList = view.findViewById(R.id.img_empty_list);

		progressBar = view.findViewById(R.id.progress_bar);
		pullToRefreshListView = (PullToRefreshListView) view.findViewById(R.id.pull_refresh_list);
		pullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				RetrieveMyApps retrieveMyApps = new RetrieveMyApps(getActivity(), null, AppsCreatedByMeFragment.this);
				if (!apps.isEmpty()) {
					if (refreshView.getCurrentMode().showHeaderLoadingLayout()) {
						retrieveMyApps.setSinceId(apps.get(0).getId());
					} else {
						retrieveMyApps.setMaxId(apps.get(apps.size() - 1).getId());
					}
				}
				retrieveMyApps.execute();
			}
		});
		pullToRefreshListView.setMode(PullToRefreshBase.Mode.BOTH);

		actualListView = pullToRefreshListView.getRefreshableView();
		registerForContextMenu(actualListView);

		adapter = new AppAdapter(getActivity(), apps, R.layout.item_app);
		actualListView.setAdapter(adapter);
		actualListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				startActivity(new Intent(getActivity(), AppDetailDialog.class).putExtra(App.class.getSimpleName(), apps.get(position-1)));
			}
		});
		if (apps.isEmpty()) {
			new RetrieveMyApps(getActivity(), progressBar, AppsCreatedByMeFragment.this).execute();
		}

		showOrHideEmptyList();
		return view;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.img_menu:
				((MainActivity) getActivity()).toggleMenu();
				break;
			case R.id.img_login_logout:
				startActivity(new Intent(getActivity(), LoginDialog.class));
				break;
		}
	}

	public void addApps(final RightList<App> newApps) {
		if (newApps.size() > 0) {
			apps.addAll(newApps);
			Collections.sort(this.apps, new Comparator<App>() {
				@Override
				public int compare(App lhs, App rhs) {
					return (int) (rhs.getId() - lhs.getId());
				}
			});
		}
		showOrHideEmptyList();
		pullToRefreshListView.post(new Runnable() {
			@Override
			public void run() {
				if (!newApps.isEmpty()) {
					adapter.notifyDataSetChanged();
				}
				pullToRefreshListView.onRefreshComplete();
			}
		});
	}

	private void showOrHideEmptyList() {
		if (apps.isEmpty()) {
			emptyList.setVisibility(View.VISIBLE);
		} else {
			emptyList.setVisibility(View.GONE);
		}
	}
}
