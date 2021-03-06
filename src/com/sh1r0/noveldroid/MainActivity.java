package com.sh1r0.noveldroid;

import java.io.File;
import java.lang.reflect.Field;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sh1r0.noveldroid.downloader.AbstractDownloader;

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {
	private static final int SUCCESS = 0x10000;
	private static final int PREPARING = 0x10001;
	private static final int FAIL = 0x10002;
	private static final int API_VERSION = Build.VERSION.SDK_INT;

	private int width;
	private EditText etID;
	private EditText etNovelName;
	private EditText etAuthor;
	private EditText etFromPage;
	private EditText etToPage;
	private Button btnAnalyze;
	private Button btnDownload;
	private TextView tvStatus;
	private TextView tvDownloadStatus;
	private Spinner spnDomain;
	private ProgressBar pbDownload;
	private ProgressDialog progressDialog;
	private SharedPreferences prefs;
	private String filename;
	private String downDirPath;
	private NotificationManager mNotificationManager;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private Novel novel;
	private AbstractDownloader novelDownloader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		width = getScreenWidth();
		PreferenceManager.setDefaultValues(this, R.xml.settings, false);

		etID = (EditText) findViewById(R.id.et_id);
		etNovelName = (EditText) findViewById(R.id.et_novel_name);
		etAuthor = (EditText) findViewById(R.id.et_author);
		etFromPage = (EditText) findViewById(R.id.et_from_page);
		etToPage = (EditText) findViewById(R.id.et_to_page);
		btnAnalyze = (Button) findViewById(R.id.btn_analyze);
		btnDownload = (Button) findViewById(R.id.btn_download);
		tvStatus = (TextView) findViewById(R.id.tv_status);
		tvDownloadStatus = (TextView) findViewById(R.id.tv_dl_status);
		spnDomain = (Spinner) findViewById(R.id.spn_doamin);
		pbDownload = (ProgressBar) findViewById(R.id.progressbar);

		ArrayAdapter<String> spnAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, this.getResources().getStringArray(
						R.array.domain));
		spnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spnDomain.setAdapter(spnAdapter);

		btnAnalyze.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				tvStatus.setText("");
				btnDownload.setEnabled(false);

				if (!isNetworkConnected()) {
					Toast.makeText(getApplicationContext(), R.string.no_connection_tooltip,
							Toast.LENGTH_SHORT).show();
					return;
				}

				String tid = etID.getText().toString();
				if (tid.isEmpty()) {
					etID.setError(getResources().getString(R.string.novel_id_tooltip));
					return;
				}

				try {
					novelDownloader = DownloaderFactory.getDownloader(spnDomain
							.getSelectedItemPosition());
					if ((novel = novelDownloader.analyze(tid)) == null) {
						throw new Exception();
					}
				} catch (Exception e) {
					String err = (e.getMessage() == null) ? "analysis fail" : e.getMessage();
					Log.e("Error", err);
					return;
				}

				etAuthor.setText(novel.author);
				etNovelName.setText(novel.name);
				etFromPage.setText(String.valueOf(novel.fromPage));
				etToPage.setText(String.valueOf(novel.toPage));

				Toast.makeText(getApplicationContext(), R.string.analysis_done_tooltip,
						Toast.LENGTH_SHORT).show();
				btnDownload.setEnabled(true);
			}
		});

		btnDownload.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				novel.name = etNovelName.getText().toString();
				novel.author = etAuthor.getText().toString();

				if (novel.name.isEmpty()) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
					dialog.setIcon(android.R.drawable.ic_dialog_alert);
					dialog.setTitle(R.string.error_dialog_title);
					dialog.setMessage(R.string.empty_name_dialog_msg);
					dialog.setCancelable(false);
					dialog.setPositiveButton(R.string.ok_btn,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							});
					dialog.show();
					return;
				}

				try {
					novel.fromPage = Integer.parseInt(etFromPage.getText().toString());
					novel.toPage = Integer.parseInt(etToPage.getText().toString());
					if (novel.fromPage < 1 || novel.fromPage > novel.toPage
							|| novel.toPage > novel.lastPage) {
						throw new NumberFormatException();
					}
				} catch (NumberFormatException e) {
					AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
					dialog.setIcon(android.R.drawable.ic_dialog_alert);
					dialog.setTitle(R.string.error_dialog_title);
					dialog.setMessage(R.string.wrong_page_dialog_msg);
					dialog.setCancelable(false);
					dialog.setPositiveButton(R.string.ok_btn,
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
								}
							});
					dialog.show();
					return;
				}

				File tempDir = new File(NovelUtils.TEMP_DIR);
				if (!tempDir.exists()) {
					tempDir.mkdirs();
				}

				tvStatus.setText(R.string.downloading_tooltip);

				new Thread(new Runnable() {
					@SuppressLint("Wakelock")
					public void run() {
						PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
						PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "btnDownload");

						wl.acquire();
						try {
							novelDownloader.download(progressHandler);

							downDirPath = prefs.getString("down_dir", NovelUtils.APP_DIR);
							mHandler.sendEmptyMessage(PREPARING);
							filename = novelDownloader.process(downDirPath,
									Integer.parseInt(prefs.getString("naming_rule", "0")),
									prefs.getString("encoding", "UTF-8"));
							if (filename == null) {
								throw new Exception();
							}
							mHandler.sendEmptyMessage(SUCCESS);
						} catch (Exception e) {
							e.printStackTrace();
							mHandler.sendEmptyMessage(FAIL);
						}
						wl.release();
					}
				}).start();
			}
		});

		DrawerItem[] menu = new DrawerItem[] {
				DrawerItem.create(0, R.string.search, R.drawable.ic_action_search, this),
				DrawerItem.create(1, R.string.settings, R.drawable.ic_action_settings, this),
				DrawerItem.create(2, R.string.quit, R.drawable.ic_action_quit, this) };

		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);
		mDrawerList.setAdapter(new DrawerItemAdapter(this, R.layout.drawer_list_item, menu));
		mDrawerList.setOnItemClickListener(this);

		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, 0, 0) {
			@Override
			public void onDrawerClosed(View drawerView) {
				supportInvalidateOptionsMenu();
			}

			@Override
			public void onDrawerOpened(View drawerView) {
				supportInvalidateOptionsMenu();
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		try {
			Field mDragger = mDrawerLayout.getClass().getDeclaredField("mLeftDragger");
			mDragger.setAccessible(true);
			ViewDragHelper draggerObj = (ViewDragHelper) mDragger.get(mDrawerLayout);
			Field mEdgeSize = draggerObj.getClass().getDeclaredField("mEdgeSize");
			mEdgeSize.setAccessible(true);
			int edge = mEdgeSize.getInt(draggerObj);
			mEdgeSize.setInt(draggerObj, edge * 10);
		} catch (Exception e) {
			e.printStackTrace();
		}

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		actionBar.setHomeButtonEnabled(true);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mDrawerLayout.closeDrawer(mDrawerList);

		switch (position) {
			case 0: // search
				popupSearchDialog();
				break;
			case 1: // settings
				startActivity(new Intent(this, SettingsActivity.class));
				break;
			case 2: // exit
				finish();
				break;
			default:
				break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		menu.findItem(R.id.menu_search).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		if (item.getItemId() == R.id.menu_search) {
			popupSearchDialog();
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case SUCCESS:
					progressDialog.dismiss();
					Toast.makeText(getApplicationContext(), R.string.download_success_tooltip,
							Toast.LENGTH_LONG).show();
					tvStatus.setText(filename + " "
							+ getResources().getString(R.string.novel_saved_tooltip));

					Intent intent = new Intent(Intent.ACTION_VIEW);
					Uri uri = Uri.fromFile(new File(downDirPath + filename));
					intent.setDataAndType(uri, "text/plain");
					String ticker = filename + " " + getString(R.string.novel_saved_tooltip);

					NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
							MainActivity.this).setContentTitle(getString(R.string.app_name))
							.setContentText(downDirPath + filename).setTicker(ticker)
							.setSmallIcon(android.R.drawable.stat_sys_download_done)
							.setAutoCancel(true);
					PendingIntent contentIntent = PendingIntent.getActivity(MainActivity.this, 0,
							intent, 0);
					mBuilder.setContentIntent(contentIntent);
					mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					mNotificationManager.notify(0, mBuilder.build());

					break;
				case PREPARING:
					pbDownload.setVisibility(View.GONE);
					tvDownloadStatus.setVisibility(View.GONE);
					progressDialog = ProgressDialog.show(MainActivity.this, getResources()
							.getString(R.string.progress_dialog_title),
							getResources().getString(R.string.progress_dialog_msg));
					break;
				case FAIL:
					if (progressDialog != null && progressDialog.isShowing())
						progressDialog.dismiss();
					Toast.makeText(getApplicationContext(), R.string.download_fail_tooltip,
							Toast.LENGTH_SHORT).show();
					tvStatus.setText(R.string.download_fail_msg);
					break;
			}
		}
	};

	@SuppressLint("HandlerLeak")
	public Handler progressHandler = new Handler() {
		int completeTaskNum;
		int totalTaskNum;
		int progress;

		@Override
		public void handleMessage(Message msg) {
			if (msg.what < 0) {
				completeTaskNum = 0;
				totalTaskNum = msg.arg1;
				pbDownload.setProgress(0);
				tvDownloadStatus.setText(0 + "%");
				pbDownload.setVisibility(View.VISIBLE);
				tvDownloadStatus.setVisibility(View.VISIBLE);
				return;
			}

			completeTaskNum += msg.what;
			progress = (int) completeTaskNum * 100 / totalTaskNum;
			tvDownloadStatus.setText(progress + "%");
			pbDownload.setProgress(progress);
		}
	};

	@Override
	protected void onResume() {
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		super.onResume();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		View v = getCurrentFocus();
		boolean ret = super.dispatchTouchEvent(event);

		if (v instanceof EditText) {
			View w = getCurrentFocus();
			int scrcoords[] = new int[2];
			w.getLocationOnScreen(scrcoords);
			float x = event.getRawX() + w.getLeft() - scrcoords[0];
			float y = event.getRawY() + w.getTop() - scrcoords[1];

			if (event.getAction() == MotionEvent.ACTION_UP
					&& (x < w.getLeft() || x >= w.getRight() || y < w.getTop() || y > w.getBottom())) {
				closeKeyboard();
			}
		}
		return ret;
	}

	private boolean isNetworkConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
	}

	private void closeKeyboard() {
		InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	@SuppressWarnings("deprecation")
	private int getScreenWidth() {
		Display display = getWindowManager().getDefaultDisplay();
		if (API_VERSION >= 13) {
			Point size = new Point();
			display.getSize(size);
			width = size.x;
		} else {
			width = display.getWidth();
		}
		return width;
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void popupSearchDialog() {
		LayoutInflater factory = LayoutInflater.from(this);
		final View searchDialogView = factory.inflate(R.layout.search_dialog, null);
		final AlertDialog searchDialog = new AlertDialog.Builder(this).setTitle(R.string.search)
				.setNegativeButton(R.string.close_btn, null).setCancelable(false).create();
		searchDialog.setView(searchDialogView);

		final WebView wv = (WebView) searchDialogView.findViewById(R.id.wv_search);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.loadUrl("https://googledrive.com/host/0By9mvBCbgqrycV9naFJSYm5mbjQ");
		wv.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				view.loadUrl(url);
				return true;
			}
		});
		wv.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((keyCode == KeyEvent.KEYCODE_BACK)) {
					if (wv.canGoBack()) {
						wv.goBack();
					} else {
						searchDialog.dismiss();
					}
					return true;
				}
				return false;
			}
		});

		searchDialog.show();
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
		lp.copyFrom(searchDialog.getWindow().getAttributes());
		lp.width = width;
		searchDialog.getWindow().setAttributes(lp);
	}
}
