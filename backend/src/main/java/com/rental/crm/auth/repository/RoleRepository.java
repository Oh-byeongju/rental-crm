package com.rental.crm.auth.repository;

import com.rental.crm.auth.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCode(String roleCode);

    boolean existsByRoleCode(String roleCode);

    @Query("""
        select r from Role r
         where (:roleCode is null or r.roleCode like concat('%', :roleCode, '%'))
           and (:roleName is null or r.roleName like concat('%', :roleName, '%'))
           and (:useYn    is null or r.useYn = :useYn)
         order by r.roleId
        """)
    Page<Role> search(@Param("roleCode") String roleCode,
                      @Param("roleName") String roleName,
                      @Param("useYn")    String useYn,
                      Pageable pageable);
}
