package com.example.audiorecord;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.audiorecord.entity.EventMsg;
import com.example.audiorecord.entity.MyAudio;
import com.example.audiorecord.entity.ResAudio;
import com.example.audiorecord.util.AudioUtil;
import com.example.audiorecord.util.OKHttpClass;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private Button tvSpeak = null, play = null;
    //    private Button popup = null;
    private ExecutorService executorService = null;
    private Handler mainThreadHandler = null;
    //    TextView tvLog = null;//录制状态
    private TextView popup_text = null;
    private boolean isPlaying = false;
    private boolean forceStopPlay = false;
    private ResAudio resAudio = null;
    private VideoView myVideoView = null;
    private LayoutInflater inflater = null;
    private final String audioReqUrl = BuildConfig.SERVER_IP + ":" + BuildConfig.SERVER_PORT;
    private View popupView = null;//弹窗视图
    private View layout = null;//本视图
    private RelativeLayout popupRelativeLayout = null;
    private BlockingQueue<Character> queue = new ArrayBlockingQueue<>(300);
    //    wav录制的音频保存地址
    public static String wavFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyDemo/" + System.currentTimeMillis() + ".wav";
    private static final int SHOWTEXT_DELAY = 200;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inflater = LayoutInflater.from(this);
        askPermissions();


        //获取默认的EventBus对象(单例)，并把当前对象注册为Subscriber。
        //注意：当销毁当前实例的时候必须注销这个Subscriber。一般与Activity的生命周期绑定
        EventBus.getDefault().register(this);
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        init();
        initVideoView();
    }

    public void init() {

        tvSpeak = findViewById(R.id.tvSpeak);
//        tvLog = findViewById(R.id.tvLog);
        play = findViewById(R.id.play);
        myVideoView = findViewById(R.id.videoView);
//        popup = findViewById(R.id.popup);
        //本视图
        layout = getLayoutInflater().inflate(R.layout.activity_main, null);
        popupView = getLayoutInflater().inflate(R.layout.popup_view, null);//弹窗视图
        popup_text = popupView.findViewById(R.id.popup_text);//弹窗视图中的textview组件
        popupRelativeLayout = popupView.findViewById(R.id.popup_rela);
//        获取其他xml中的标签
        play.setOnClickListener(this);
        tvSpeak.setOnClickListener(this);
//        popup.setOnClickListener(this);
//        tvSpeak.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        //按下按钮开始录制
////                        startRecord();
//                        startWavRecord();
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        //松开按钮结束录制
////                        stopRecord();
////                        stopWavRecord();
//                        break;
//                    case MotionEvent.ACTION_CANCEL:
//                        break;
//                }
//                return true;
//            }
//        });
    }

    private void initVideoView() {
        //得到videoView
        myVideoView = (VideoView) findViewById(R.id.videoView);
        final String videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_default).toString();
        //设置视频路径
        Log.i(TAG, "videoview的路径" + videoPath);
        myVideoView.setVideoPath(videoPath);
        //开始播放
        myVideoView.start();
        //设置监听是否准备好
        myVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                mp.setLooping(true);
            }
        });
        //设置监听是否播放完
        myVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                myVideoView.setVideoPath(videoPath);
                myVideoView.start();
            }
        });
    }

    private void resetPopupHeight() {
        popup_text.post(new Runnable() {
            @Override
            public void run() {
                // 在视图被添加到窗口并测量、布局之后执行
                int textHeight = popup_text.getHeight();
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        //        获取屏幕的宽高
                        WindowManager wm = MainActivity.this.getWindowManager();
                        int width = wm.getDefaultDisplay().getWidth();
                        int height = wm.getDefaultDisplay().getHeight();
                        RelativeLayout.LayoutParams params = null;
                        if (width < height) {//竖屏
                            params = new RelativeLayout.LayoutParams(
                                    RelativeLayout.LayoutParams.WRAP_CONTENT, // 宽度，可以是具体像素值或使用WRAP_CONTENT/MATCH_PARENT
                                    textHeight  // 高度，同上
                            );
//                            popupRelativeLayout.setLayoutParams(params);
                        } else {
                            popup_text.setHeight(height / 2);
                        }
                        Log.i(TAG, "重新测量赋值popupWindow的高度为：" + textHeight);
                    }
                });
            }
        });
    }

    //    弹窗视图
    PopupWindow popupWindow = null;

    public void Popup() {
        Log.i(TAG, "popup进行一次调用【】【】【】");
        /**
         * Dialog的context不能传入getApplicationContext()，它要依赖于activity。
         */
        if (popupWindow == null)
            popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//        popupWindow.setOutsideTouchable(true);//设置点击外部区域可以取消popupWindow

//        获取屏幕的宽高
        WindowManager wm = this.getWindowManager();
        int width = wm.getDefaultDisplay().getWidth();
        int height = wm.getDefaultDisplay().getHeight();

//        宽度不用设置，跟随textview的高度自适应改变
        if (width < height) {//竖屏
            popupWindow.setWidth((int) (width * 0.4));
//            popupWindow.setHeight(height / 4);
//            Log.i(TAG, height1 + "][[][]][][[][]" + height2);
//            popupWindow.setHeight(dpToPx(this,height2+30));
        } else {
            popupWindow.setWidth(width / 4);
//            popupWindow.setHeight(height / 2);
//            popupWindow.setHeight(dpToPx(this,height2+30));
        }

//        int width1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        int height1 = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        popup_text.measure(width1, height1);
//        int height2 = popup_text.getMeasuredHeight();
//        int width2 = popup_text.getMeasuredWidth();


        /**
         * PopupWindow弹窗不能直接在Activity的onCreate函数里执行，必须要等activity的生命周期函数全部执行完毕之后，需要依附的View加载完成了才可以。
         */
        popupWindow.showAtLocation(layout, Gravity.CENTER, -width / 4, -height / 4);
    }

    //接收消息
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EventMsg event) {
        Log.d("MainActivity", "onEvent...." + event);
        switch (event.getId()) {
            case 0:
                if ((boolean) event.getData())
                    Toast.makeText(this, "录制 成功", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(this, "录制失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case 1:
                if (event.isStatus()) {
//                    处理获取的数据
                    handleAnswer((String) event.getData());
                } else {
                    Toast.makeText(this, "语音发送失败:" + event.getData(), Toast.LENGTH_SHORT).show();
                }
                break;
            case 2://声音消失，结束录制
                if (event.isStatus()) {
                    Log.i(TAG, "声音消失，结束录制");
                    stopWavRecord();
                }else {
                    Log.i(TAG, "开始录音后没有声音，异常中断，提示用户");
                    Toast.makeText(this, "没有听清，点击开始重新录制", Toast.LENGTH_SHORT).show();
                    AudioUtil.getInstance().stopRecording(wavFilePath);//只要结束录制，不用发送语音
                    tvSpeak.setText("开始");
                    tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_normal));
                }
                break;
        }
    }

    private void startWavRecord() {
        tvSpeak.setText("停止");
        tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_click));
        mStartRecordTime = System.currentTimeMillis();//开始时间
        if (!AudioUtil.getInstance().startRecording()) {
            Toast.makeText(this, "开始录制失败", Toast.LENGTH_SHORT).show();
        }
    }

    public void stopWavRecord() {
//        tvSpeak.setText("开始");      //要实现录音结束后依旧是停止按钮，用来停止播放，所以这里两行注释掉
//        tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_normal));

        mStopRecordTime = System.currentTimeMillis();//结束时间
//        tvLog.setText("录音" + (mStopRecordTime - mStartRecordTime) / 1000 + "秒");

        new File(wavFilePath).getParentFile().mkdirs();
        if (!AudioUtil.getInstance().stopRecording(wavFilePath)) {
            Toast.makeText(this, "结束录制失败", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(this, "录制成功", Toast.LENGTH_SHORT).show();
        sendAsw(new File(wavFilePath));//发送语音
    }

    MediaRecorder mediaRecorder = null;
    File mAudioFile = null;
    Long mStartRecordTime, mStopRecordTime;

    private void recordFail() {
        mAudioFile = null;
        //要在主线程执行
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "录音失败", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void releaseRecorder() {
        //检查mediaRecorder不为空
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private void sendAsw(File file) {
        Log.i(TAG, "开始发送语音");
        OKHttpClass.postAnsyFileRequest(audioReqUrl + "/asr", file);
    }

    private void handleAnswer(String res) {
        Log.i(TAG, res + "=-=-得到服务器回复=---=---");

        Gson gson = new Gson();
        Type type = new TypeToken<ResAudio>() {
        }.getType();
        if (res != null) {
            resAudio = gson.fromJson(res, type);//type可以写为ResAudio.class
        }
//        开始播放
        doStartPlay();
    }

    public void doStartPlay() {
//        if (!isPlaying && resAudio != null) {

        play.setText("停止");

        //切换虚拟人物形象为chat
        final String videoPath = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_chat).toString();
        myVideoView.setVideoPath(videoPath);
        //重新设置监听，继续播放chat
        myVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                myVideoView.setVideoPath(videoPath);
                myVideoView.start();
            }
        });

        //配置播放器
        mMediaPlayer = new MediaPlayer();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (resAudio != null) {
                    for (MyAudio myAudio : resAudio.getAudio_list()) {
                        if (myAudio.getAudio() == null || myAudio.getAudio().length() == 0)//语音地址没有
                            continue;
                        //开始播放回答语音
                        startPlay(audioReqUrl + myAudio.getAudio());
                        //追加显示回答的文字
                        mainThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Popup();//显示popup
                                setPopupText("");
                                myVideoView.start();//播放chat视频
                                if (!queue.isEmpty())//队列非空时，代表前面还有句子。两句话中间加一个分号
                                    queue.offer(';');
                                for (char c : myAudio.getText().toCharArray()) {
                                    queue.offer(c);
                                }
                                if (!isCnntinueShowText)//还在显示文字，就不用重新启动方法
                                    continueShowText();
                            }
                        });
                        try {
                            Thread.currentThread().sleep(myAudio.getWait());
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
//        } else {
//            play.setText("播放");
//            executorService.submit(new Runnable() {
//                @Override
//                public void run() {
//                    stopPlay();
//                }
//            });
//        }
    }

    boolean isCnntinueShowText = false;

    //逐个连续显示服务器返回的文字
    private void continueShowText() {
        isCnntinueShowText = true;
        if (forceStopPlay)//强制停止
        {
            queue.clear();//清空文字缓存区，自然停止文字追加
        }
        if (queue.isEmpty()) {
            isCnntinueShowText = false;
            setPopupText(popup_text.getText() + ";");//结束说话，文字最后面加上个分号
            return;
        }
        setPopupText(popup_text.getText() + "" + queue.poll());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                continueShowText();
            }
        }, new Random().nextInt(SHOWTEXT_DELAY));//随机间隔
    }

    //    设置text的文字，并且重新测量高度，给popup设置新高度
