package com.rental.domain.engineer.repository;

import com.rental.domain.engineer.entity.Engineer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EngineerRepository extends JpaRepository<Engineer, Long> {

    boolean existsByEngineerCode(String engineerCode);

    /** 검색 페이징 — IDX_CT_ENGINEER_AREA (AREA, USE_YN) 활용. */
    @Query("""
        select e from Engineer e
         where (:engineerCode is null or e.engineerCode like concat('%', :engineerCode, '%'))
           and (:engineerName is null or e.engineerName like concat('%', :engineerName, '%'))
           and (:engineerType is null or e.engineerType = :engineerType)
           and (:area         is null or e.area         like concat('%', :area, '%'))
           and (:useYn        is null or e.useYn = :useYn)
         order by e.engineerId
        """)
    Page<Engineer> search(@Param("engineerCode") String engineerCode,
                          @Param("engineerName") String engineerName,
                          @Param("engineerType") String engineerType,
                          @Param("area")         String area,
                          @Param("useYn")        String useYn,
                          Pageable pageable);
}
