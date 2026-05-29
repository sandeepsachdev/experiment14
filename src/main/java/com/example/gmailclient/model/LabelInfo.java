package com.example.gmailclient.model;

public class LabelInfo {

    private String id;
    private String name;
    private String type; // "system" or "user"

    public LabelInfo() {}

    public LabelInfo(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
