package com.beaver.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("jwt")
public class JwtConfig {
    private String secret;
    private long accessTokenValidity;
    private long refreshTokenValidity;
    private boolean authDisabled;

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setAccessTokenValidity(long accessTokenValidity) {
        this.accessTokenValidity = accessTokenValidity;
    }

    public void setRefreshTokenValidity(long refreshTokenValidity) {
        this.refreshTokenValidity = refreshTokenValidity;
    }

    // Keep the old setter for backward compatibility
    public void setValidity(long validity) {
        this.accessTokenValidity = validity;
    }

    public String getSecret() {
        return secret;
    }

    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }

    // Keep the old getter for backward compatibility
    public long getValidity() {
        return accessTokenValidity;
    }

    public boolean isAuthDisabled() {
        return authDisabled;
    }

    public void setAuthDisabled(boolean authDisabled) {
        this.authDisabled = authDisabled;
    }
}