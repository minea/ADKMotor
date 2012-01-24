package com.minea.android.adkmotor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Paint.FontMetrics;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * カスタムビュー 異なるサイズのフォントでメイン、サブのテキストを描画する。
 * 
 * @author yokoi
 * 
 */
public class MultiTextView extends View {

	CommandClass commandHm;
	
	// 部品の幅
	private int width = 55;

	// 部品の高さ
	private int height = 50;

	// メインのテキスト
	private String mainText = null;

	/**
	 * コンストラクタ
	 * 
	 * @param context
	 */
	public MultiTextView(Context context) {
		super(context);
		commandHm = new CommandClass();
	}
	
	public void setText(String s){
		mainText = s;
		
		if (s.equals("前進")){
			commandHm.setAttribute("TYPE", "SEND");
			commandHm.setAttribute("MASSAGE","ADVANCE");
		} else if (s.equals("後退")){
			commandHm.setAttribute("TYPE","SEND");
			commandHm.setAttribute("MASSAGE","BACK");
		} else if (s.equals("右回転")){
			commandHm.setAttribute("TYPE","SEND");
			commandHm.setAttribute("MASSAGE","RROTATE");
		} else if (s.equals("左回転")){
			commandHm.setAttribute("TYPE","SEND");
			commandHm.setAttribute("MASSAGE","LROTATE");
		} else if (s.equals("停止")){
			commandHm.setAttribute("TYPE","SEND");
			commandHm.setAttribute("MASSAGE","STOP");
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		// 背景の描画
		Paint bgPaint = new Paint();
		bgPaint.setColor(Color.DKGRAY);
		//canvas.drawRect(getLeft(), getTop(), getRight(), getBottom(), bgPaint);
		canvas.drawRect(0, 0, 100, 50, bgPaint);
		// メインテキスト用ペイント
		Paint mainTextPaint = new Paint();
		mainTextPaint.setColor(Color.WHITE);
		mainTextPaint.setTextSize(25);
		mainTextPaint.setAntiAlias(true);

		// メインテキストの描画
		if (mainText != null) {
			PointF textPoint = getTextPoint(mainTextPaint, mainText,
					width / 2, height / 2);
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
