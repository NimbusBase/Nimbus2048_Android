package com.example.jason.bounce;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AnimatedView extends ImageView{

	private Context mContext;
	int x = -1;
	int y = -1;
	private int xVelocity = 10;
	private int yVelocity = 5;
	private Handler h;
	private final int FRAME_RATE = 30;
    private final int degreePerSecond = 30;
    int degree = 0;


	public AnimatedView(Context context, AttributeSet attrs)  {  
		super(context, attrs);  
		mContext = context;  
		h = new Handler();
    } 
	
	private Runnable r = new Runnable() {
		@Override
		public void run() {
			invalidate(); 
		}
	};
	
	protected void onDraw(Canvas c) {  
		
		BitmapDrawable ball = (BitmapDrawable) mContext.getResources().getDrawable(R.drawable.ball);
	    if (x<0 && y <0) {
	    	x = this.getWidth()/2;
	    	y = this.getHeight()/2;
	    } else {
	    	x += xVelocity;
	    	y += yVelocity;
	    	if ((x > this.getWidth() - ball.getBitmap().getWidth()) || (x < 0)) {
	    		xVelocity = xVelocity*-1;
	    	}
	    	if ((y > this.getHeight() - ball.getBitmap().getHeight()) || (y < 0)) {
	    		yVelocity = yVelocity*-1;
	    	}
	    }
	    c.drawBitmap(ball.getBitmap(), x, y, null);

        Bitmap bmpOriginal = ball.getBitmap();
        Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas tempCanvas = new Canvas(bmResult);
        degree = (degree+degreePerSecond)%360;
        tempCanvas.rotate(degree, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
        tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
        c.drawBitmap(bmResult, 0, 0, null);

	    h.postDelayed(r, FRAME_RATE);
			
	    	      
	} 
}
