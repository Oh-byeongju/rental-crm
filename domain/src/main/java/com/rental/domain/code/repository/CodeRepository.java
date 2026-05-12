package com.rental.domain.code.repository;

import com.rental.domain.code.entity.Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CodeRepository extends JpaRepository<Code, Long> {

    boolean existsByGroupCodeAndCodeValue(String groupCode, String codeValue);

    Optional<Code> findByGroupCodeAndCodeValue(String groupCode, String codeValue);

    /**
     * 그룹 내 코드 정렬 조회 — IDX_CM_CODE_GROUP (GROUP_CODE, USE_YN, SORT_ORDER) 활용.
     * selectbox 옵션 채울 때 자주 사용.
     */
    @Query("""
        select c from Code c
         where c.groupCode = :groupCode
           and (:useYn is null or c.useYn = :useYn)
         order by c.sortOrder
        """)
    List<Code> findByGroupCode(@Param("groupCode") String groupCode,
                               @Param("useYn")     String useYn);

    /**
     * 그룹 내 검색 페이징.
     */
    @Query("""
        select c from Code c
         where c.groupCode = :groupCode
           and (:codeValue is null or c.codeValue like concat(:codeValue, '%'))
           and (:codeName  is null or c.codeName  like concat(:codeName,  '%'))
           and (:useYn     is null or c.useYn = :useYn)
         order by c.sortOrder
        """)
    Page<Code> search(@Param("groupCode") String groupCode,
                      @Param("codeValue") String codeValue,
                      @Param("codeName")  String codeName,
                      @Param("useYn")     String useYn,
                      Pageable pageable);

    long countByGroupCode(String groupCode);
}
