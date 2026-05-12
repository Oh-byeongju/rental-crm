package com.rental.domain.admin.repository;

import com.rental.domain.admin.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {

    /** 사용자의 모든 매핑 (UI 매트릭스 + 권한 판정용). */
    List<UserAuth> findByUserId(Long userId);

    /** 권한 판정용 — GRANT 또는 REVOKE 만 분리 조회. */
    @Query("""
        select ua.authCode from UserAuth ua
         where ua.userId    = :userId
           and ua.grantType = :grantType
        """)
    List<String> findAuthCodesByUserIdAndGrantType(@Param("userId") Long userId,
                                                    @Param("grantType") String grantType);

    /** 매트릭스 저장 시 전체 삭제 후 재삽입 패턴 (ADR-009 §2-3). */
    @Modifying
    @Query("delete from UserAuth ua where ua.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
