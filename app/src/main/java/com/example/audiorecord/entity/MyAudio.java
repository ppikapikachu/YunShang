package com.example.audiorecord.entity;

public class MyAudio{

    private String audio;//响应语音文件路径
    private int wait;//等待时间（秒）
    private String text;//语音对应的文本内容
    private int person;//说话人标识（0：系统，1：用户）

    public MyAudio(String audio, int wait, String text, int person) {
        this.audio = audio;
        this.wait = wait;
        this.text = text;
        this.person = person;
    }

    public String getAudio() {
        return audio;
    }

    public void setAudio(String audio) {
        this.audio = audio;
    }

    public int getWait() {
        return wait;
    }

    public void setWait(int wait) {
        this.wait = wait;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPerson() {
        return person;
    }

    public void setPerson(int person) {
        this.person = person;
    }
}