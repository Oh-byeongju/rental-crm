package com.rental.domain.code.repository;

import com.rental.domain.code.entity.CodeGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodeGroupRepository extends JpaRepository<CodeGroup, String> {

    /**
     * 검색 조건 기반 페이징 조회.
     * 빈 조건은 null 로 넘김 → 해당 WHERE 절 스킵.
     */
    @Query("""
        select g from CodeGroup g
         where (:groupCode is null or g.groupCode like concat(:groupCode, '%'))
           and (:groupName is null or g.groupName like concat(:groupName, '%'))
           and (:useYn     is null or g.useYn = :useYn)
        """)
    Page<CodeGroup> search(@Param("groupCode") String groupCode,
                           @Param("groupName") String groupName,
                           @Param("useYn")     String useYn,
                           Pageable pageable);
}
