package xyz.lzf.ext.config;

import org.apache.shiro.session.mgt.eis.SessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import xyz.lzf.ext.cache.RedisCacheManager;
import xyz.lzf.ext.shiro.RedisSessionDao;

@Configuration
@EnableConfigurationProperties({ShiroRedisProperties.class})
public class ShiroRedisConfiguration {

    @Bean
//    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisCacheManager cacheManager(ShiroRedisProperties properties,@Autowired StringRedisTemplate redisTemplate){
        return new RedisCacheManager(redisTemplate, properties.getCacheKeyPrefix());
    }

    @Bean
//    @ConditionalOnBean(StringRedisTemplate.class)
    public RedisSessionDao sessionDAO(ShiroRedisProperties properties,@Autowired StringRedisTemplate redisTemplate){
        return new RedisSessionDao(redisTemplate, properties.getSessionKeyPrefix(), properties.getSessionExpired());
    }
}
