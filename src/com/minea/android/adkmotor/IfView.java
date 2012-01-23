package com.minea.android.adkmotor;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.SpannableStringBuilder;

public class IfView extends LinearLayout {
	EditText ed0, ed1;
	SpannableStringBuilder sb; // getText用
	String item; // Spinner のテキスト取得用

	public IfView(Context context) {
		super(context);
		init(context);
	}

	public void init(Context context) {
		setOrientation(HORIZONTAL);
		ed0 = new EditText(context);
		ed0.setTextColor(Color.WHITE);
		ed0.setBackgroundColor(Color.DKGRAY);
		ed0.setWidth(100);
		addView(ed0);
		
		TextView tx0 = new TextView(context);
		tx0.setTextColor(Color.WHITE);
		tx0.setBackgroundColor(Color.DKGRAY);
		tx0.setWidth(100);
		tx0.setText("が");
		addView(tx0);
		
		ed1 = new EditText(context);
		ed1.setTextColor(Color.WHITE);
		ed1.setBackgroundColor(Color.DKGRAY);
		ed1.setWidth(100);
		addView(ed1);

		// spinner の作成
		Spinner spinner = new Spinner(context);
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
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}});
	}

	public String getText(int i) {
		String s;
		switch (i) {
		case 0:
			sb = (SpannableStringBuilder) ed0.getText();
			s = sb.toString();
			break;
		case 1:
			sb = (SpannableStringBuilder) ed1.getText();
			s = sb.toString();
			break;
		case 2:
			s = item;
			break;
		default:
			s = "";
			break;
		}
		return s;
	}
}