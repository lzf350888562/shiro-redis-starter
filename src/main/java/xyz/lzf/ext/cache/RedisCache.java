package xyz.lzf.ext.cache;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;

/**
 * Shiro Cache using RedisTemplate ...
 * @author lzf
 */
public class RedisCache<K,V> implements Cache<K,V> {
    private Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private RedisTemplate<String, Object> redisTemplate;
    // 统一前缀, 方便管理, 并可使泛型字符串化
    private String keyPrefix = "shiro_redis_cache:";

    public RedisCache(RedisTemplate<String, Object> redisTemplate) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("RedisTemplate argument cannot be null.");
        }
        this.redisTemplate = redisTemplate;
    }
    // overloading...
    public RedisCache(RedisTemplate<String, Object> redisTemplate, String keyPrefix) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("RedisTemplate argument cannot be null.");
        }
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    @Override
    public V get(K key) throws CacheException {
        logger.debug("从RedisCache中获取数据 key: [" + key + "]");
        try {
            if (key == null) {
                return null;
            }else{
                // 注意: 这里获取字符串key不能强转, 因为shiro将对象作为key
                return (V)redisTemplate.opsForValue().get(getRedisKey(key.toString()));

            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public V put(K key, V value) throws CacheException {
        logger.debug("向RedisCache缓存数据 key: [" + key + "]");
        try {
            redisTemplate.opsForValue().set(getRedisKey(key.toString()), value);
            return value;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public V remove(K key) throws CacheException {
        logger.debug("从RedisCache中删除 key: [" + key + "]");
        try {
            // 先获取, 删除时返回删除的值
            V previous = get(key);
            redisTemplate.delete(getRedisKey(key.toString()));
            return previous;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public void clear() throws CacheException {
        logger.debug("从RedisCache中删除所有相关元素");
        try {
            Set<String> keys = redisTemplate.keys(getRedisKey("*"));
            if(keys.size() != 0){
                redisTemplate.delete(keys);
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public int size() {
        logger.debug("从RedisCache中统计相关元素个数");
        try {
            Set<String> keys = redisTemplate.keys(getRedisKey("*"));
            return keys.size();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public Set<K> keys() {
        logger.debug("从RedisCache中获取所有key");
        try {
            Set<String> keys = redisTemplate.keys(keyPrefix + "*");
            Set<K> newKeys = new HashSet<K>(keys.size());
            for (String key : keys) {
                newKeys.add((K)getOriginKey(key));
            }
            return newKeys;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public Collection<V> values() {
        logger.debug("从RedisCache中获取所有value");
        try {
            Set<String> keys = redisTemplate.keys(getRedisKey("*"));
            List<Object> values = redisTemplate.opsForValue().multiGet(keys);
            if(values.size() == 0){
                return Collections.emptyList();
            }
            List<V> newValues = new ArrayList<>(values.size());
            for (Object value : values) {
                newValues.add((V)value);
            }
            return newValues;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    private String getOriginKey(String redisKey){
        return redisKey.substring(keyPrefix.length());
    }

    private String getRedisKey(String originKey){
        return keyPrefix + originKey;
    }
}
