package xyz.lzf.ext.shiro;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.UnknownSessionException;
import org.apache.shiro.session.mgt.eis.AbstractSessionDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author lzf
 */
public class RedisSessionDao extends AbstractSessionDAO {

    private static Logger logger = LoggerFactory.getLogger(RedisSessionDao.class);

    private RedisTemplate<String, Object> redisTemplate;

    private String keyPrefix = "shiro_redis_session:";
    private int expired = 60;

    public RedisSessionDao(String keyPrefix, int expired) {
        this.keyPrefix = keyPrefix;
        this.expired = expired;
    }

    @Override
    public void update(Session session) throws UnknownSessionException {
        if (session == null || session.getId() == null) {
            logger.error("session or session id is null");
            return;
        }
        if (session instanceof ShiroSession) {
            ShiroSession ss = (ShiroSession) session;
            if (!ss.isChanged()) {
                return;
            }
            ss.setChanged(false);
        }
        // 如果session已经无效(过期), 也可以不用更新

        String redisKey = getRedisKey(String.valueOf(session.getId()));
        redisTemplate.opsForValue().set(redisKey, session);
//            session.setTimeout(expired * 1000L); 利用redis缓存失效作为session过期
        redisTemplate.expire(redisKey, expired, TimeUnit.MINUTES);
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
        sessions = new HashSet<Session>();
        Set<String> keys = redisTemplate.keys(getRedisKey("*"));
        List<Object> objects = redisTemplate.opsForValue().multiGet(keys);
        for (Object object : objects) {
            sessions.add((Session) object);
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

    @Override
    protected Session doReadSession(Serializable sessionId) {
        if (sessionId == null) {
            logger.error("session id is null");
            return null;
        }
        return (Session)redisTemplate.opsForValue().get(getRedisKey((String) sessionId));
    }


    private String getOriginKey(String redisKey) {
        return redisKey.substring(keyPrefix.length());
    }

    private String getRedisKey(String originKey) {
        return keyPrefix + originKey;
    }

    public RedisTemplate<String, Object> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * SimpleSession只能通过JDK字节数组方式直接反序列化, 使用JSON会反序列化失败
     * 但考虑devtools不兼容jdk序列化, 所以另寻方案
     */
//    @Override
//    protected Session doReadSession(Serializable sessionId) {
//        if (sessionId == null) {
//            logger.error("session id is null");
//            return null;
//        }
//        Session session = null;
//        try {
//            String sessionJson = redisTemplate.opsForValue().get(getRedisKey((String) sessionId));
//            if(sessionJson == null){
//                return session;
//            }
//            // shiro 默认使用的是SimpleSession
//            JsonNode jsonNode = objectMapper.readTree(sessionJson);
//            session = new SimpleSession();
//            Class<SimpleSession> simpleSessionClass = SimpleSession.class;
//            Iterator<String> jsonIterator = jsonNode.fieldNames();
//            while (jsonIterator.hasNext()){
//                String fieldName = jsonIterator.next();
//                try {
//                    Field field = simpleSessionClass.getDeclaredField(fieldName);
//                    field.setAccessible(true);
//                    if(field.getType() == Map.class){
//                        JsonNode attributesNode = jsonNode.get(fieldName);
//                        // convert JsonNode to Map
//                        Map<Object, Object> attributeMap = objectMapper.convertValue(attributesNode, new TypeReference<HashMap<Object, Object>>() {});
//                        field.set(session, attributeMap);
//                    }else if(field.getType() == long.class){
//                        field.setLong(session, jsonNode.get(fieldName).asLong(0L));
//                    }else if(field.getType() == Date.class){
//                        if(jsonNode.hasNonNull(fieldName)){
//                            field.set(session, new Date(jsonNode.get(fieldName).asLong(0L)));
//                        }else{  // 重要, 否则被判断为过期shiro将进行删除
//                            field.set(session, null);
//                        }
//                    }else if(field.getType() == boolean.class){
//                        field.set(session, Boolean.valueOf(jsonNode.get(fieldName).asText("true")));
//                    }else{
//                        field.set(session, jsonNode.get(fieldName).asText(""));
//                    }
//                } catch (NoSuchFieldException | IllegalAccessException e) {
//                    // nothing to do
//                    // Json中有一些属性SimpleSession没有 直接异常跳过
//                }
//            }
////            session = objectMapper.readValue(sessionJson, SimpleSession.class); 无法反序列化
//        } catch (IOException e) {
//            logger.error("cannot read session json", e);
//        }
//        return session;
//    }
}
