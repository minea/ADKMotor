package com.minea.android.adkmotor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.View;

public class ArrowLine extends View {
	CommandClass commandHm;
	Bitmap bmp;
	public ArrowLine(Context context) {
		super(context);
		commandHm = new CommandClass();
		bmp = BitmapFactory.decodeResource(this.getResources(), R.drawable.arrow);
	}
	
	@Override
	public void onDraw(Canvas canvas){
        canvas.drawBitmap(bmp, 0, 0, null); 
	}
}