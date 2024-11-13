package com.example.audiorecord.util;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.audiorecord.MainActivity;
import com.example.audiorecord.MyApplication;
import com.example.audiorecord.entity.EventMsg;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AudioUtil {
    private static String TAG = AudioUtil.class.getSimpleName();
    private static final int SAMPLE_RATE = 44100;  // 采样率
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;  // 音频通道：单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;  // 音频格式：PCM 16位

    public boolean isRecording() {//获取录制状态
        return isRecording;
    }

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private String outputFile;

    private Handler handler = new Handler();
    private Runnable silenceCheckerRunnable;
    private long silenceStartTime;
    private long recordStartTime;

    private static class AudioUtilHolder {
        private static final AudioUtil Instance = new AudioUtil();
    }

    public static AudioUtil getInstance() {
        return AudioUtil.AudioUtilHolder.Instance;
    }

    public boolean startRecording() {
        if (ActivityCompat.checkSelfPermission(MyApplication.getContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return false;
        }
        try {

            int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, bufferSize);
            outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/MyDemo/" +System.currentTimeMillis()+ ".pcm";
            new File(outputFile).getParentFile().mkdirs();//创建文件夹
            audioRecord.startRecording();
            isRecording = true;

            silenceStartTime = 0;//初始化
            new Thread(new Runnable() {
                @Override
                public void run() {
                    writeAudioDataToFile();
                }
            }).start();

            // 加的,启动静音检测Runnable（可选，用于更精确地检测长时间静音）
            silenceCheckerRunnable = () -> {
                if (isRecording && silenceStartTime != 0 && System.currentTimeMillis() - silenceStartTime > SILENCE_DURATION) {
                    EventBus.getDefault().post(new EventMsg(2,true,null));
                } else {
                    handler.postDelayed(silenceCheckerRunnable, 1000); // 每秒检查一次
                }
            };
            handler.postDelayed(silenceCheckerRunnable, 1000);
        } catch (RuntimeException e) {
            e.printStackTrace();
            releaseRecord();//释放内存
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            releaseRecord();//释放内存
            return false;
        }
        return true;
    }

    public boolean stopRecording(String wavFilePath) {
        isRecording = false;
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return convertPcmToWav(outputFile, wavFilePath);
    }
//    释放
    private void releaseRecord(){
        isRecording = false;
        try {
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void writeAudioDataToFile() {
        byte[] buffer = new byte[1024];
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(outputFile);
            recordStartTime = System.currentTimeMillis();//记录开始录音的时间，用作计算是否两秒内没有声音，没有则结束录音并提示用户说话
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, buffer.length);//将数据读到buffer缓存中

                if (read > 0) {
                    fos.write(buffer, 0, read);//将缓存数据写入fos文件中

                    // 检查静音
                    if (isSilence(buffer, read)) {
                        if (silenceStartTime == 0) {
                            silenceStartTime = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() - silenceStartTime > SILENCE_DURATION) {
                            //判断这次停止时间是不是和开始录音的时间只间隔2、3秒，是则判定为开始录用后用户2、3秒内不讲话，提示用户
                            if (System.currentTimeMillis() - recordStartTime > SILENCE_DURATION &&
                                    System.currentTimeMillis() - recordStartTime < SILENCE_DURATION + 1000){//在开始录音后两秒到三秒没有说话就提示用户
                                EventBus.getDefault().post(new EventMsg(2,false,"请说话"));//false，开始录音没有说话的标志
                                break;
                            }
                            EventBus.getDefault().post(new EventMsg(2,true,null));
                            break;
                        }
                    } else {
                        silenceStartTime = 0; // 重置静音开始时间
                    }

                }
            }
            fos.close();
        } catch (IOException e) {
            Log.i(TAG,"缓存写入失败:"+e.toString());
            EventBus.getDefault().post(new EventMsg(0,false));
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // 在此处进行将 PCM 文件转换为 WAV 格式的操作
    // 可以使用 WavUtil 等工具类进行转换
    public static boolean convertPcmToWav(String pcmFilePath, String wavFilePath) {
        FileInputStream fis = null;
        FileOutputStream fos = null;

        try {
            fis = new FileInputStream(pcmFilePath);
            fos = new FileOutputStream(wavFilePath);

            // WAV 文件头部
            byte[] header = createWavHeader();

            // 写入 WAV 文件头部
            fos.write(header);

            byte[] buffer = new byte[1024];
            int bytesRead;

            // 逐个读取 PCM 数据并写入 WAV 文件
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            fos.close();
            fis.close();

            Log.i(TAG,"pcm转wav成功");
            return true;
        } catch (IOException e) {
            Log.i(TAG,"pcm转wav失败："+e.toString());
            EventBus.getDefault().post(new EventMsg(0,false));
            e.printStackTrace();
            return false;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static byte[] createWavHeader() {
        short numChannels = 1;  // 单声道
        int sampleRate = 44100;  // 采样率
        short bitsPerSample = 16;  // 位深度

        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        short blockAlign = (short) (numChannels * bitsPerSample / 8);

        ByteBuffer buffer = ByteBuffer.allocate(44);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // ChunkID（RIFF 标志）
        buffer.put(new byte[]{'R', 'I', 'F', 'F'});

        // ChunkSize
        buffer.putInt(36);  // 后续文件大小

        // Format（WAVE 标志）
        buffer.put(new byte[]{'W', 'A', 'V', 'E'});

        // Subchunk1ID（fmt 标志）
        buffer.put(new byte[]{'f', 'm', 't', ' '});

        // Subchunk1Size（fmt 大小）
        buffer.putInt(16);  // 固定为 16

        // AudioFormat（音频格式，PCM 为 1）
        buffer.putShort((short) 1);

        // NumChannels（通道数）
        buffer.putShort(numChannels);

        // SampleRate（采样率）
        buffer.putInt(sampleRate);

        // ByteRate（码率）
        buffer.putInt(byteRate);

        // BlockAlign（块对齐）
        buffer.putShort(blockAlign);

        // BitsPerSample（位深度）
        buffer.putShort(bitsPerSample);

        // Subchunk2ID（data 标志）
        buffer.put(new byte[]{'d', 'a', 't', 'a'});

        // Subchunk2Size（音频数据大小）
        buffer.putInt(0);  // 之后填充

        return buffer.array();
    }

    private static final double SILENCE_THRESHOLD = 17; // 静音阈值
    private static final int SILENCE_DURATION = 2000; // 静音持续时间（毫秒）
    private boolean isSilence(byte[] buffer, int read) {
        double sum = 0.0;
        for (int i = 0; i < read; i += 2) { // PCM 16位，每次读取2个字节
            sum += Math.abs((buffer[i + 1] << 8) | (buffer[i] & 0xFF)); // 转换为有符号16位整数
        }
        double rms = Math.sqrt(sum / (read / 2.0)); // 计算均方根值（RMS）
        Log.i(TAG,"静音rms值为："+rms);
        return rms < SILENCE_THRESHOLD;
    }
}
