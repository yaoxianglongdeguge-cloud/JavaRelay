-- KEYS[1]: 司机状态的 Hash Key，比如 "driver:info:1001"
-- ARGV[1]: Hash 里的字段名，固定为 "status"
-- ARGV[2]: 期望的旧状态
-- ARGV[3]: 目标新状态

-- 1. 从 Hash 中获取该司机的当前状态
local currentStatus = redis.call('HGET', KEYS[1], ARGV[1])

if currentStatus ~= ARGV[2] then
    return 0
end

redis.call('HSET', KEYS[1], ARGV[1], ARGV[3])
return 1