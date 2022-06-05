package xyz.lzf.ext.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import xyz.lzf.ext.cache.RedisCacheManager;
import xyz.lzf.ext.serializer.RedisObjectSerializer;
import xyz.lzf.ext.shiro.RedisSessionDao;
import xyz.lzf.ext.shiro.ShiroSessionFactory;

/**
 * configuration class
 * @author lzf
 */
@Configuration
@EnableConfigurationProperties({ShiroRedisProperties.class})
public class ShiroRedisConfiguration {

    /**
     * 参考RedisAutoConfiguration 直接引入redisConnectionFactory
     */
    @Bean(name = "shiroRedisTemplate")
    public RedisTemplate<String, Object> shiroRedisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
        template.setKeySerializer(new RedisObjectSerializer());
        template.setValueSerializer(new RedisObjectSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(ShiroRedisProperties properties,
                                          RedisTemplate<String, Object> shiroRedisTemplate){
        RedisCacheManager redisCacheManager = new RedisCacheManager(properties.getCacheKeyPrefix());
        redisCacheManager.setRedisTemplate(shiroRedisTemplate);
        return redisCacheManager;
    }

    @Bean
    public RedisSessionDao sessionDAO(ShiroRedisProperties properties,
                                      RedisTemplate<String, Object> shiroRedisTemplate){
        RedisSessionDao redisSessionDao = new RedisSessionDao(properties.getSessionKeyPrefix(), properties.getSessionExpired());
        redisSessionDao.setRedisTemplate(shiroRedisTemplate);
        return redisSessionDao;
    }

    @Bean
    public ShiroSessionFactory sessionFactory(){
        ShiroSessionFactory sessionFactory = new ShiroSessionFactory();
        return sessionFactory;
    }
}
