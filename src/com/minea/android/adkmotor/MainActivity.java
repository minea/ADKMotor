package com.minea.android.adkmotor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Collection;
import java.util.Iterator;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;

public class MainActivity extends Activity implements Runnable {
	private static final String TAG = "ADKMotor";
	private static final String ACTION_USB_PERMISSION = "com.minea.android.app.adkmotorics.action.USB_PERMISSION";

	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	private UsbManager mUsbManager;
	private UsbAccessory mAccessory;

	ParcelFileDescriptor mFileDescriptor;

	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	Timer ti;
	ThreadTimer timer_task;
	HashMap<Integer, Command> commands;
	int current_head;
	Command root_command;

	LinearLayout layout;
	int idCounter = 0x700; // ID Counter

	int rootId;
	int widgetId;
	HashMap<Integer, IfLabel> ifLabels;
	HashMap<Integer, MultiEditLabel> multiEditLabels;
	HashMap<Integer, MultiTextLabel> multiTextLabels;

	public MainActivity() {
		commands = new HashMap<Integer, Command>();
		root_command = new NOP();
		commands.put(new Integer(0), root_command);
		current_head = 0;
		ti = new Timer();
		widgetId = 0;
		rootId = 0;
		ifLabels = new HashMap<Integer, IfLabel>();
		multiEditLabels = new HashMap<Integer, MultiEditLabel>();
		multiTextLabels = new HashMap<Integer, MultiTextLabel>();
	}

	// コマンド追加(idは自動的に追加)
	public int setCommand(Command _command) {
		commands.put(new Integer(++current_head), _command);
		return current_head;
	}

	// コマンド追加(指定したidのコマンドに対して追加(上書き))
	public void setCommand(int _at, Command _command) {
		commands.put(new Integer(_at), _command);
	}

	// すでにあるコマンドの削除
	public void removeCommand(int _at) {
		if (commands.containsKey(_at)) {
			Collection<Command> exists_commands = commands.values();
			Iterator<Command> iter = exists_commands.iterator();
			while (iter.hasNext())
				iter.next().removeConnection(commands.get(_at));
			commands.remove(_at);
		}
	}

	// コマンドのユニークidを使って、どこコマンドとどこコマンドをつなげるかを設定
	public void setConnection(int _from, Command.ConnectionTarget _from_target,
			int _to) {
		if (commands.containsKey(_from) && commands.containsKey(_to))
			commands.get(_from).setNext(_from_target, commands.get(_to));
	}

	// つながりをなかった事にする
	public void removeConnection(int _from,
			Command.ConnectionTarget _from_target) {
		if (commands.containsKey(_from))
			commands.get(_from).setNext(_from_target, new END());
	}

