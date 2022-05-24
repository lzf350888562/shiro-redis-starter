package xyz.lzf.ext.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * properties class
 * @author lzf
 */
@ConfigurationProperties("shiro.redis")
public class ShiroRedisProperties {
    private int sessionExpired = 30;
    private String sessionKeyPrefix = "shiro_redis_session:";
    private String cacheKeyPrefix = "shiro_redis_cache:";

    public int getSessionExpired() {
        return sessionExpired;
    }

    public void setSessionExpired(int sessionExpired) {
        if(sessionExpired != 0){
            this.sessionExpired = sessionExpired;
        }
    }

    public String getSessionKeyPrefix() {
        return sessionKeyPrefix;
    }

    public void setSessionKeyPrefix(String sessionKeyPrefix) {
        if(sessionKeyPrefix != null && !"".equals(sessionKeyPrefix)){
            this.sessionKeyPrefix = sessionKeyPrefix;
        }
    }

    public String getCacheKeyPrefix() {
        return cacheKeyPrefix;
    }

    public void setCacheKeyPrefix(String cacheKeyPrefix) {
        if(cacheKeyPrefix != null && !"".equals(cacheKeyPrefix)){
            this.cacheKeyPrefix = cacheKeyPrefix;
        }
    }
}
