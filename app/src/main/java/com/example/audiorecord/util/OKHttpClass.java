package com.example.audiorecord.util;

import android.content.Context;
import android.util.Log;

import com.example.audiorecord.entity.EventMsg;
import com.example.audiorecord.entity.ResAudio;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OKHttpClass {
    private final static String TAG = OKHttpClass.class.getSimpleName();
    public static final String urlHead = "https://54n4643q86.zicp.fun/";
    private static Request request = null;
    private static Call call = null;
    private static int TimeOut = 120;
    //单例获取ohttp3对象
    private static OkHttpClient client = null;
    public static Context mcontext;
    /**
     * OkHttpClient的构造方法，通过线程锁的方式构造
     * @return OkHttpClient对象
     */
    private static synchronized OkHttpClient getMyInstance() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .readTimeout(TimeOut, TimeUnit.SECONDS)
                    .connectTimeout(TimeOut, TimeUnit.SECONDS)
                    .writeTimeout(TimeOut, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    /**
     * callback接口
     * 异步请求时使用
     */
    static class MyCallBack implements Callback {
        private OkHttpCallback okHttpCallBack;

        public MyCallBack(OkHttpCallback okHttpCallBack) {
            this.okHttpCallBack = okHttpCallBack;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            okHttpCallBack.onFailure(e);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            okHttpCallBack.onSuccess(response);
        }
    }
    /**
     * 获得同步get请求对象Response
     * @param url
     * @return Response
     */
    private static Response doSyncGet(String url) {
        //创建OkHttpClient对象
        client = getMyInstance();
        request = new Request.Builder()
                .url(url)//请求链接
                .build();//创建Request对象
        try {
            //获取Response对象
            Response response = client.newCall(request).execute();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    /**
     * 获得异步get请求对象
     * @param url      请求地址
     * @param callback 实现callback接口
     */
    private static void doAsyncGet(String url,OkHttpCallback callback) {
        MyCallBack myCallback = new MyCallBack(callback);
        client = getMyInstance();
        request = new Request.Builder()
                .url(url)
                .get()
                .build();
        client.newCall(request).enqueue(myCallback);
    }

    /**
     * 同步get请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return String
     */
    public static String getSyncRequest(String url,String... args) {
        List<String> result=new ArrayList<>();//返回值
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Response finalResponse = doSyncGet(finalAddress);
                String res = null;
                try {
                    Log.d("同步get请求请求地址：",finalAddress);
                    if (finalResponse.isSuccessful()) {//请求成功
                        ResponseBody body = finalResponse.body();//拿到响应体
                        res = body.string();
                        result.add(res);
                        Log.d("HttpUtil", "同步get请求成功！");
                        Log.d("请求对象：", res);
                    } else {
                        Log.d("HttpUtil", "同步get请求失败！");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        while(result.size()==0){
            try {
                TimeUnit.MILLISECONDS.sleep(10);//等待xx毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result.get(0);
    }
    /**
     * 异步get请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return String
     */
    public static String getAsyncRequest(String url,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        doAsyncGet(finalAddress, new OkHttpCallback() {
            @Override
            public void onFailure(IOException e) {
                Log.d("异步get请求地址：",finalAddress);
                Log.d("HttpUtil", "异步get请求失败！");
            }
            @Override
            public void onSuccess(Response response) {
                Log.d("异步get请求地址：",finalAddress);
                String res = null;
                try {
                    res = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                result.add(res);
                Log.d("HttpUtil", "异步get请求成功！");
                Log.d("请求对象：", res);
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        while(result.size()==0){
            try {
                TimeUnit.MILLISECONDS.sleep(10);//等待xx毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result.get(0);
    }
    /**
     * 同步post请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */
    public static String postSyncRequest(String url,String json,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        new Thread(new Runnable() {
            @Override
            public void run() {
                client=getMyInstance();
                Log.d("同步post请求地址：",finalAddress);
                //用下面两行的话就是发送json字符串，后端接收后自己去解析
//                FormBody.Builder formBody = new FormBody.Builder();
//                formBody.add("json",json);

                /**
                 * 用下面这行就是发送后端格式的json字符串，后端就不用去解析了。要发送单个的话就用
                 * JSONObject json = new JSONObject();json.put("username", "pikachu");json.toString();
                 * 然后把json.toString()传入postSyncRequest()这个方法中
                 */
                //MediaType  设置Content-Type 标头中包含的媒体类型值
                RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), json);

                request=new Request.Builder()
                        .url(finalAddress)
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.111 Safari/537.36")
                        .post(requestBody)
                        .addHeader("device-platform", "android")
//                        .addHeader("token",mcontext.getSharedPreferences("data",mcontext.MODE_PRIVATE).getString("token",null))
                        .build();
                try{
                    Response response=client.newCall(request).execute();
                    String res=response.body().string();

                    result.add(res);
                    Log.d("HttpUtil", "同步post请求成功！");
                    Log.d("请求对象：", res);
                }catch (Exception e){
                    Log.d("HttpUtil", "同步post请求失败！");
                    e.printStackTrace();
                }
            }
        }).start();
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        while(result.size()==0){
            try {
                TimeUnit.MILLISECONDS.sleep(10);//等待xx毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result.get(0);
    }
    /**
     * 异步post请求
     * 例如：请求的最终地址为：http://127.0.0.1:8081/user/getUser/123
     * @param url 基本请求地址   例子： http://127.0.0.1:8081
     * @param json 提交的json字符串
     * @param args 请求的参数    args[]=new String[]{"user","getUser","123"}
     * @return
     */
    public static String postAsyncRequest(String url,String json,String... args){
        List<String> result=new ArrayList<>();
        String address=url;
        for(int i=0;i<args.length;i++){
            address=address+"/"+args[i];
        }
        final String finalAddress = address;
        Log.d("同步post请求地址：",finalAddress);
        client=getMyInstance();
        FormBody.Builder formBody = new FormBody.Builder();//创建表单请求体
        formBody.add("json",json);
        request = new Request.Builder()
                .url(finalAddress)
                .post(formBody.build())
                .addHeader("device-platform", "android")
                .build();
        Call call=client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("HttpUtil","异步post请求失败！");
                    }
                }).start();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String res = null;
                        try {
                            res = response.body().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        result.add(res);
                        Log.d("HttpUtil","异步post请求成功！");
                        Log.d("请求对象",res);
                    }
                }).start();
            }
        });
        /**因为函数返回是立刻执行的，而result要在请求完成之后才能获得
         * 所以需要等待result获得返回值之后再执行return*/
        while(result.size()==0){
            try {
                TimeUnit.MILLISECONDS.sleep(10);//等待xx毫秒
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return result.get(0);
    }

    public static void postAnsyFileRequest(String url, File file){

        Log.i(TAG,"发送请求到:"+url);
//        OkHttpClient client = new OkHttpClient();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(7, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS).build();
        final String[] res = {null};
        // 构建多部分表单数据
        RequestBody fileBody = RequestBody.create(file, MediaType.parse("application/octet-stream"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), fileBody)
                .build();

        // 构建请求
        Request request = new Request.Builder()
                .addHeader("Content-Type", "multipart/form-data")
                .url(url)
                .post(requestBody)
                .build();

        // 发送请求并处理响应
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("OKHttpClass","请求失败"+e.toString());
//                if (SocketTimeoutException.class.equals(e.getCause())){
//                    Log.i("OKHttpClass","请求超时");
//                }
                EventBus.getDefault().post(new EventMsg(1,false,e.toString()));
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    res[0] = response.body().string();
                    EventBus.getDefault().post(new EventMsg(1,true,res[0]));
                    // 处理成功的响应
                    System.out.println(res[0]);
                } else {
                    // 处理不成功的响应
                    EventBus.getDefault().post(new EventMsg(1,false,response.code()));
                    System.err.println("Request failed: " + response.code());
                }
            }
        });
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        if (res.length>0)
//        return res[0];
//        return null;
    }
}
