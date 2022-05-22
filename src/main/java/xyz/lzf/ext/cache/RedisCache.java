package xyz.lzf.ext.cache;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;

/**
 * Shiro Cache using RedisTemplate ...
 * @author lzf
 */
public class RedisCache<K,V> implements Cache<K,V> {
    private Logger logger = LoggerFactory.getLogger(RedisCache.class);

    private StringRedisTemplate redisTemplate;
    // 统一前缀, 方便管理, 并可使泛型字符串化
    private String keyPrefix = "shiro_redis_cache:";
    private ObjectMapper objectMapper;

    public RedisCache(StringRedisTemplate redisTemplate) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("RedisTemplate argument cannot be null.");
        }
        objectMapper = new ObjectMapper();
        this.redisTemplate = redisTemplate;
    }
    // overloading...
    public RedisCache(StringRedisTemplate redisTemplate, String keyPrefix) {
        if (redisTemplate == null) {
            throw new IllegalArgumentException("RedisTemplate argument cannot be null.");
        }
        objectMapper = new ObjectMapper();
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
                String jsonValue = redisTemplate.opsForValue().get(getRedisKey((String)key));
                return objectMapper.readValue(jsonValue, new TypeReference<V>() {});
            }
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    @Override
    public V put(K key, V value) throws CacheException {
        logger.debug("向RedisCache缓存数据 key: [" + key + "]");
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(getRedisKey((String)key), jsonValue);
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
            redisTemplate.delete(getRedisKey((String)key));
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
            List<String> values = redisTemplate.opsForValue().multiGet(keys);
            if(values.size() == 0){
                return Collections.emptyList();
            }
            List<V> newValues = new ArrayList<>(values.size());
            for (String value : values) {
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
