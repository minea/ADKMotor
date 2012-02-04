package com.minea.android.adkmotor;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Paint.FontMetrics;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.view.View.OnDragListener;
import android.view.ViewGroup;
import android.view.View.DragShadowBuilder;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

interface CommandClass {
	public void setAttribute(String name, String attri);

	public HashMap<String, String> getAttribute();
	
	public void setSQLite(SQLiteDatabase mySql);
}

class IfLabel extends LinearLayout implements CommandClass {
	EditText ed0, ed1;
	SpannableStringBuilder sb; // getText用
	String item; // Spinner のテキスト取得用
	HashMap<String, String> commandHm;
	SQLiteDatabase mydb; // 矢印保管

	public IfLabel(Context context) {
		super(context);
		commandHm = new HashMap<String, String>();
		init(context);

		/* ドラッグの処理 */
		setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ClipData data = ClipData.newPlainText("text",
						"text : " + v.toString());
				v.startDrag(data, new DragShadowBuilder(v), (Object) v, 0);
				return true;
			}
		});

	}

	public void init(Context context) {
		setOrientation(HORIZONTAL);
		setBackgroundColor(Color.LTGRAY);
		setAttribute("TYPE", "IF");
		ed0 = new EditText(context);
		ed0.setTextColor(Color.BLACK);
		ed0.setBackgroundColor(Color.LTGRAY);
		ed0.setWidth(120);

		ed0.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable edit) {
				String s = edit.toString();
				setAttribute("IF_LFET_VALUE", s);
			}
		});

		addView(ed0);

		TextView tx0 = new TextView(context);
		tx0.setTextColor(Color.BLACK);
		tx0.setBackgroundColor(Color.LTGRAY);
		tx0.setWidth(20);
		tx0.setText("が");
		addView(tx0);

		ed1 = new EditText(context);
		ed1.setTextColor(Color.BLACK);
		ed1.setBackgroundColor(Color.LTGRAY);
		ed1.setWidth(120);

		ed1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable edit) {
				String s = edit.toString();
				setAttribute("IF_RIGHT_VALUE", s);
			}
		});

		addView(ed1);

		// spinner の作成
		Spinner spinner = new Spinner(context);
		spinner.setBackgroundColor(Color.LTGRAY);

		// List の作成
		ArrayList<String> list = new ArrayList<String>();
		list.add("に等しい");
		list.add("に等しくない");
		list.add("より小さい");
		list.add("より大きい");
		list.add("以上");
		list.add("以下");

		// Adapterの作成
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, list);
		spinner.setAdapter(adapter);
		addView(spinner);
		// スピナーのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Spinner spinner = (Spinner) parent;
				// 選択されたアイテムを取得します
				item = (String) spinner.getSelectedItem();

				setAttribute("CONDITION", item);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	@Override
	public void setAttribute(String name, String attri) {
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name, attri);
	}

	@Override
	public HashMap<String, String> getAttribute() {
		return commandHm;
	}

	@Override
	public void setSQLite(SQLiteDatabase mySql){
		mydb = mySql;
	}
	
	@Override
	public boolean onDragEvent(DragEvent event) {
		boolean result = false;
		switch (event.getAction()) {
		case DragEvent.ACTION_DRAG_STARTED: {
			// ドラッグ開始時に呼び出し
			Log.i("DragSampleView", "Drag started, event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENDED: {
			// ドラッグ終了時に呼び出し
			Log.i("DragSampleView", "Drag ended.");
		}
			break;
		case DragEvent.ACTION_DRAG_LOCATION: {
			// ドラッグ中に呼び出し
			Log.i("DragSampleView", "... seeing drag locations ...");
			result = true;
		}
			break;
		case DragEvent.ACTION_DROP: {
			// ドロップ時に呼び出し
			Log.i("DragSampleView", "Got a drop! =" + this + " event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENTERED: {
			// ドラッグ開始直後に呼び出し
			Log.i("DragSampleView", "Entered " + this);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_EXITED: {
			// ドラッグ終了直前に呼び出し
			Log.i("DragSampleView", "Exited " + this);
			result = true;
		}
			break;
		default:
			Log.i("DragSampleView", "other drag event: " + event);
			result = true;
			break;
		}
		return result;
	}
}

class MultiEditLabel extends EditText implements TextWatcher, CommandClass {
	HashMap<String, String> commandHm;
	SQLiteDatabase mydb; // 矢印保管

	public MultiEditLabel(Context context) {
		super(context);
		commandHm = new HashMap<String, String>();

		setEnabled(true);
		setFocusableInTouchMode(true);
		setFocusable(true);

		// 文字数制限 3桁まで
		InputFilter[] _inputFilter = new InputFilter[1];
		_inputFilter[0] = new InputFilter.LengthFilter(3);
		setFilters(_inputFilter);

		// 文字制限 数字のみ
		setInputType(InputType.TYPE_CLASS_NUMBER);

		// テキストの色を白, テキストの背景をダークグレイ
		setTextColor(Color.BLACK);
		setBackgroundColor(Color.LTGRAY);

		addTextChangedListener(this);

		/* ドラッグの処理 */
		setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ClipData data = ClipData.newPlainText("text",
						"text : " + v.toString());
				v.startDrag(data, new DragShadowBuilder(v), (Object) v, 0);
				return true;
			}
		});
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable edit) {
		String s = edit.toString();
		setAttribute("TYPE", "WAIT");
		setAttribute("TIME", s);
		Log.d("WAIT", s);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// ビューのサイズを設定する
		setMeasuredDimension(150, 60);
	}

	@Override
	public void setAttribute(String name, String attri) {
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name, attri);
	}

	@Override
	public HashMap<String, String> getAttribute() {
		return commandHm;
	}
	
	@Override
	public void setSQLite(SQLiteDatabase mySql){
		mydb = mySql;
	}

	@Override
	public boolean onDragEvent(DragEvent event) {
		boolean result = false;
		switch (event.getAction()) {
		case DragEvent.ACTION_DRAG_STARTED: {
			// ドラッグ開始時に呼び出し
			Log.i("DragSampleView", "Drag started, event=" + event);
			
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENDED: {
			// ドラッグ終了時に呼び出し
			Log.i("DragSampleView", "Drag ended.");
		}
			break;
		case DragEvent.ACTION_DRAG_LOCATION: {
			// ドラッグ中に呼び出し
			Log.i("DragSampleView", "... seeing drag locations ...");
			result = true;
		}
			break;
		case DragEvent.ACTION_DROP: {
			// ドロップ時に呼び出し
			Log.i("DragSampleView", "Got a drop! =" + this + " event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENTERED: {
			// ドラッグ開始直後に呼び出し
			Log.i("DragSampleView", "Entered " + this);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_EXITED: {
			// ドラッグ終了直前に呼び出し
			Log.i("DragSampleView", "Exited " + this);
			result = true;
		}
			break;
		default:
			Log.i("DragSampleView", "other drag event: " + event);
			result = true;
			break;
		}
		return result;
	}
}

class MultiTextLabel extends View implements CommandClass {

	private int width = 100;
	private int height = 50;
	private String mainText = "";
	HashMap<String, String> commandHm;
	SQLiteDatabase mydb; // 矢印保管

	public MultiTextLabel(Context context) {
		super(context);
		commandHm = new HashMap<String, String>();
		setAttribute("TYPE", "SEND");

		/* ドラッグの処理 */
		setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ClipData data = ClipData.newPlainText("text",
						"text : " + v.toString());
				v.startDrag(data, new DragShadowBuilder(v), (Object) v, 0);
				return true;
			}
		});
	}

	public void setText(String s) {
		mainText = s;

		if (s.equals("前進")) {
			setAttribute("MASSAGE", "ADVANCE");
		} else if (s.equals("後退")) {
			setAttribute("MASSAGE", "BACK");
		} else if (s.equals("右回転")) {
			setAttribute("MASSAGE", "RROTATE");
		} else if (s.equals("左回転")) {
			setAttribute("MASSAGE", "LROTATE");
		} else if (s.equals("停止")) {
			setAttribute("MASSAGE", "STOP");
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// 背景の描画
		Paint bgPaint = new Paint();
		bgPaint.setColor(Color.LTGRAY);
		// canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(),
		// bgPaint);
		canvas.drawRect(0, 0, 100, 50, bgPaint);
		// メインテキスト用ペイント
		Paint mainTextPaint = new Paint();
		mainTextPaint.setColor(Color.BLACK);
		mainTextPaint.setTextSize(25);
		mainTextPaint.setAntiAlias(true);

		// メインテキストの描画
		if (mainText != null) {
			PointF textPoint = getTextPoint(mainTextPaint, mainText, width / 2,
					height / 2);
			canvas.drawText(mainText, textPoint.x, textPoint.y, mainTextPaint);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// ビューのサイズを設定する
		setMeasuredDimension(width, height);
	}

	private PointF getTextPoint(Paint textPaint, String text, int centerX,
			int centerY) {
		FontMetrics fontMetrics = textPaint.getFontMetrics();

		// 文字列の幅を取得
		float textWidth = textPaint.measureText(text);

		// 中心にしたいX座標から文字列の幅の半分を引く
		float baseX = centerX - textWidth / 2;

		// 中心にしたいY座標からAscentとDescentの半分を引く
		float baseY = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2;

		return new PointF(baseX, baseY);
	}

	@Override
	public void setAttribute(String name, String attri) {
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name, attri);
	}

	@Override
	public HashMap<String, String> getAttribute() {
		return commandHm;
	}
	
	@Override
	public void setSQLite(SQLiteDatabase mySql){
		mydb = mySql;
	}

	@Override
	public boolean onDragEvent(DragEvent event) {
		boolean result = false;
		switch (event.getAction()) {
		case DragEvent.ACTION_DRAG_STARTED: {
			// ドラッグ開始時に呼び出し
			Log.i("DragSampleView", "Drag started, event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENDED: {
			// ドラッグ終了時に呼び出し
			Log.i("DragSampleView", "Drag ended.");
		}
			break;
		case DragEvent.ACTION_DRAG_LOCATION: {
			// ドラッグ中に呼び出し
			Log.i("DragSampleView", "... seeing drag locations ...");
			result = true;
		}
			break;
		case DragEvent.ACTION_DROP: {
			// ドロップ時に呼び出し
			Log.i("DragSampleView", "Got a drop! =" + this + " event=" + event);
			ContentValues values = new ContentValues();
			values.put("FromWidgetID", getId());
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENTERED: {
			// ドラッグ開始直後に呼び出し
			Log.i("DragSampleView", "Entered " + this);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_EXITED: {
			// ドラッグ終了直前に呼び出し
			Log.i("DragSampleView", "Exited " + this);
			result = true;
		}
			break;
		default:
			Log.i("DragSampleView", "other drag event: " + event);
			result = true;
			break;
		}
		return result;
	}
}

class EmuLabel extends LinearLayout implements CommandClass {
	EditText ed0, ed1;
	SpannableStringBuilder sb; // getText用
	String item; // Spinner のテキスト取得用
	HashMap<String, String> commandHm;
	SQLiteDatabase mydb; // 矢印保管

	public EmuLabel(Context context) {
		super(context);
		commandHm = new HashMap<String, String>();
		init(context);

		/* ドラッグの処理 */
		setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				ClipData data = ClipData.newPlainText("text",
						"text : " + v.toString());
				v.startDrag(data, new DragShadowBuilder(v), (Object) v, 0);
				return true;
			}
		});

	}

	public void init(Context context) {
		setOrientation(HORIZONTAL);
		setBackgroundColor(Color.LTGRAY);
		setAttribute("TYPE", "EXPR");
		ed0 = new EditText(context);
		ed0.setTextColor(Color.BLACK);
		ed0.setBackgroundColor(Color.LTGRAY);
		ed0.setWidth(120);

		ed0.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable edit) {
				String s = edit.toString();
				setAttribute("IF_LFET_VALUE", s);
			}
		});

		addView(ed0);

		// spinner の作成
		Spinner spinner = new Spinner(context);
		spinner.setBackgroundColor(Color.LTGRAY);

		// List の作成
		ArrayList<String> list = new ArrayList<String>();
		list.add("==");
		list.add("=");
		list.add("-");
		list.add("+");
		list.add("×");
		list.add("÷");

		// Adapterの作成
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
				android.R.layout.simple_spinner_item, list);
		spinner.setAdapter(adapter);
		addView(spinner);
		// スピナーのアイテムが選択された時に呼び出されるコールバックリスナーを登録します
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				Spinner spinner = (Spinner) parent;
				// 選択されたアイテムを取得します
				item = (String) spinner.getSelectedItem();
				setAttribute("CONDITION", item);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

		ed1 = new EditText(context);
		ed1.setTextColor(Color.BLACK);
		ed1.setBackgroundColor(Color.LTGRAY);
		ed1.setWidth(120);

		ed1.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable edit) {
				String s = edit.toString();
				setAttribute("IF_RIGHT_VALUE", s);
			}
		});

		addView(ed1);

	}

	@Override
	public void setAttribute(String name, String attri) {
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name, attri);
	}

	@Override
	public HashMap<String, String> getAttribute() {
		return commandHm;
	}
	
	@Override
	public void setSQLite(SQLiteDatabase mySql){
		mydb = mySql;
	}

	@Override
	public boolean onDragEvent(DragEvent event) {
		boolean result = false;
		switch (event.getAction()) {
		case DragEvent.ACTION_DRAG_STARTED: {
			// ドラッグ開始時に呼び出し
			Log.i("DragSampleView", "Drag started, event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENDED: {
			// ドラッグ終了時に呼び出し
			Log.i("DragSampleView", "Drag ended.");
		}
			break;
		case DragEvent.ACTION_DRAG_LOCATION: {
			// ドラッグ中に呼び出し
			Log.i("DragSampleView", "... seeing drag locations ...");
			result = true;
		}
			break;
		case DragEvent.ACTION_DROP: {
			// ドロップ時に呼び出し
			Log.i("DragSampleView", "Got a drop! =" + this + " event=" + event);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_ENTERED: {
			// ドラッグ開始直後に呼び出し
			Log.i("DragSampleView", "Entered " + this);
			result = true;
		}
			break;
		case DragEvent.ACTION_DRAG_EXITED: {
			// ドラッグ終了直前に呼び出し
			Log.i("DragSampleView", "Exited " + this);
			result = true;
		}
			break;
		default:
			Log.i("DragSampleView", "other drag event: " + event);
			result = true;
			break;
		}
		return result;
	}
}

class ArrowDraw extends View {
	Bitmap bmp;

	public ArrowDraw(Context context) {
		super(context);

	}

	@Override
	public void onDraw(Canvas canvas) {
		Paint pathPaint = new Paint();
		pathPaint.setStyle(Paint.Style.STROKE);
		pathPaint.setStrokeWidth(4);
		pathPaint.setColor(Color.BLACK);
		Path mPath = new Path();

		mPath.moveTo(50, 0);
		mPath.lineTo(50, 23);
		mPath.lineTo(42, 18);
		mPath.lineTo(50, 25);
		mPath.lineTo(58, 18);
		mPath.lineTo(50, 25);
		canvas.drawPath(mPath, pathPaint);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// ビューのサイズを設定する
		setMeasuredDimension(60, 30);
	}
}
/* wedget生成時にコンストラクタにSQLiteを引き渡す */