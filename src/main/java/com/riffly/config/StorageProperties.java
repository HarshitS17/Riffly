package com.riffly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.List;

@ConfigurationProperties(prefix = "riffly.storage")
public class StorageProperties {

    private String musicDir = "/var/riffly/music";
    private List<String> allowedExtensions = List.of("mp3", "flac", "wav", "ogg", "aac", "m4a");

    public String getMusicDir()                               { return musicDir; }
    public void   setMusicDir(String musicDir)                { this.musicDir = musicDir; }
    public List<String> getAllowedExtensions()                 { return allowedExtensions; }
    public void setAllowedExtensions(List<String> extensions) { this.allowedExtensions = extensions; }
}
