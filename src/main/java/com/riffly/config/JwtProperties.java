package com.riffly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "riffly.jwt")
public class JwtProperties {

    /** Base64-encoded HMAC-SHA256 secret key. */
    private String secret = "changeme-replace-in-production";

    /** Token lifetime in milliseconds. Default: 24 hours. */
    private long expirationMs = 86_400_000L;

    public String getSecret()               { return secret; }
    public void   setSecret(String s)       { this.secret = s; }
    public long   getExpirationMs()         { return expirationMs; }
    public void   setExpirationMs(long ms)  { this.expirationMs = ms; }
}
