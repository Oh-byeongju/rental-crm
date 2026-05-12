package com.rental.crm.notification.entity;

import com.rental.crm.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 — `CM_NOTIFICATION`. Ch.3 Kafka Consumer 가 INSERT 할 대상.
 *
 * <p>정책:
 * <ul>
 *   <li>RECIPIENT_USER_ID: NULL = 전체 관리자 broadcast. NOT NULL = 특정 사용자</li>
 *   <li>NOTIFICATION_TYPE: CM_CODE 그룹 NOTIFICATION_TYPE (BILLING_CREATED / PAYMENT_COMPLETED / PAYMENT_OVERDUE / VISIT_ASSIGNED)</li>
 *   <li>REF_TYPE / REF_ID: 참조 도메인 + ID (예: BILLING / 123). 알림 클릭 시 해당 상세 페이지 이동</li>
 *   <li>READ_YN: 기본 'N'. 사용자가 알림 클릭 시 'Y'</li>
 *   <li>WRK_RMK / 감사 9컬럼: 시계열 로그 특성상 과한 측면 있음 — ERD §메모 일관성 위해 적용</li>
 * </ul>
 */
@Entity
@Table(name = "CM_NOTIFICATION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseAuditEntity {

    public static final String READ_YES = "Y";
    public static final String READ_NO  = "N";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_cm_notification")
    @SequenceGenerator(name = "seq_cm_notification", sequenceName = "SEQ_CM_NOTIFICATION", allocationSize = 50)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    /** NULL = 전체 관리자 broadcast. */
    @Column(name = "RECIPIENT_USER_ID", updatable = false)
    private Long recipientUserId;

    @Column(name = "NOTIFICATION_TYPE", length = 50, nullable = false, updatable = false)
    private String notificationType;

    @Column(name = "MESSAGE", length = 1000, nullable = false, updatable = false)
    private String message;

    @Column(name = "REF_TYPE", length = 50, updatable = false)
    private String refType;

    @Column(name = "REF_ID", updatable = false)
    private Long refId;

    @Column(name = "READ_YN", length = 1, nullable = false)
    private String readYn;

    @Builder
    private Notification(Long recipientUserId, String notificationType, String message,
                         String refType, Long refId) {
        this.recipientUserId  = recipientUserId;
        this.notificationType = notificationType;
        this.message          = message;
        this.refType          = refType;
        this.refId            = refId;
        this.readYn           = READ_NO;
    }

    // ===== 도메인 행위 =====
    public void markRead() { this.readYn = READ_YES; }
    public boolean isUnread() { return READ_NO.equals(this.readYn); }
}
