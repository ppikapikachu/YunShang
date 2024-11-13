package com.example.audiorecord.entity;

public class EventMsg {
    int id;
    boolean status;
    Object data;

    public EventMsg(int id, Object data) {
        this.id = id;
        this.data = data;
    }

    public EventMsg(int id, boolean status, Object data) {
        this.id = id;
        this.status = status;
        this.data = data;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "EventMsg{" +
                "id=" + id +
                ", status=" + status +
                ", data=" + data +
                '}';
    }
}
