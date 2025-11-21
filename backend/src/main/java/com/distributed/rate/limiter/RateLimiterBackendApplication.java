package com.distributed.rate.limiter;

import com.distributed.rate.limiter.models.RateLimitRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@SpringBootApplication
@Log4j2
public class RateLimiterBackendApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RateLimiterBackendApplication.class, args);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultRedisScript<List> slidingWindowLuaScript()
    {
        DefaultRedisScript<List> defaultRedisScript = new DefaultRedisScript<>();
        defaultRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("sliding-window.lua")));
        defaultRedisScript.setResultType(List.class);
        return defaultRedisScript;
    }

    @Bean
    public List<RateLimitRule> populateRules(ObjectMapper objectMapper)
    {
        try
        {
            ClassPathResource resource = new ClassPathResource("rules.json");
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>()
            {
            });
        } catch (Exception exception)
        {
            log.error("There is an error reading rules.json", exception);
            throw new IllegalStateException("There is an error reading rules.json", exception);
        }
    }

}
