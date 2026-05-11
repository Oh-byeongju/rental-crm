package com.rental.crm.admin.repository;

import com.rental.crm.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByUseYn(String useYn);

    /** 권한 판정용 — 사용자의 ROLE_ID 만. */
    @Query("select u.roleId from AdminUser u where u.userId = :userId")
    Optional<Long> findRoleIdByUserId(@Param("userId") Long userId);

    /** 캐시 invalidation 용 — 특정 역할 보유 사용자 ID 셋. */
    @Query("select u.userId from AdminUser u where u.roleId = :roleId")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);

    /** 검색 페이징. */
    @Query("""
        select u from AdminUser u
         where (:email    is null or u.email    like concat('%', :email, '%'))
           and (:userName is null or u.userName like concat('%', :userName, '%'))
           and (:useYn    is null or u.useYn = :useYn)
         order by u.userId
        """)
    Page<AdminUser> search(@Param("email")    String email,
                           @Param("userName") String userName,
                           @Param("useYn")    String useYn,
                           Pageable pageable);
}
