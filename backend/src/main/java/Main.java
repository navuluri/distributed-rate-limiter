import com.distributed.rate.limiter.models.RateLimitRules;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class Main
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("Hello, World!");
        ObjectMapper objectMapper = new ObjectMapper();
        RateLimitRules rateLimitRules =  new RateLimitRules();
        List<RateLimitRules> rateLimitRulesList = new ArrayList<>();

    }
}
