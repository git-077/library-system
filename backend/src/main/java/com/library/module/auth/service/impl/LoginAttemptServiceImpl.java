package com.library.module.auth.service.impl;

import com.library.common.config.LoginSecurityProperties;
import com.library.common.exception.BizException;
import com.library.module.auth.service.LoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录防护的内存实现（单实例）。
 * <p>两个维度：<b>账号维度</b>防止盯住一个账号撞密码，<b>IP 维度</b>防止同一来源换账号刷。
 * 两者都失败才放行日志，避免日志被刷爆。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptServiceImpl implements LoginAttemptService {

    private static final long ONE_MINUTE = 60_000L;

    private final LoginSecurityProperties props;

    /** key = 用户名，value = 失败次数与锁定截止时间 */
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    /** key = IP，value = 最近一分钟内的登录请求时间戳 */
    private final Map<String, Deque<Long>> ipHits = new ConcurrentHashMap<>();

    private static final class Attempt {
        private int fails;
        private long lockUntil;
    }

    @Override
    public void ensureLoginAllowed(String username, String ip) {
        long now = System.currentTimeMillis();

        Attempt attempt = attempts.get(username);
        if (attempt != null) {
            if (attempt.lockUntil > now) {
                long minutes = Math.max(1, (attempt.lockUntil - now + ONE_MINUTE - 1) / ONE_MINUTE);
                throw new BizException(429, "密码错误次数过多，账号已临时锁定，请 " + minutes + " 分钟后再试");
            }
            // 锁定期已过：清掉计数，给用户重新尝试的机会
            if (attempt.lockUntil > 0) {
                attempts.remove(username);
            }
        }

        if (isIpLimited(ip, now)) {
            throw new BizException(429, "登录尝试过于频繁，请稍后再试");
        }
    }

    @Override
    public int recordFailure(String username, String ip) {
        long now = System.currentTimeMillis();
        Attempt attempt = attempts.computeIfAbsent(username, k -> new Attempt());
        synchronized (attempt) {
            attempt.fails++;
            if (attempt.fails >= props.getMaxFail()) {
                attempt.lockUntil = now + props.getLockMinutes() * ONE_MINUTE;
                log.warn("登录失败次数达到上限，账号锁定 username=[{}] ip=[{}] 时长={}分钟",
                        username, ip, props.getLockMinutes());
                return 0;
            }
            return props.getMaxFail() - attempt.fails;
        }
    }

    @Override
    public void recordSuccess(String username) {
        attempts.remove(username);
    }

    /** 滑动窗口限流：只保留最近一分钟的请求，超过阈值即拒绝 */
    private boolean isIpLimited(String ip, long now) {
        purgeExpired(now);
        Deque<Long> hits = ipHits.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && now - hits.peekFirst() > ONE_MINUTE) {
                hits.pollFirst();
            }
            if (hits.size() >= props.getIpMaxPerMinute()) {
                log.warn("登录请求过于频繁 ip=[{}] 一分钟内={}次", ip, hits.size());
                return true;
            }
            hits.addLast(now);
            return false;
        }
    }

    /** 顺手清理过期数据，避免长期运行后 Map 无限增长 */
    private void purgeExpired(long now) {
        if (attempts.size() > 512) {
            attempts.entrySet().removeIf(e ->
                    e.getValue().lockUntil > 0 && e.getValue().lockUntil < now);
        }
        if (ipHits.size() > 512) {
            ipHits.entrySet().removeIf(e -> {
                Deque<Long> d = e.getValue();
                synchronized (d) {
                    return d.isEmpty() || now - d.peekLast() > ONE_MINUTE;
                }
            });
        }
    }
}
