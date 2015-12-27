package com.example.administrator.pathanimator;

import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;

/**
 * @author crazychen
 * @version 1.0
 * @date 2015/12/11
 */
public class PathAnimator {
    private ValueAnimator mPathAnimation;
    private Path mPath;
    private PathMeasure mPathMeasure;
    private PathAnimatorUpdateListener mPathAnimatorUpdateListener;
    /**
     * 当前运动占总路径的百分比
     */
    private float pathPrecent = 0;
    /**
     * 路径总长度
     */
    private float totalSegmentLength = 0;
    /**
     * 保存segment路径，也就是把总路径根据contour分割
     */
    private ArrayList<Path> mPathList = new ArrayList<Path>();
    /**
     * 0到每个contour的路径长度
     */
    private ArrayList<Float> mPathLengthList = new ArrayList<Float>();
    /**
     * 当前运动到的路径标号
     */
    private int curPathIndex = 0;
    
    public PathAnimator(Path path){
        mPath = path;
        mPathMeasure = new PathMeasure(mPath,false);
        initPath();
        initAnim();
    }

    /**
     * 初始化路径
     * 将复合路径分割保存到list.
     * 记录路径长度
     */
    private void initPath(){
        curPathIndex = 0;
        PathMeasure pm = new PathMeasure(mPath, false);
        do {
            Path tempPath = new Path();
            tempPath.rewind();
            pm.getSegment(0, pm.getLength(), tempPath, true);
            totalSegmentLength += pm.getLength();
            tempPath.close();
            mPathList.add(tempPath);
            /**
             * 存储遍历过的当前路径长度
             */
            if (mPathLengthList.size()>0) {
                mPathLengthList.add(pm.getLength() + mPathLengthList.get(mPathLengthList.size()-1));
            }else{
                mPathLengthList.add(pm.getLength());
            }
        } while (pm.nextContour());//移动到下一路径
    }

    /**
     * 路径动画，能够根据当前时间，计算出当前运动路径
     */
    private void initAnim(){
        mPathAnimation = ValueAnimator.ofFloat(0f, 1f );
        mPathAnimation.setDuration(500);
        mPathAnimation.setInterpolator(new LinearInterpolator());
        mPathAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                pathPrecent = (float) animation.getAnimatedValue();
                Path totalPath = new Path();
                for (int i = 0; i < curPathIndex; i++) {//将之前运动过的路径加入进来
                    totalPath.addPath(mPathList.get(i));
                }
                Path path = new Path();
                float curLen = pathPrecent * totalSegmentLength;

                if (curPathIndex < mPathLengthList.size() && curLen > mPathLengthList.get(curPathIndex)) {//如果已经长度当前路径长度，移动到下一个节点
                    mPathMeasure.nextContour();
                    curPathIndex++;
                }
                if (curPathIndex > 0)//减去已经运动过的长度，获得真实长度
                    curLen = curLen - mPathLengthList.get(curPathIndex - 1);
                mPathMeasure.getSegment(0, curLen, path, true);
                /*
                 * On KITKAT and earlier releases, the resulting path may not display on a hardware-accelerated Canvas. 
                 * A simple workaround is to add a single operation to this path, such as dst.rLineTo(0, 0).
                 */
                path.rLineTo(0, 0);
                totalPath.addPath(path);
                if(mPathAnimatorUpdateListener!=null) {
                    mPathAnimatorUpdateListener.onAnimationUpdate(pathPrecent, totalPath);
                }
            }
        });
    }

    /**
     * 返回内部动画，用于设置其他动画属性
     * @return
     */
    public ValueAnimator InnerValueAnimator(){
        return mPathAnimation;
    }
    
    /**
     * 开始路径动画
     */
    public void start(){
        curPathIndex = 0;
        mPathAnimation.start();
    }
    
    public void startDelay(long startDelay){
        mPathAnimation.setStartDelay(startDelay);
    }
    
    /**
     * 获取部分路径
     * @param startPrecent 起点百分比
     * @param endPrecent 终点百分比
     * @return 中间路径
     */
    public Path getSegment(float startPrecent,float endPrecent){
        if (startPrecent<0||endPrecent<0||startPrecent>=endPrecent) return null;
        endPrecent = endPrecent>1?1:endPrecent;
        
        float start = totalSegmentLength*startPrecent;
        float end = totalSegmentLength*endPrecent;
        
        Path totalPath = new Path();

        Path path;
        PathMeasure pm;
        for (int i = 0 ; i < mPathLengthList.size(); i++) {//将之前运动过的路径加入进来
            float startFlag = i==0?0:mPathLengthList.get(i-1);
            float endFlag = mPathLengthList.get(i);
            if(start<=startFlag&&endFlag<=end){
                path = new Path();
                pm = new PathMeasure(mPathList.get(i), false);
                pm.getSegment(0,endFlag-startFlag, path, true);
                totalPath.addPath(path);
            }else if(start>=startFlag&&endFlag>=end){
                path = new Path();
                pm = new PathMeasure(mPathList.get(i), false);
                pm.getSegment(start - startFlag, end - startFlag, path, true);
                totalPath.addPath(path);
            }else if(start>startFlag&&endFlag>start&&end>endFlag){
                path = new Path();
                pm = new PathMeasure(mPathList.get(i), true);
                pm.getSegment(start-startFlag,endFlag-startFlag, path, true);
                totalPath.addPath(path);
            } else if(startFlag>start&&end>startFlag&&end<endFlag){
                path = new Path();
                pm = new PathMeasure(mPathList.get(i), false);
                pm.getSegment(0,end-startFlag, path, true);
                totalPath.addPath(path);
            }
        }
        return totalPath;
    }

    /**
     * 获取路径上的某一位置
     * @param pathPrecent
     * @param pos
     * @param tan
     */
    public void getPosTan(float pathPrecent, float pos[], float tan[]){
        if (pathPrecent<0) return;
        pathPrecent = pathPrecent>1?1:pathPrecent;
        float pathLength = pathPrecent*totalSegmentLength;
        int pathIndex = 0;
        while(pathIndex<mPathLengthList.size()){
            if(mPathLengthList.get(pathIndex)>=pathLength){
                break;
            }
            pathIndex++;
        }
        PathMeasure pm = new PathMeasure(mPathList.get(pathIndex), false);
        float startLen = pathIndex==0?0:mPathLengthList.get(pathIndex-1);
        pm.getPosTan(pathLength-startLen,pos,tan);
    }

    /**
     * 获取总长度
     * @return
     */
    public float getTotalSegmentLength() {
        return totalSegmentLength;
    }

    /**
     * 动画时间
     * @param duration
     */
    public void setDuration(long duration){
        mPathAnimation.setDuration(duration);
    }

    /**
     * 插值器
     * @param value
     */
    public void setInterpolator(TimeInterpolator value){
        mPathAnimation.setInterpolator(new AccelerateInterpolator());
    }

    public void addUpdateListener(PathAnimatorUpdateListener pathAnimatorUpdateListener){
        mPathAnimatorUpdateListener = pathAnimatorUpdateListener;
    }
    
    public interface PathAnimatorUpdateListener{
        /**
         * @param pathPrecent 当前路径完成程度，以小数表示，范围是(0,1)
         * @param path 当前完成路径
         */
        public void onAnimationUpdate(float pathPrecent, Path path);
    }

    public void addListener(AnimatorListenerAdapter AnimatorListenerAdapter){
        mPathAnimation.addListener(AnimatorListenerAdapter);
    }
}
