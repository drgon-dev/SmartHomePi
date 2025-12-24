package com.example.myapplication;

class DataContainer {
    private String type;
    private Object data;
    private long timestamp;
    private String version = "1.0";

    public DataContainer(String type, Object data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }


    public String getType() { return type; }
    public Object getData() { return data; }
    public long getTimestamp() { return timestamp; }
    public String getVersion() { return version; }
}
