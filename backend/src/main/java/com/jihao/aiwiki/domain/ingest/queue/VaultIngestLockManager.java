package com.jihao.aiwiki.domain.ingest.queue;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vault 级摄入锁管理器。
 *
 * @author jihao
 * @date 2026/05/06
 */
@Component
public class VaultIngestLockManager {

    /** 锁过期时间 */
    private static final Duration LOCK_TTL = Duration.ofMinutes(10);

    /** Redis 客户端 */
    private final StringRedisTemplate redisTemplate;

    /** 无 Redis 时的本地锁兜底 */
    private final Map<Long, LocalLock> localLocks = new ConcurrentHashMap<>();

    /**
     * 创建 Vault 锁管理器。
     *
     * @param redisTemplateProvider Redis 客户端 Provider
     */
    public VaultIngestLockManager(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    /**
     * 尝试获取 Vault 摄入锁。
     *
     * @param vaultId Vault ID
     * @param owner 锁持有者
     * @return 获取成功返回 true
     */
    public boolean tryLock(Long vaultId, String owner) {
        if (redisTemplate != null) {
            Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey(vaultId), owner, LOCK_TTL);
            return Boolean.TRUE.equals(locked);
        }
        Instant expiresAt = Instant.now().plus(LOCK_TTL);
        return localLocks.compute(vaultId, (key, current) -> {
            if (current == null || current.expiresAt().isBefore(Instant.now())) {
                return new LocalLock(owner, expiresAt);
            }
            return current;
        }).owner().equals(owner);
    }

    /**
     * 释放 Vault 摄入锁。
     *
     * @param vaultId Vault ID
     * @param owner 锁持有者
     */
    public void unlock(Long vaultId, String owner) {
        if (redisTemplate != null) {
            String key = lockKey(vaultId);
            if (Objects.equals(redisTemplate.opsForValue().get(key), owner)) {
                redisTemplate.delete(key);
            }
            return;
        }
        localLocks.computeIfPresent(vaultId, (key, current) -> Objects.equals(current.owner(), owner) ? null : current);
    }

    /**
     * 构造 Redis 锁 key。
     *
     * @param vaultId Vault ID
     * @return Redis 锁 key
     */
    public String lockKey(Long vaultId) {
        return "vault:" + vaultId + ":ingest-lock";
    }

    /**
     * 本地锁值。
     *
     * @param owner 持有者
     * @param expiresAt 过期时间
     */
    private record LocalLock(String owner, Instant expiresAt) {
    }
}
