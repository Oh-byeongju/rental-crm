package com.rental.crm.equipment.repository;

import com.rental.crm.equipment.entity.Equipment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    boolean existsByEquipmentCode(String equipmentCode);

    boolean existsByModelNameAndManufacturer(String modelName, String manufacturer);

    /**
     * 검색 페이징.
     * stockFilter: null=전체, "AVAILABLE"=재고 있음(>0), "OUT_OF_STOCK"=재고 없음(=0)
     */
    @Query("""
        select e from Equipment e
         where (:equipmentCode is null or e.equipmentCode like concat('%', :equipmentCode, '%'))
           and (:equipmentType is null or e.equipmentType = :equipmentType)
           and (:modelName     is null or e.modelName     like concat('%', :modelName, '%'))
           and (:manufacturer  is null or e.manufacturer  like concat('%', :manufacturer, '%'))
           and (:useYn         is null or e.useYn = :useYn)
           and (:stockFilter   is null
                or (:stockFilter = 'AVAILABLE'    and e.stockQty > 0)
                or (:stockFilter = 'OUT_OF_STOCK' and e.stockQty = 0))
         order by e.equipmentId
        """)
    Page<Equipment> search(@Param("equipmentCode") String equipmentCode,
                           @Param("equipmentType") String equipmentType,
                           @Param("modelName")     String modelName,
                           @Param("manufacturer")  String manufacturer,
                           @Param("useYn")         String useYn,
                           @Param("stockFilter")   String stockFilter,
                           Pageable pageable);
}
