package com.rental.domain.product.repository;

import com.rental.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByProductCode(String productCode);

    /** 검색 페이징 — IDX_CT_PRODUCT_EQUIPMENT (EQUIPMENT_ID, USE_YN) 활용. */
    @Query("""
        select p from Product p
         where (:productCode is null or p.productCode like concat('%', :productCode, '%'))
           and (:equipmentId is null or p.equipmentId = :equipmentId)
           and (:productName is null or p.productName like concat('%', :productName, '%'))
           and (:useYn       is null or p.useYn = :useYn)
         order by p.productId
        """)
    Page<Product> search(@Param("productCode") String productCode,
                         @Param("equipmentId") Long equipmentId,
                         @Param("productName") String productName,
                         @Param("useYn")       String useYn,
                         Pageable pageable);
}
