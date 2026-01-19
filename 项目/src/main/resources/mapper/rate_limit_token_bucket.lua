-- Token bucket rate limiter
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillPerSecond = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local data = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])

if tokens == nil then
    tokens = capacity
    ts = now
end

local delta = math.max(0, now - ts)
local refill = (delta / 1000.0) * refillPerSecond
tokens = math.min(capacity, tokens + refill)

if tokens < requested then
    redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
    redis.call('PEXPIRE', key, math.ceil(1000 * capacity / refillPerSecond * 2))
    return 0
end

tokens = tokens - requested
redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', key, math.ceil(1000 * capacity / refillPerSecond * 2))
return 1
