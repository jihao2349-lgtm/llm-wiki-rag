package com.jihao.aiwiki.domain.ingest.queue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vault 摄入锁测试。
 *
 * @author jihao
 * @date 2026/05/06
 */
class VaultIngestLockManagerTest {

    /**
     * 锁 key 必须符合 T3 Redis 约定。
     */
    @Test
    void shouldUseDocumentedRedisLockKey() {
        VaultIngestLockManager lockManager = lockManager();

        assertEquals("vault:7:ingest-lock", lockManager.lockKey(7L));
    }

    /**
     * 同一个 Vault 同时只允许一个持有者。
     */
    @Test
    void shouldAllowOnlyOneOwnerForSameVault() {
        VaultIngestLockManager lockManager = lockManager();

        assertTrue(lockManager.tryLock(1L, "worker-a"));
        assertFalse(lockManager.tryLock(1L, "worker-b"));
        lockManager.unlock(1L, "worker-a");
        assertTrue(lockManager.tryLock(1L, "worker-b"));
    }

    /**
     * 创建无 Redis 的锁管理器。
     *
     * @return 锁管理器
     */
    private VaultIngestLockManager lockManager() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        return new VaultIngestLockManager(beanFactory.getBeanProvider(StringRedisTemplate.class));
    }
}
