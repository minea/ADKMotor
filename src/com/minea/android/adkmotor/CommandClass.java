package com.minea.android.adkmotor;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.FontMetrics;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public class CommandClass {
	// メンバ変数
	HashMap<String, String> commandHm;
	CommandClass primaryConnection, secondaryConnection;
	Command commands;

	// コンストラクタ
	public CommandClass() {
		commandHm = new HashMap<String, String>();
	}

	public void setAttribute(String name, String attri) {
		// name:ユニークな変数名 attri:ローレベルコマンド
		commandHm.put(name, attri);
	}

	public void setPrimaryConnection(CommandClass c) {
		primaryConnection = c;
	}

	public CommandClass getPrimaryConnection() {
		return primaryConnection;
	}

	public void setSecondaryConnection(CommandClass c) {
		secondaryConnection = c;
	}

	public CommandClass getsecondaryConnection() {
		return secondaryConnection;
	}

	public HashMap<String, String> getAttribute() {
		return commandHm;
	}
}

class IfLabel extends LinearLayout implements TextWatcher {
	EditText ed0, ed1;
	SpannableStringBuilder sb; // getText用
	String item; // Spinner のテキスト取得用
	CommandClass hm;

	public IfLabel(Context context) {
		super(context);
		hm = new CommandClass();
		init(context);
	}

	public void init(Context context) {
		setOrientation(HORIZONTAL);
		setBackgroundColor(Color.DKGRAY);
		hm.setAttribute("TYPE " + getId(), "IF");
		ed0 = new EditText(context);
		ed0.setTextColor(Color.WHITE);
		ed0.setBackgroundColor(Color.DKGRAY);
		ed0.setWidth(100);
		ed0.setId(0);
		addView(ed0);

		TextView tx0 = new TextView(context);
		tx0.setTextColor(Color.WHITE);
		tx0.setBackgroundColor(Color.DKGRAY);
		tx0.setWidth(15);
		tx0.setText("が");
		addView(tx0);

		ed1 = new EditText(context);
		ed1.setTextColor(Color.WHITE);
		ed1.setBackgroundColor(Color.DKGRAY);
		ed1.setWidth(100);
		ed1.setId(1);
		addView(ed1);

		// spinner の作成
		Spinner spinner = new Spinner(context);
		spinner.setBackgroundColor(Color.DKGRAY);

		// List の作成
		ArrayList<String> list = new ArrayList<String>();
		list.add("等しい");
		list.add("等しくない");
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
				hm.setAttribute("CONDITION", item);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
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
		/*
		 * String s = edit.toString(); if (getId() == 0) {
		 * hm.setAttribute("IF_LFET_VALUE", s); Log.d("IF LEFT", s); } else if
		 * (getId() == 1) { hm.setAttribute("IF_RIGHT_VALUE", s);
		 * Log.d("IF RIGHT", s); }
		 */
	}
}

class MultiEditLabel extends EditText implements TextWatcher {
	CommandClass hm;

	public MultiEditLabel(Context context) {
		super(context);
		hm = new CommandClass();

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
		setTextColor(Color.WHITE);
		setBackgroundColor(Color.DKGRAY);

		addTextChangedListener(this);
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
		hm.setAttribute("TYPE", "WAIT");
		hm.setAttribute("TIME", s);
		Log.d("WAIT", s);
	}
}

class MultiTextLabel extends View {

	private int width = 55;
	private int height = 50;
	private String mainText = "";
	CommandClass hm;

	public MultiTextLabel(Context context) {
		super(context);
		hm = new CommandClass();
	}

	public void setText(String s) {
		mainText = s;

		if (s.equals("前進")) {
			hm.setAttribute("TYPE", "SEND");
			hm.setAttribute("MASSAGE", "ADVANCE");
		} else if (s.equals("後退")) {
			hm.setAttribute("TYPE", "SEND");
			hm.setAttribute("MASSAGE", "BACK");
		} else if (s.equals("右回転")) {
			hm.setAttribute("TYPE", "SEND");
			hm.setAttribute("MASSAGE", "RROTATE");
		} else if (s.equals("左回転")) {
			hm.setAttribute("TYPE", "SEND");
			hm.setAttribute("MASSAGE", "LROTATE");
		} else if (s.equals("停止")) {
			hm.setAttribute("TYPE", "SEND");
			hm.setAttribute("MASSAGE", "STOP");
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// 背景の描画
		Paint bgPaint = new Paint();
		bgPaint.setColor(Color.DKGRAY);
		// canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(),
		// bgPaint);
		canvas.drawRect(0, 0, 100, 50, bgPaint);
		// メインテキスト用ペイント
		Paint mainTextPaint = new Paint();
		mainTextPaint.setColor(Color.WHITE);
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
}

class ArrowLine extends View {
	Bitmap bmp;

	public ArrowLine(Context context) {
		super(context);
	}

	@Override
	public void onDraw(Canvas canvas) {
		bmp = BitmapFactory.decodeResource(this.getResources(),
				R.drawable.arrow);
		canvas.drawBitmap(bmp, 0, 0, null);
	}
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// ビューのサイズを設定する
		setMeasuredDimension(35, 35);
	}
}