	// 全削除
	public void clearCommands() {
		root_command = new NOP();
		commands.clear();
		commands.put(new Integer(0), root_command);
		current_head = 0;
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					// Intent からアクセサリを取得
					UsbAccessory accessory = (UsbAccessory) intent
							.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

					// パーミッションがあるかチェック
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						// 接続を開く
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				// Intent からアクセサリを取得
				UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					// 接続を閉じる
					closeAccessory();
				}
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// UsbManager のインスタンスを取得
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		// オレオレパーミッション用 Broadcast Intent
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);

		// オレオレパーミッション Intent とアクセサリが取り外されたときの Intent を登録
		IntentFilter filter = new IntentFilter();
		filter.addAction(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		/* レイアウトを作成する */
		layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(layout);
		/* root widgetの作成 */
		MultiTextLabel rootLabel = new MultiTextLabel(this);
		rootLabel.setText("スタート");
		layout.addView(rootLabel);
		multiTextLabels.put(widgetId, rootLabel);
		widgetId++;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}
		// USB Accessory の一覧を取得
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);

		// Log.d(TAG, "00 : permission denied for accessory " + accessory);

		if (accessory != null) {
			// Accessory にアクセスする権限があるかチェック
			if (mUsbManager.hasPermission(accessory)) {
				// 接続を開く
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						// パーミッションを依頼
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void openAccessory(UsbAccessory accessory) {
		// アクセサリにアクセスするためのファイルディスクリプタを取得

		Log.d(TAG, "openAccessory : permission denied for accessory "
				+ accessory);

		mFileDescriptor = mUsbManager.openAccessory(accessory);

		if (mFileDescriptor != null) {
			mAccessory = accessory;

			FileDescriptor fd = mFileDescriptor.getFileDescriptor();

			// 入出力用のストリームを確保
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);

			timer_task = new ThreadTimer();
			// この中でアクセサリとやりとりする
			Thread thread = new Thread(null, this, "DemoKit");
			thread.start();
			Log.d(TAG, "accessory opened");
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	// ここでアクセサリと通信する
	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		// アクセサリ -> アプリ
		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;
				switch (buffer[i]) {
				case 0x1:
					// 2byte のオレオレプロトコル
					// 0x1 0x0 や 0x1 0x1 など
					i += 2;
					break;

				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}
		}
	}

	// アプリ -> アクセサリ
	public void sendCommand(byte command, byte value) {
		byte[] buffer = new byte[2];
		if (value != 0x0 && value != 0x1 && value != 0x2)
			value = 0x0;
		// 2byte のオレオレプロトコル
		// 0x1 0x0 や 0x1 0x1
		buffer[0] = command;
		buffer[1] = value;
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	void buildLowLevelCommands() {
		
	}
	/* ボタンを押した時の動作 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// メソッド変数
		SEND send_command = new SEND();
		ArrowLine arrow = new ArrowLine(this);

		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.mAdvance:
			layout.addView(arrow);
			widgetId++;
			MultiTextLabel mtvA = new MultiTextLabel(this);
			mtvA.setText("前進");
			searchingPrimary(widgetId, mtvA);

			layout.addView(mtvA);
			multiTextLabels.put(widgetId, mtvA);

			break;
		case R.id.mBack:
			layout.addView(arrow);
			widgetId++;
			MultiTextLabel mtvB = new MultiTextLabel(this);
			mtvB.setText("後退");
			searchingPrimary(widgetId, mtvB);

			// layout.addView(arrow);
			layout.addView(mtvB);
			multiTextLabels.put(widgetId, mtvB);
			break;
		case R.id.mRRotate:

			break;
		case R.id.mWait:
			layout.addView(arrow);
			widgetId++;
			MultiEditLabel metW = new MultiEditLabel(this);
			searchingPrimary(widgetId, metW);
			layout.addView(metW);
			multiEditLabels.put(widgetId, metW);
			break;
		case R.id.mIf:
			layout.addView(arrow);
			widgetId++;
			IfLabel ifl = new IfLabel(this);
			searchingPrimary(widgetId, ifl);
			layout.addView(ifl);
			ifLabels.put(widgetId, ifl);
			break;
		case R.id.itemRun:
			buildLowLevelCommands();
			break;
		case R.id.itemSave:
			// CommandClass cc = new CommandClass();
			// cc = list.get(0).commandHm.getAttribute();
			break;
		default:
			Log.d(TAG, "Meny not select.");
			return true;
		}
		return false;
	}

	private class ThreadTimer extends TimerTask {
		FileInputStream mInputStream;
		FileOutputStream mOutputStream;
		Command current_command;
		HashMap<String, Integer> variables;

		public ThreadTimer() {
			current_command = new END();
			variables = new HashMap<String, Integer>();
		}

		/*
		 * 将来つかうかも void reset() { current_command = new END(); variables = new
		 * HashMap<String,Integer>(); }
		 */
		public void setIO(FileInputStream _input, FileOutputStream _output) {
			Log.d("ThreadTimer", "ThreadTimer.setIO 0");
			mInputStream = _input;
			mOutputStream = _output;
			Log.d("ThreadTimer", "ThreadTimer.setIO 1");
		}

		void setRoot(Command c) {
			Log.d("ThreadTimer", "ThreadTimer.setRoot");
			current_command = c;
			variables = new HashMap<String, Integer>();
		}

		@Override
		public void run() {
			Log.d("ThreadTimer", "ThreadTimer.run 0");
			do {
				Log.d("ThreadTimer", "ThreadTimer.run 1");
				current_command = current_command.run(mInputStream,
						mOutputStream, variables);
				Log.d("ThreadTimer", "ThreadTimer.run 2");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			} while (!current_command.isEnd());
			current_command.run(mInputStream, mOutputStream, variables);
		}
	}
	
	public void searchingPrimary(int id, IfLabel label){
		for (int i = id - 1; i >= 0; i--) {
			if (null != multiTextLabels.get(i)) {
				multiTextLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != multiEditLabels.get(i)) {
				multiEditLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != ifLabels.get(i)) {
				ifLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			}
		}
	}
	
	public void searchingPrimary(int id, MultiEditLabel label){
		for (int i = id - 1; i >= 0; i--) {
			if (null != multiTextLabels.get(i)) {
				multiTextLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != multiEditLabels.get(i)) {
				multiEditLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != ifLabels.get(i)) {
				ifLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			}
		}
	}
	
	public void searchingPrimary(int id, MultiTextLabel label){
		for (int i = id - 1; i >= 0; i--) {
			if (null != multiTextLabels.get(i)) {
				multiTextLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != multiEditLabels.get(i)) {
				multiEditLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			} else if (null != ifLabels.get(i)) {
				ifLabels.get(i).hm.setPrimaryConnection(label.hm);
				break;
			}
		}
	}
}
