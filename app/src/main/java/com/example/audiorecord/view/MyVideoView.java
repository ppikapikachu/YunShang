package com.example.audiorecord.view;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.VideoView;

import com.example.audiorecord.MyApplication;
import com.example.audiorecord.R;

public class MyVideoView extends VideoView {

    //最终的视频资源宽度
    private int mVideoWidth=480;
    //最终视频资源高度
    private int mVideoHeight=480;
    //视频资源原始宽度
    private int videoRealW=1;
    //视频资源原始高度
    private int videoRealH=1;

    public MyVideoView(Context context) {
        super(context);
    }

    public MyVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void setVideoPath(String path) {
        super.setVideoPath(path);

//        MediaMetadataRetriever是Android原生提供的获取音视频文件信息的一个类，我们可以通过这个类的相关方法获取一些基本信息，
//        如视频时长、宽高、帧率、方向、某一帧的图片等。
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        Uri parse = Uri.parse(path);
        Log.i("-->>自定义videoview的parse",parse+"");
//        retriever.setDataSource(path);//不能用这个，闪退报错
        retriever.setDataSource(MyApplication.getContext(),parse);
        String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);//视频高度
        String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);//视频宽度
        Log.i("----->" + "VideoView", height+"setVideoPath:" +width);

        try {
            videoRealH =Integer.parseInt(height) ;
            videoRealW = Integer.parseInt(width);

        }catch (NumberFormatException e){
            Log.e("----->" + "VideoView", "setVideoPath:" + e.toString());

        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getDefaultSize(0,widthMeasureSpec);
        int height = getDefaultSize(0,heightMeasureSpec);
//        if (height>width){//屏幕竖屏
//            if(videoRealH>videoRealW){//视频竖屏
                Log.i("MyVideioView-...","竖屏竖屏"+width+"==="+height);
                float videoAspectRatio = (float) videoRealW / videoRealH;//视频宽高比
                float screenAspectRatio = (float) width / height;//屏幕宽高比
                if (videoAspectRatio>screenAspectRatio){//视频更宽,则视频的高度将完全适应屏幕高度，宽度会相应调整。
                    if (videoRealW > width){//宽度要缩小
                        float scaleFactorHeight = (float) width / videoRealW;
                        mVideoHeight = (int) (videoRealH*scaleFactorHeight);
                    }else {
                        float scaleFactorHeight = (float) width / videoRealW;
                        mVideoHeight = (int) (videoRealH*scaleFactorHeight);
                    }
                    mVideoWidth=width;
                }else {
                    if (videoRealH > height){//高度要缩小
                        float scaleFactorWidth = (float) height / videoRealH;
                        mVideoWidth = (int) (videoRealW*scaleFactorWidth);
                    }else {
                        float scaleFactorWidth = (float) height / videoRealH;
                        mVideoWidth = (int) (videoRealW*scaleFactorWidth);
                    }
                    mVideoHeight= height;
                }
                float scaleFactorWidth = (float) height / videoRealH;
                float scaleFactorHeight = (float) width / videoRealW;
                float scaleFactor = Math.min(scaleFactorWidth, scaleFactorHeight);
                int scaledWidth = (int) (videoRealW * scaleFactor);
                int scaledHeight = (int) (videoRealH * scaleFactor);
                //如果视频资源是竖屏
                //占满屏幕
//            }else {
//                //如果视频资源是横屏
//                //宽度占满，高度保存比例
//                mVideoWidth=width;
//                float r=videoRealH/(float)videoRealW;
//                mVideoHeight= (int) (mVideoWidth*r);
//            }
//        }else {
//            //横屏
//            if(videoRealH>videoRealW){
//                //如果视频资源是竖屏
//                //宽度占满，高度保存比例
//                mVideoHeight=height;
//                float r=videoRealW/(float)videoRealH;
//                mVideoWidth= (int) (mVideoHeight*r);
//            }else {
//                //如果视频资源是横屏
//                //占满屏幕
//                mVideoHeight=height;
//                mVideoWidth=width;
//            }
//        }
        if(videoRealH==videoRealW&&videoRealH==1){
            //没能获取到视频真实的宽高，自适应就可以了，什么也不用做
            super.onMeasure(widthMeasureSpec,heightMeasureSpec);
        }else {
            Log.i("----->" + "VideoView", mVideoWidth+"最终设置的宽高:" +mVideoHeight);
            setMeasuredDimension(mVideoWidth, mVideoHeight);
        }
    }


}
