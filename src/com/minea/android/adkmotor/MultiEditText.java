package com.minea.android.adkmotor;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

public class MultiEditText extends EditText implements TextWatcher {
	CommandClass commandHm;

	public MultiEditText(Context context) {
		super(context);
		commandHm = new CommandClass();
		
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
	public void beforeTextChanged(CharSequence s, int start, int count,int after) {
		
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {

	}

	@Override
	public void afterTextChanged(Editable edit) {
		String s = edit.toString();
		commandHm.setAttribute("TYPE", "WAIT");
		commandHm.setAttribute("TIME", s);
		Log.d("Id Number "+getId(),s);
	}
}