-- Sliding window rate limiter
local key = KEYS[1]
local seqKey = KEYS[2]
local windowMs = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)
local count = redis.call('ZCARD', key)
if count >= limit then
    return 0
end

local seq = redis.call('INCR', seqKey)
redis.call('ZADD', key, now, now .. '-' .. seq)
redis.call('PEXPIRE', key, windowMs * 2)
redis.call('PEXPIRE', seqKey, windowMs * 2)
return 1
