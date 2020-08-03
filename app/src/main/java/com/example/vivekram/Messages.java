package com.example.vivekram;

public class Messages {
    private String from, type, message;

    public Messages(){}

    public Messages(String from, String type, String message) {
        this.from = from;
        this.type = type;
        this.message = message;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
