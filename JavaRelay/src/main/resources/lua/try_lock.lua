-- KEYS[1] = lock:xxx
-- ARGV[1] = token
-- ARGV[2] = 过期秒数
if redis.call('SET', KEYS[1], ARGV[1], 'NX', 'EX', ARGV[2]) then
    return 1
end
return 0