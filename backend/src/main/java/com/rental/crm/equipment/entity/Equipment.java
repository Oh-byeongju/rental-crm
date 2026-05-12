package com.rental.crm.equipment.entity;

import com.rental.crm.common.entity.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 렌탈 장비 마스터 — `CT_EQUIPMENT`.
 *
 * <p>정책:
 * <ul>
 *   <li>EQUIPMENT_CODE 는 UNIQUE 비즈니스 키 — 변경 가능 (DB FK 무결성 영향 없음 — 시스템 PK 는 EQUIPMENT_ID 시퀀스)</li>
 *   <li>UNIQUE: EQUIPMENT_CODE / (MODEL_NAME, MANUFACTURER)</li>
 *   <li>EQUIPMENT_TYPE: CM_CODE 그룹 EQUIPMENT_TYPE 의 사용중 코드값만 허용</li>
 *   <li>STOCK_QTY: 총 보유 수량 (DEFAULT 0, NOT NULL). 가용 수량은 활성 계약 수 차감 후 동적 계산 (계약 도메인 작업 시 적용)</li>
 *   <li>비활성화: USE_YN='N' (소프트 삭제, 단종 의미)</li>
 * </ul>
 */
@Entity
@Table(name = "CT_EQUIPMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Equipment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_equipment")
    @SequenceGenerator(name = "seq_ct_equipment", sequenceName = "SEQ_CT_EQUIPMENT", allocationSize = 50)
    @Column(name = "EQUIPMENT_ID")
    private Long equipmentId;

    @Column(name = "EQUIPMENT_CODE", length = 20, nullable = false, unique = true)
    private String equipmentCode;

    @Column(name = "EQUIPMENT_TYPE", length = 50, nullable = false)
    private String equipmentType;

    @Column(name = "MODEL_NAME", length = 200, nullable = false)
    private String modelName;

    @Column(name = "MANUFACTURER", length = 200, nullable = false)
    private String manufacturer;

    @Column(name = "RELEASE_DATE")
    private LocalDate releaseDate;

    @Column(name = "IMAGE_URL", length = 500)
    private String imageUrl;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "STOCK_QTY", nullable = false)
    private Integer stockQty;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Equipment(String equipmentCode, String equipmentType,
                      String modelName, String manufacturer,
                      LocalDate releaseDate, String imageUrl, String description,
                      Integer stockQty) {
        this.equipmentCode  = equipmentCode;
        this.equipmentType  = equipmentType;
        this.modelName      = modelName;
        this.manufacturer   = manufacturer;
        this.releaseDate    = releaseDate;
        this.imageUrl       = imageUrl;
        this.description    = description;
        this.stockQty       = stockQty == null ? 0 : stockQty;
        this.useYn          = "Y";
    }

    // ===== 도메인 행위 =====
    public void changeEquipmentCode(String equipmentCode) { this.equipmentCode = equipmentCode; }
    public void changeEquipmentType(String equipmentType) { this.equipmentType = equipmentType; }
    public void changeModelName(String modelName)         { this.modelName     = modelName; }
    public void changeManufacturer(String manufacturer)   { this.manufacturer  = manufacturer; }
    public void changeReleaseDate(LocalDate releaseDate)  { this.releaseDate   = releaseDate; }
    public void changeImageUrl(String imageUrl)           { this.imageUrl      = imageUrl; }
    public void changeDescription(String description)     { this.description   = description; }
    public void changeStockQty(Integer stockQty)          { this.stockQty      = stockQty == null ? 0 : stockQty; }
    public void changeUseYn(String useYn)                 { this.useYn         = useYn; }
    public void deactivate()                              { this.useYn         = "N"; }
}
