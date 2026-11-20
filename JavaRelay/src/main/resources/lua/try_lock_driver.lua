-- KEYS[1] = service:driver:info:<id>   （String，内容是 JSON）
-- ARGV[1] = 期望旧状态，如 "0"
-- ARGV[2] = 目标新状态，如 "1"
local raw = redis.call('GET', KEYS[1])
if not raw then
    return 0                       -- 缓存不存在，抢不到
end

local ok, obj = pcall(cjson.decode, raw)
if not ok or type(obj) ~= 'table' then
    return 0                       -- 不是合法 JSON，直接放弃
end

local cur = obj['status']
if cur == nil or tostring(cur) ~= ARGV[1] then
    return 0                       -- 状态不是期望值（已被别人抢走）
end

obj['status'] = tonumber(ARGV[2])  -- 写回保持数字类型
redis.call('SET', KEYS[1], cjson.encode(obj))
return 1