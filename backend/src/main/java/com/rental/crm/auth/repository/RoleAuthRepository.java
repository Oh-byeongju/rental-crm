package com.rental.crm.auth.repository;

import com.rental.crm.auth.entity.RoleAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleAuthRepository extends JpaRepository<RoleAuth, Long> {

    /** 역할에 부여된 AUTH_CODE 목록 (매트릭스 화면 초기 체크 상태용). */
    @Query("select ra.authCode from RoleAuth ra where ra.roleId = :roleId order by ra.authCode")
    List<String> findAuthCodesByRoleId(@Param("roleId") Long roleId);

    /** 역할의 모든 매핑 일괄 삭제 — 매트릭스 저장 시 전체 삭제 → 재삽입. */
    @Modifying
    @Query("delete from RoleAuth ra where ra.roleId = :roleId")
    int deleteByRoleId(@Param("roleId") Long roleId);

    long countByRoleId(Long roleId);
}
