package com.rental.crm.notification.repository;

import com.rental.crm.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 사용자별 미읽음 카운트 — 헤더 알림 뱃지 표시용.
     * NULL recipient (broadcast) 포함. IDX_CM_NOTIFICATION_UNREAD (READ_YN, CREATED_AT DESC) 활용.
     */
    @Query("""
        select count(n) from Notification n
         where n.readYn = 'N'
           and (n.recipientUserId is null or n.recipientUserId = :userId)
        """)
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * 사용자별 알림 목록 — broadcast 포함.
     * READ_YN / NOTIFICATION_TYPE 필터 + 최신순.
     */
    @Query("""
        select n from Notification n
         where (n.recipientUserId is null or n.recipientUserId = :userId)
           and (:readYn           is null or n.readYn = :readYn)
           and (:notificationType is null or n.notificationType = :notificationType)
         order by n.notificationId desc
        """)
    Page<Notification> findByUserId(@Param("userId")           Long userId,
                                    @Param("readYn")           String readYn,
                                    @Param("notificationType") String notificationType,
                                    Pageable pageable);

    /** 사용자별 전체 미읽음 → 일괄 읽음 처리. broadcast 도 포함. */
    @Modifying
    @Query("""
        update Notification n set n.readYn = 'Y'
         where n.readYn = 'N'
           and (n.recipientUserId is null or n.recipientUserId = :userId)
        """)
    int markAllReadByUserId(@Param("userId") Long userId);
}
