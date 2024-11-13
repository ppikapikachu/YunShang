package com.example.audiorecord.entity;

import java.util.ArrayList;

public class ResAudio {

    private String text;//语音识别出的文字
    private ArrayList<MyAudio> audio_list;

    public ResAudio(String text, ArrayList<MyAudio> audio_list) {
        this.text = text;
        this.audio_list = audio_list;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public ArrayList<MyAudio> getAudio_list() {
        return audio_list;
    }

    public void setAudio_list(ArrayList<MyAudio> audio_list) {
        this.audio_list = audio_list;
    }


}
