if(redis.call('get',KEYS[1])==ARGV[1]) then
    ---线程标识与锁标识一致才释放锁
    return redis.call('del',KEYS[1])
end
return 0