//    不测了，用低高度的气泡框
    private void setPopupText(String str) {
        if (Looper.myLooper() == Looper.getMainLooper()) {//主线程
            popup_text.setText(str);
        } else {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    popup_text.setText(str);
                }
            });
        }

//        resetPopupHeight();
    }

    //播放从后台返回的语音
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play:
//                play.setText("播放");
//                new Handler().post(new Runnable() {
//                    @Override
//                    public void run() {
//                        stopPlay();
//                    }
//                });
                break;
//            case R.id.popup:
//
//                Popup();
//                break;
            case R.id.tvSpeak:
                if (AudioUtil.getInstance().isRecording()) {//正在录制则结束录制
                    AudioUtil.getInstance().stopRecording(wavFilePath);//只要结束录制，不用发送语音
                    tvSpeak.setText("开始");
                    tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_normal));
                } else if (isPlaying) {//正在播放则结束
                    forceStopPlay = true;//强制停止语音回答标志位
                    stopPlay();
                    initVideoView();

                } else {//开始录制
                    forceStopPlay = false;//重置标志位
                    tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_click));
                    startWavRecord();
                }

                break;
        }
    }

    MediaPlayer mMediaPlayer = null;

    private void startPlay(String audioPath) {
        Log.i(TAG, "得到回复的音频地址：" + audioPath);
        try {

            //设置声音文件
            mMediaPlayer.setDataSource(audioPath);
            //设置监听回掉
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    stopPlay();
                    startWavRecord();
//                    continueWavRecord();
                }
            });

            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    //提示用户 释放播放器
                    playFail();
                    stopPlay();
                    return true;
                }
            });

            //配置音量 是否循环
            mMediaPlayer.setVolume(1, 1);
            mMediaPlayer.setLooping(false);

            //准备 开始
            mMediaPlayer.prepare();
            //设置播放标签
            isPlaying = true;
            mMediaPlayer.start();

        } catch (RuntimeException e) {
//            可能上面执行到一半用户取消播放了，直接在这里处理
            if (isPlaying)
                e.printStackTrace();
            //异常处理防止闪退
            playFail();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlay() {
        tvSpeak.setText("开始");//全部结束，录制和播放都结束了才设置按钮位开始
        tvSpeak.setBackground(getResources().getDrawable(R.drawable.btn_bg_normal));
        popupWindow.dismiss();//语音播完后隐藏掉popupWindow，语音一半比文字慢
        //重置播放状态
        isPlaying = false;
        play.setText("播放");
        if (mMediaPlayer != null) {
            mMediaPlayer.setOnCompletionListener(null);
            mMediaPlayer.setOnErrorListener(null);
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
//        播放音频结束，切换虚拟人物为default
        initVideoView();
    }
    private void playFail() {
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "播放失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askPermissions() {//动态申请权限！

        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS,//联系人的权限
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,//读写SD卡权限
                    Manifest.permission.RECORD_AUDIO};//麦克风权限
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                }
            }
        }
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
        }
    }

    public int dpToPx(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * scale);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);//反注册总线
        mainThreadHandler.removeCallbacksAndMessages(null);
        mainThreadHandler = null;

    }
}