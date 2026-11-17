package com.library.module.auth.service;

/**
 * 登录防护：失败次数锁定 + IP 限流。
 * <p>为什么不做成拦截器：登录是唯一「匿名可写」的入口，防护必须和
 * 具体业务判断（账号是否存在、密码是否匹配）绑在一起，放在 Service 里才可控；
 * 拦截器只能看到 URL，无法区分「密码错」和「账号被禁用」，容易误锁正常用户。
 * <p>状态存内存（单实例足够）；将来多实例部署时把实现换成 Redis 即可，接口不变。
 */
public interface LoginAttemptService {

    /**
     * 登录前置检查：账号是否处于锁定期、该 IP 是否请求过于频繁。
     *
     * @throws com.library.common.exception.BizException 429 表示需要等待
     */
    void ensureLoginAllowed(String username, String ip);

    /**
     * 记录一次密码错误。
     *
     * @return 剩余可尝试次数；0 表示本次已触发锁定
     */
    int recordFailure(String username, String ip);

    /** 登录成功后清空该账号的失败计数（IP 频率窗口不清，防止用成功登录绕过限流） */
    void recordSuccess(String username);
}
