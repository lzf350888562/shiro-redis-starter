package xyz.lzf.ext.cache;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.mgt.SimpleSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shiro CacheManager. cache the data, such as realm,principal
 * @author lzf
 */
public class RedisCacheManager implements CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisCacheManager.class);

    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
    private RedisTemplate<String, Object> redisTemplate;
    private String keyPrefix;

    public RedisCacheManager(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name) throws CacheException {
        logger.debug("获取名称为: " + name + " 的RedisCache实例");
        Cache c = caches.get(name);
        if (c == null) {
            c = new RedisCache<K, V>(redisTemplate, keyPrefix);
            caches.put(name, c);
        }
        return c;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
}
