package com.example.administrator.demo.path;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import com.example.administrator.pathanimator.PathAnimator;

/**
 * @author
 * @version 1.0
 * @date 2015/12/27 0027
 */
public class PathView1 extends View {
    PathAnimator mPathAnimator;
    int w,h;
    Path mPath;
    Paint mPaint = new Paint();
    public PathView1(Context context) {
        super(context);
    }

    public PathView1(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PathView1(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.w = w;
        this.h = h;
        init();
        super.onSizeChanged(w, h, oldw, oldh);
    }
    
    private void init(){
        //画笔颜色
        mPaint.setStrokeWidth(20);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(Color.GREEN);
        mPaint.setAntiAlias(true);
        
        //要绘制的路径
        Path path = new Path();
        path.addCircle(w / 2, h / 2, w / 2 - 20, Path.Direction.CCW);
        path.moveTo(w / 2 - 60, h / 2 + 10);
        path.lineTo(w / 2 - 30, h / 2 + 50);
        path.lineTo(w / 2 + 50, h / 2 - 60);
        
        //路径动画
        mPathAnimator = new PathAnimator(path);
        mPathAnimator.setDuration(1500);//动画时间
        mPathAnimator.startDelay(500);
        mPathAnimator.addUpdateListener(new PathAnimator.PathAnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(float pathPrecent, Path path) {
                mPath = path;//更新当前路径
                invalidate();
            }
        });
        mPathAnimator.start();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        if(mPath!=null){
            canvas.drawPath(mPath,mPaint);
        }
    }
}
