package com.beaver.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    
    private String secret;
    private String accessTokenExpiration;
    private String refreshTokenExpiration;
    
    public String getSecret() {
        return secret;
    }
    
    public void setSecret(String secret) {
        this.secret = secret;
    }
    
    public String getAccessTokenExpiration() {
        return accessTokenExpiration;
    }
    
    public void setAccessTokenExpiration(String accessTokenExpiration) {
        this.accessTokenExpiration = accessTokenExpiration;
    }
    
    public String getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
    
    public void setRefreshTokenExpiration(String refreshTokenExpiration) {
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
    
    /**
     * Get access token expiration in milliseconds
     */
    public long getAccessTokenExpirationMs() {
        return parseExpirationToMs(accessTokenExpiration);
    }
    
    /**
     * Get refresh token expiration in milliseconds  
     */
    public long getRefreshTokenExpirationMs() {
        return parseExpirationToMs(refreshTokenExpiration);
    }
    
    private long parseExpirationToMs(String expiration) {
        if (expiration == null) {
            return 900000L; // 15 minutes default
        }
        
        // Support simple formats like "15m", "24h", or raw milliseconds
        if (expiration.endsWith("m")) {
            return Long.parseLong(expiration.substring(0, expiration.length() - 1)) * 60 * 1000;
        } else if (expiration.endsWith("h")) {
            return Long.parseLong(expiration.substring(0, expiration.length() - 1)) * 60 * 60 * 1000;
        } else {
            return Long.parseLong(expiration);
        }
    }
}