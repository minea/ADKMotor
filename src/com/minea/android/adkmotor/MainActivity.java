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

import com.minea.android.adkmotor.IF.CompOperation;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.Pair;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.graphics.Color;
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
	int current_head;
	Command root_command;

	RelativeLayout layout;
	int idCounter = 0x700; // ID Counter

	int rootId;
	int widgetId; // widget に割り振るID
	int arrowId;
	HashMap<Integer, Command> commands;
	HashMap<Integer, CommandClass> commandArray; // Widget の一覧保持
	static SQLiteDatabase mydb; // 矢印の情報保持用
	static int fromWidgetId = 0;
	static int ifFlag = 0; // True == 1, false == -1, null == 0
	static float from_x = 50;
	static float from_y = 0;
	static float to_y = 0;

	int paramsHeigh = 90; // 命令ラベルと命令ラベルの間
	private final int WC = ViewGroup.LayoutParams.WRAP_CONTENT; // Layout用のパラメータ

	private final String CREATE_TABLE_SQL = "CREATE TABLE CommandConnection ( ArrowID INTEGER, "
			+ "FromWidgetID INTEGER, "
			+ "ToWidgetID INTEGER, "
			+ "IfFlag INTEGER);";

	public MainActivity() {
		commands = new HashMap<Integer, Command>();
		root_command = new NOP();
		commands.put(new Integer(0), root_command);
		current_head = 0;
		ti = new Timer();
		widgetId = 0;
		rootId = 0;
		arrowId = 0;
		timer_task = new ThreadTimer();
		commandArray = new HashMap<Integer, CommandClass>();
	}

	// コマンド追加(idは自動的に追加)
	public int setCommand(Command _command) {
		commands.put(new Integer(++current_head), _command);
		Log.d("current_head", "is " + current_head);
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

		// 矢印の遷移先を管理するSQLite作成
		MySQLiteOpenHelper hlpr = new MySQLiteOpenHelper(
				getApplicationContext(), CREATE_TABLE_SQL);
		mydb = hlpr.getWritableDatabase();

		/* レイアウトを作成する */
		layout = new RelativeLayout(this);
		setContentView(layout);
		layout.setBackgroundColor(Color.WHITE);
		/* root widgetの作成 */
		MultiTextLabel rootLabel = new MultiTextLabel(this);
		rootLabel.setText("スタート");
		rootLabel.setId(widgetId);
		layout.addView(rootLabel);
		commandArray.put(0, rootLabel);
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
		/*if (value != 0x0 && value != 0x1 && value != 0x2 && value != 0x3 && value != 0x4)
			value = 0x0;*/
		// 2byte のオレオレプロトコル
		// 0x1 0x0 や 0x1 0x1
		buffer[0] = command;
		buffer[1] = value;
		Log.e("sendCommand","value is "+ buffer[1]);
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
		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {
		case R.id.mAdvance:
			/* Widget の処理 */
			final MultiTextLabel mtvA = new MultiTextLabel(this);
			mtvA.setText("前進");
			mtvA.setId(widgetId);
			commandArray.put(widgetId, mtvA);
			widgetId++;
			// commandConnection.put(widgetId, Pair.create(3, -1));

			/*
			 * コマンドの処理 send_command = new SEND();
			 * send_command.setOperation("Advance"); setCommand(++current_head,
			 * send_command); setConnection(current_head - 1,
			 * Command.ConnectionTarget.NEXT, current_head);
			 */
			/* レイアウトに配置 */
			layout.addView(mtvA, createParam(paramsHeigh));
			paramsHeigh += 90;

			final ArrowDraw arrow = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			mtvA.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DRAG_STARTED: {
					}
						break;
					case DragEvent.ACTION_DRAG_ENDED: {
					}
						break;
					case DragEvent.ACTION_DRAG_LOCATION: {
						result = true;
					}
						break;
					case DragEvent.ACTION_DROP: {
						Log.i("mtvA", "---Drop---");
						to_y = mtvA.getTop();
						arrow.setFromToPoint(from_x, from_y, to_y);
						arrow.setId(arrowId);
						arrowId++;
						layout.addView(arrow);
						
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrow.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", mtvA.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);

						/*
						 * Cursor cursor = mydb.query("CommandConnection", new
						 * String[] { "ArrowID", "FromWidgetID", "ToWidgetID" },
						 * null, null, null, null, null);
						 * //startManagingCursor(cursor); boolean isEof =
						 * cursor.moveToFirst(); Log.d("drop", "fromWidgetID" +
						 * cursor.getInt(1));
						 */
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mBack:
			final MultiTextLabel mtvB = new MultiTextLabel(this);
			mtvB.setText("後退");
			mtvB.setId(widgetId);
			commandArray.put(widgetId, mtvB);
			widgetId++;
			layout.addView(mtvB, createParam(paramsHeigh));
			paramsHeigh += 90;

			final ArrowDraw arrowB = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			mtvB.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("mtvB", "---Drop---");
						arrowB.setId(arrowId);
						arrowId++;

						to_y = mtvB.getTop();
						arrowB.setFromToPoint(from_x, from_y, to_y);
						layout.addView(arrowB);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowB.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", mtvB.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mRRotate:
			final MultiTextLabel mtvRR = new MultiTextLabel(this);
			mtvRR.setText("右回転");
			mtvRR.setId(widgetId);
			commandArray.put(widgetId, mtvRR);
			widgetId++;

			layout.addView(mtvRR, createParam(paramsHeigh));
			paramsHeigh += 90;

			final ArrowDraw arrowRR = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			mtvRR.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("mtvRR", "---Drop---");
						arrowRR.setId(arrowId);
						arrowId++;

						to_y = mtvRR.getTop();
						arrowRR.setFromToPoint(from_x, from_y, to_y);
						layout.addView(arrowRR);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowRR.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", mtvRR.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mLRotate:
			final MultiTextLabel mtvLR = new MultiTextLabel(this);
			mtvLR.setText("左回転");
			mtvLR.setId(widgetId);
			commandArray.put(widgetId, mtvLR);
			widgetId++;
			layout.addView(mtvLR, createParam(paramsHeigh));
			paramsHeigh += 90;

			final ArrowDraw arrowLR = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			mtvLR.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("mtvLR", "---Drop---");
						
						arrowLR.setId(arrowId);
						arrowId++;

						to_y = mtvLR.getTop();
						arrowLR.setFromToPoint(from_x, from_y, to_y);
						layout.addView(arrowLR);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowLR.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", mtvLR.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mWait:
			final WaitLabel waitL = new WaitLabel(this);
			waitL.setId(widgetId);
			commandArray.put(widgetId, waitL);
			widgetId++;
			layout.addView(waitL, createParam(paramsHeigh));
			paramsHeigh += 90;
			final ArrowDraw arrowW = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			waitL.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("waitL", "---Drop---");
						arrowW.setId(arrowId);
						arrowId++;
						to_y = waitL.getTop();
						arrowW.setFromToPoint(from_x, from_y, to_y);
						layout.addView(arrowW);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowW.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", waitL.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mIf:
			final IfLabel ifl = new IfLabel(this);
			IfTFLabel iftl = new IfTFLabel(this);
			IfTFLabel iffl = new IfTFLabel(this);
			ifl.setId(widgetId);
			iftl.setId(widgetId);
			iffl.setId(widgetId);
			iftl.superWidgetId = ifl.getId();
			iffl.superWidgetId = ifl.getId();
			iftl.setText("はい");
			iffl.setText("いいえ");
			// IfLabel を Layout に貼付け
			layout.addView(ifl, createParam(paramsHeigh));
			// Ifのはいといいえを貼付け
			LayoutParams params = new RelativeLayout.LayoutParams(WC, WC);
			params.setMargins(0, paramsHeigh + 60, 0, 0);
			layout.addView(iftl, params);
			LayoutParams params_ = new RelativeLayout.LayoutParams(WC, WC);
			params_.setMargins(130, paramsHeigh + 60, 0, 0);
			layout.addView(iffl, params_);
			commandArray.put(widgetId, ifl);
			widgetId++;
			paramsHeigh += 140;

			final ArrowDraw arrowIf = new ArrowDraw(this);
			/* ドラッグアンドドロップ処理 */
			ifl.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("DragSample", "Drop!!");
						arrowIf.setId(arrowId);
						arrowId++;
						to_y = ifl.getTop();
						arrowIf.setFromToPoint(from_x, from_y, to_y);
						layout.addView(arrowIf);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowIf.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", ifl.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			/*
			 * IF if_command = new IF(); setCommand(++current_head, if_command);
			 * setConnection(current_head - 1, Command.ConnectionTarget.NEXT,
			 * current_head);
			 */
			break;
		case R.id.mStop:
			final MultiTextLabel mtvS = new MultiTextLabel(this);
			mtvS.setText("停止");
			mtvS.setId(widgetId);
			commandArray.put(widgetId, mtvS);
			widgetId++;
			layout.addView(mtvS, createParam(paramsHeigh));
			paramsHeigh += 90;

			final ArrowDraw arrowS = new ArrowDraw(this);
			mtvS.setOnDragListener(new View.OnDragListener() {
				public boolean onDrag(View v, DragEvent event) {
					final int action = event.getAction();
					boolean result = false;
					switch (action) {
					case DragEvent.ACTION_DROP: {
						Log.i("DragSample", "Drop!!");
						arrowS.setId(arrowId);
						arrowId++;
						arrowS.setFromToPoint(from_x, from_y, to_y);
						to_y = mtvS.getTop();
						layout.addView(arrowS);
						ContentValues values = new ContentValues();
						values.put("ArrowID", arrowS.getId());
						values.put("FromWidgetID", fromWidgetId);
						values.put("ToWidgetID", mtvS.getId());
						if(ifFlag != 0){
							values.put("IfFlag", ifFlag);
							ifFlag = 0;
						}
						mydb.insert("CommandConnection", null, values);
						result = true;

						break;
					}
					}
					return result;
				}
			});
			break;
		case R.id.mExpr:
			ExprLabel exprL = new ExprLabel(this);
			exprL.setId(widgetId);
			widgetId++;
			layout.addView(exprL, createParam(paramsHeigh));
			paramsHeigh += 90;
			commandArray.put(widgetId, exprL);
			/*
			 * send_command = new SEND(); send_command.setOperation("Back");
			 * setCommand(++current_head, send_command);
			 * setConnection(current_head - 1, Command.ConnectionTarget.NEXT,
			 * current_head);
			 */

			break;
		case R.id.itemRun:
			// CommandArray から Commands に変換
			commandArrayToCommands(commandArray, widgetId);
			Log.i("widget ID", "is " + widgetId);

			// commandsToConnection(arrowId);
			// Command の接続
			Cursor c = mydb.query("CommandConnection", new String[] { "ArrowID","FromWidgetID",
					"ToWidgetID","IfFlag"}, null, null, null, null, null);
	         
			String text = "";
	        boolean isEof = c.moveToFirst();
	        while (isEof) {
	            text = String.format("ArrowID "+ c.getInt(0) + ", FromWidgetID "+ c.getInt(1) + 
						", ToWidgetID " + c.getInt(2) + ", IfFlag" +c.getInt(3));
	            Log.e("SQLite",text);
	            isEof = c.moveToNext();
	        }
	        c.close();
	        mydb.close();
	    
	 

			
			timer_task.setRoot(root_command);
			timer_task.setIO(mInputStream, mOutputStream);
			timer_task.run();
			clearCommands();
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
				/*
				 * Log.d("ThreadTimer", "ThreadTimer.run 2"); try {
				 * Thread.sleep(1000); } catch (InterruptedException e) { }
				 */
			} while (!current_command.isEnd());
			current_command.run(mInputStream, mOutputStream, variables);
		}
	}

	/* Layoutに設置する場所やらの設定 */
	private RelativeLayout.LayoutParams createParam(int h) {
		LayoutParams params = new RelativeLayout.LayoutParams(WC, WC);
		params.setMargins(0, h, 0, 0);
		return params;
	}

	public void commandArrayToCommands(
			HashMap<Integer, CommandClass> commandArray, int lastId) {

		HashMap<String, String> commandHm;
		CommandClass commandClass;

		for (int i = 1; i < lastId; i++) {
			commandClass = commandArray.get(i);
			if( commandClass == null){
				Log.i("commandClass "+i,"NULL");
			} else {
			commandHm = commandClass.getAttribute();
			Log.i("commandClass " + i, "is " + commandHm.get("MASSAGE"));

			// 前進などのコマンド
			if (commandHm.get("TYPE").equalsIgnoreCase("SEND")) {
				SEND send_command = new SEND();
				send_command.setOperation(commandHm.get("MASSAGE"));

				Log.d("set " + i, "SEND COMMND");
				setCommand(i, send_command);
				setConnection(i - 1, Command.ConnectionTarget.NEXT, i);
			} else if (commandHm.get("TYPE").equalsIgnoreCase("IF")) {
				String item = "";
				String left = "";
				String right = "";
				CompOperation operation = CompOperation.EQUAL;

				IF if_command = new IF();

				left = commandHm.get("IF_LFET_VALUE");
				right = commandHm.get("IF_RIGHT_VALUE");

				item = commandHm.get("CONDITION");
				if (item.equalsIgnoreCase("に等しい")) {
					operation = CompOperation.EQUAL;
				} else if (item.equalsIgnoreCase("に等しくない")) {
					operation = CompOperation.NOT_EQUAL;
				} else if (item.equalsIgnoreCase("より小さい")) {
					operation = CompOperation.LESS_THAN;
				} else if (item.equalsIgnoreCase("より大きい")) {
					operation = CompOperation.MORE_THAN;
				} else if (item.equalsIgnoreCase("以上")) {
					operation = CompOperation.MORE_EQUAL;
				} else if (item.equalsIgnoreCase("以下")) {
					operation = CompOperation.LESS_EQUAL;
				}
				if_command.setOperation(left, operation, right);

				setCommand(i, if_command);
				setConnection(i - 1, Command.ConnectionTarget.NEXT, i);
			} else if (commandHm.get("TYPE").equalsIgnoreCase("WAIT")) {
				String timeS = "";
				int timeI = 0;

				WAIT wait_command = new WAIT();
				timeS = commandHm.get("TIME");
				timeI = Integer.parseInt(timeS);
				wait_command.setTime(timeI * 1000);

				setCommand(i, wait_command);
				setConnection(i - 1, Command.ConnectionTarget.NEXT, i);
			} else if (commandHm.get("TYPE").equalsIgnoreCase("EXPR")) {
				String timeS = "";
				int timeI = 0;

				EXPR expr_command = new EXPR();

				timeS = commandHm.get("TIME");
				timeI = Integer.parseInt(timeS);
				// wait_command.setTime(timeI * 1000);
			}}
		}
	}

}
/*
 * 2次間数で矢印を描く Y軸の相対的な距離を取り、
 */
