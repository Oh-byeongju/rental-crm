package com.rental.domain.contract.repository;

import com.rental.domain.contract.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    boolean existsByContractNo(String contractNo);

    /**
     * 활성 계약 전체. Ch.1 청구 배치 입력. 5만 건까지 메모리 1차 로드 OK (~25MB).
     * 더 커지면 Pageable 또는 Stream 으로 전환 검토.
     */
    @Query("select c from Contract c where c.contractStatus = 'ACTIVE'")
    java.util.List<Contract> findAllActive();

    /**
     * 특정 장비를 사용하는 활성(ACTIVE) 계약 수.
     * 가용 수량 = 장비.STOCK_QTY − 이 카운트.
     * Native SQL — Contract↔Product 간 JPA 연관관계 매핑 없음.
     */
    @Query(value = """
        SELECT COUNT(*) FROM CT_CONTRACT c
          JOIN CT_PRODUCT p ON c.PRODUCT_ID = p.PRODUCT_ID
         WHERE p.EQUIPMENT_ID = :equipmentId
           AND c.CONTRACT_STATUS = 'ACTIVE'
        """, nativeQuery = true)
    long countActiveByEquipmentId(@Param("equipmentId") Long equipmentId);

    /** 검색 페이징. */
    @Query("""
        select c from Contract c
         where (:contractNo     is null or c.contractNo     like concat('%', :contractNo, '%'))
           and (:customerId     is null or c.customerId     = :customerId)
           and (:productId      is null or c.productId      = :productId)
           and (:contractStatus is null or c.contractStatus = :contractStatus)
         order by c.contractId desc
        """)
    Page<Contract> search(@Param("contractNo")     String contractNo,
                          @Param("customerId")     Long customerId,
                          @Param("productId")      Long productId,
                          @Param("contractStatus") String contractStatus,
                          Pageable pageable);
}
