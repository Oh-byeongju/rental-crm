package com.rental.crm.common.audit;

/**
 * 감사 컬럼 자동 주입을 위한 ThreadLocal 컨텍스트.
 * HTTP 요청 진입 시 {@link AuditContextFilter} 가 set, 종료 시 clear.
 * 비인증/배치 컨텍스트에서 호출 시 {@link AuditInfo#defaults()} 반환.
 *
 * @see com.rental.crm.common.entity.BaseAuditEntity
 * @see <a href="../../../../../../../../../guide/decisions/ADR-005-audit-columns-auto-injection.md">ADR-005</a>
 */
public final class AuditContext {

    private static final ThreadLocal<AuditInfo> CONTEXT = new ThreadLocal<>();

    private AuditContext() {}

    public static AuditInfo current() {
        var info = CONTEXT.get();
        return info != null ? info : AuditInfo.defaults();
    }

    public static void set(AuditInfo info) {
        CONTEXT.set(info);
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record AuditInfo(String pgmId, String ip) {
        public static AuditInfo defaults() {
            return new AuditInfo("SYSTEM", "127.0.0.1");
        }
    }
}
