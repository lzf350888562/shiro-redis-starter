package xyz.lzf.ext.shiro;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class RedisSessionDao extends AbstractSessionDAO {
    private static Logger logger = LoggerFactory.getLogger(RedisSessionDao.class);

    private StringRedisTemplate redisTemplate;
    private ObjectMapper objectMapper;

    private String keyPrefix = "shiro_redis_session:";
    private int expired = 60;

    public RedisSessionDao(StringRedisTemplate redisTemplate, String keyPrefix, int expired) {
        this.redisTemplate = redisTemplate;
        this.keyPrefix = keyPrefix;
        this.expired = expired;
        this.objectMapper = new ObjectMapper();
    }


    @Override
    public void update(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            logger.error("session or session id is null");
            return;
        }
        try {
            session.setAttribute("a","b");
            String sessionJson = objectMapper.writeValueAsString(session);
            String redisKey = getRedisKey(String.valueOf(session.getId()));
            redisTemplate.opsForValue().set(redisKey, sessionJson);
            session.setTimeout(expired * 1000L);
            redisTemplate.expire(redisKey, expired, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            logger.error("cannot write session json value ", e);
            return;
        }
    }

    @Override
    public void delete(Session session) {
        if (session == null || session.getId() == null) {
            logger.error("session or session id is null");
            return;
        }
        redisTemplate.delete(getRedisKey(String.valueOf(session.getId())));
    }

    @Override
    public Collection<Session> getActiveSessions() {
        Set<Session> sessions = null;
        try {
            sessions = new HashSet<Session>();
            Set<String> keys = redisTemplate.keys(getRedisKey("*"));
            List<String> sessionJsons = redisTemplate.opsForValue().multiGet(keys);
            for (String sessionJson : sessionJsons) {
                Session session = objectMapper.readValue(sessionJson, Session.class);
                sessions.add(session);
            }
        } catch (IOException e) {
            logger.error("cannot read session json", e);
        }
        return sessions;
    }

    @Override
    protected Serializable doCreate(Session session) {
        Serializable sessionId = this.generateSessionId(session);
        this.assignSessionId(session, sessionId);
        this.update(session);
        return sessionId;
    }

    /**
     * SimpleSession只能通过JDK字节数组方式直接反序列化, 使用JSON会反序列化失败
     * 但考虑devtools不兼容jdk序列化, 所以另寻方案
     */
    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (sessionId == null) {
            logger.error("session id is null");
            return null;
        }
        Session session = null;
        try {
            String sessionJson = redisTemplate.opsForValue().get(getRedisKey((String) sessionId));
            if(sessionJson == null){
                return session;
            }
            // shiro 默认使用的是SimpleSession
            JsonNode jsonNode = objectMapper.readTree(sessionJson);
            session = new SimpleSession();
            Class<SimpleSession> simpleSessionClass = SimpleSession.class;
            Iterator<String> jsonIterator = jsonNode.fieldNames();
            while (jsonIterator.hasNext()){
                String fieldName = jsonIterator.next();
                try {
                    Field field = simpleSessionClass.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    if(field.getType() == Map.class){
                        JsonNode attributesNode = jsonNode.get(fieldName);
                        // convert JsonNode to Map
                        Map<Object, Object> attributeMap = objectMapper.convertValue(attributesNode, new TypeReference<HashMap<Object, Object>>() {});
                        field.set(session, attributeMap);
                    }else if(field.getType() == long.class){
                        field.setLong(session, jsonNode.get(fieldName).asLong(0L));
                    }else if(field.getType() == Date.class){
                        if(!"null".equals(jsonNode.get(fieldName))){
                            field.set(session, new Date(jsonNode.get(fieldName).asLong(0L)));
                        }
                    }else if(field.getType() == boolean.class){
                        field.set(session, Boolean.valueOf(jsonNode.get(fieldName).asText("true")));
                    }else{
                        field.set(session, jsonNode.get(fieldName).asText(""));
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    // nothing to do
                    // Json中有一些属性SimpleSession没有 直接异常跳过
                }
            }

//            session = objectMapper.readValue(sessionJson, SimpleSession.class); 无法反序列化
        } catch (IOException e) {
            logger.error("cannot read session json", e);
        }
        return session;
    }


    private String getOriginKey(String redisKey) {
        return redisKey.substring(keyPrefix.length());
    }

    private String getRedisKey(String originKey) {
        return keyPrefix + originKey;
    }
}
