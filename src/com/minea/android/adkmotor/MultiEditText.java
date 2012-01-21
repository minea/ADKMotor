package com.minea.android.adkmotor;

import android.content.Context;
import android.graphics.Color;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

public class MultiEditText extends EditText {
	public MultiEditText(Context context) {
		super(context);
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
	}
}