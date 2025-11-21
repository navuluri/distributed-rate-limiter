-- KEYS[1] = Redis key for sliding window (e.g., "rate:user123")
-- ARGV[1] = current timestamp (milliseconds)
-- ARGV[2] = window size (milliseconds)
-- ARGV[3] = limit

local key       = KEYS[1]
local now       = tonumber(ARGV[1])
local window    = tonumber(ARGV[2])
local limit     = tonumber(ARGV[3])

-- 1. Remove all entries older than the sliding window
local min = now - window
redis.call("ZREMRANGEBYSCORE", key, 0, min)

-- 2. Insert the current timestamp with unique member
--    Use timestamp-random to avoid duplicate members for same millis
local member = tostring(now) .. "-" .. tostring(math.random(1000000))
redis.call("ZADD", key, now, member)

-- 3. Count remaining entries inside the window
local count = redis.call("ZCOUNT", key, min, now)

-- 4. Set TTL so key expires if not used
redis.call("PEXPIRE", key, window)

-- 5. Determine the earliest entry in the window → used to compute reset time
local earliest = redis.call("ZRANGE", key, 0, 0, "WITHSCORES")

local resetTime = 0
if earliest ~= nil and #earliest > 0 then
    local earliestScore = tonumber(earliest[2])
    resetTime = earliestScore + window
else
    resetTime = now + window
end

-- 6. Return results
--    1) current count
--    2) remaining tokens
--    3) reset timestamp (epoch millis)
return { count, limit - count, resetTime }
