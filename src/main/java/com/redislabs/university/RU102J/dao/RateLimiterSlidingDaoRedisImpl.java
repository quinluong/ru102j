package com.redislabs.university.RU102J.dao;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

public class RateLimiterSlidingDaoRedisImpl implements RateLimiter {

    private final JedisPool jedisPool;
    private final long windowSizeMS; // in milli
    private final long maxHits;

    public RateLimiterSlidingDaoRedisImpl(JedisPool pool, long windowSizeMS,
                                          long maxHits) {
        this.jedisPool = pool;
        this.windowSizeMS = windowSizeMS;
        this.maxHits = maxHits;
    }

    // Challenge #7
    @Override
    public void hit(String name) throws RateLimitExceededException {
        // START CHALLENGE #7
        // END CHALLENGE #7
        
        String key = RedisSchema.getRateLimiterSlidingKey(windowSizeMS, name, maxHits);
        Instant instant = ZonedDateTime.now().toInstant();
        long currTimestampInMilli = instant.toEpochMilli();
        long milestoneTimestampInMilli = currTimestampInMilli - windowSizeMS;
        String randomNumber = UUID.randomUUID().toString();
        
        try (Jedis jedis = jedisPool.getResource()) {
            Transaction multi = jedis.multi();
            
            multi.zadd(
                    key,
                    currTimestampInMilli,
                    randomNumber
            );
            multi.zremrangeByScore(key, 0, milestoneTimestampInMilli);
            Response<Long> hits = multi.zcard(key);
            
            multi.exec();
            
            if (hits.get() > maxHits) {
                throw new RateLimitExceededException();
            }
        }
    }
}
