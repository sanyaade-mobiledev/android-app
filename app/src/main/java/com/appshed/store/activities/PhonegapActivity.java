package com.appshed.store.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.appshed.store.R;
import com.appshed.store.applications.AppStoreApplication;
import com.appshed.store.entities.App;
import com.appshed.store.utils.SystemUtils;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.apache.cordova.Config;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaInterfaceImpl;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaPreferences;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaWebViewImpl;
import org.apache.cordova.PluginEntry;
import org.apache.cordova.engine.SystemWebView;
import org.apache.cordova.engine.SystemWebViewClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Anton Maniskevich on 8/7/14.
 */
public class PhonegapActivity extends Activity implements CordovaInterface {

	private static final String TAG = PhonegapActivity.class.getSimpleName();
	public static final int SPLASH_DELAY = 3000;
	private CordovaWebView cordovaWebView;
	protected CordovaPlugin activityResultCallback = null;
	protected boolean activityResultKeepRunning;
	protected boolean keepRunning = true;
	private final ExecutorService threadPool = Executors.newCachedThreadPool();
	private App selectedApp;
	private View splashContainer;
	protected CordovaInterfaceImpl cordovaInterface;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_phonegap);
		final long startTime = System.currentTimeMillis();
		splashContainer = findViewById(R.id.splash_container);
		splashContainer.setOnClickListener(null);
		ImageView splashImage = (ImageView) findViewById(R.id.img_splash);
		String url = null;
		if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(App.class.getSimpleName())) {
			long appId = getIntent().getExtras().getLong(App.class.getSimpleName());
			Log.i(TAG, "" + appId);
			selectedApp = AppStoreApplication.dbUtils.getAllWhere(String.format("id = %d", appId), App.class).getFirst();
			Log.i(TAG, "" + selectedApp);
			url = "file:" + SystemUtils.getAppFolder(appId) + "/index.html";
			splashImage.setImageDrawable(BitmapDrawable.createFromPath(SystemUtils.getAppFolder(appId) + "/Defaulth5682x.png"));
		}

		cordovaInterface =  new CordovaInterfaceImpl(getActivity());
		if(savedInstanceState != null) {
			cordovaInterface.restoreInstanceState(savedInstanceState);
		}
		cordovaWebView = new CordovaWebViewImpl(CordovaWebViewImpl.createEngine(getActivity(), new CordovaPreferences()));
		RelativeLayout.LayoutParams wvlp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		cordovaWebView.getView().setLayoutParams(wvlp);
		((LinearLayout) findViewById(R.id.web_view_container)).addView(cordovaWebView.getView());

		cordovaWebView.clearCache();
		Config.init(this);
		cordovaWebView.init(this, Config.getPluginEntries(), Config.getPreferences());
		cordovaWebView.loadUrl(url);
//		cordovaWebView.setWebViewClient(new WebViewClient(){
//			@Override
//			public void onPageFinished(WebView view, String url) {
//				super.onPageFinished(view, url);
				long endTime = System.currentTimeMillis();
				splashContainer.postDelayed(new Runnable() {
					@Override
					public void run() {
						splashContainer.setVisibility(View.GONE);
					}
				}, endTime - startTime >= SPLASH_DELAY ? 0 : SPLASH_DELAY-(endTime-startTime));
//			}
//		});


		AdView adView = (AdView) this.findViewById(R.id.adView);
		if (selectedApp.isAds()) {
			AdRequest adRequest = new AdRequest.Builder()
					.addTestDevice("F868FDCE83251A07F3CC020B9CB199B1")
					.build();
			adView.loadAd(adRequest);
		} else {
			findViewById(R.id.bottom_bar).setVisibility(View.GONE);
		}
	}

	@Override
	public void startActivityForResult(CordovaPlugin command, Intent intent, int requestCode) {
		this.activityResultCallback = command;
		this.activityResultKeepRunning = this.keepRunning;
		// If multitasking turned on, then disable it for activities that return results
		if (command != null) {
			this.keepRunning = false;
		}
		// Start activity
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void setActivityResultCallback(CordovaPlugin plugin) {
		this.activityResultCallback = plugin;
	}

	@Override
	public Activity getActivity() {
		return this;
	}

	@Override
	public Object onMessage(String id, Object data) {
		Log.i(TAG, "onMessage=" + id);
		if("onReceivedError".equals(id)) {
//			JSONObject d = (JSONObject)data;
//			try {
//				this.onReceivedError(d.getInt("errorCode"), d.getString("description"), d.getString("url"));
//			} catch (JSONException var5) {
//				var5.printStackTrace();
//			}
		} else if("exit".equals(id)) {
			this.finish();
		}

		return null;
	}

	@Override
	public ExecutorService getThreadPool() {
		return threadPool;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		CordovaPlugin callback = this.activityResultCallback;
		if (callback != null) {
			callback.onActivityResult(requestCode, resultCode, intent);
		}
	}

	public void onDestroy() {
		super.onDestroy();
		if (this.cordovaWebView != null) {
			this.cordovaWebView.handleDestroy();
		}
	}

}
