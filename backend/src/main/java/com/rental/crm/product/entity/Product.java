package com.rental.crm.product.entity;

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

/**
 * 렌탈 상품 마스터 — `CT_PRODUCT`.
 *
 * <p>정책:
 * <ul>
 *   <li>PRODUCT_CODE 는 UNIQUE 비즈니스 키 — 변경 가능 (EQUIPMENT_CODE 와 동일 정책 — DB FK 무결성 영향 없음)</li>
 *   <li>EQUIPMENT_ID: CT_EQUIPMENT FK — 존재 검증 필수. USE_YN 제약 없음 (단종 장비도 상품 등록/유지 가능)</li>
 *   <li>MONTHLY_FEE: 0 초과 (DB CHECK CK_CT_PRODUCT_FEE)</li>
 *   <li>CONTRACT_MONTHS: 1 이상 (DB CHECK CK_CT_PRODUCT_MONTHS)</li>
 *   <li>DEPOSIT_AMOUNT / INSTALL_FEE: 0 이상 (DEFAULT 0)</li>
 *   <li>비활성화: USE_YN='N' (소프트 삭제)</li>
 * </ul>
 */
@Entity
@Table(name = "CT_PRODUCT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_ct_product")
    @SequenceGenerator(name = "seq_ct_product", sequenceName = "SEQ_CT_PRODUCT", allocationSize = 50)
    @Column(name = "PRODUCT_ID")
    private Long productId;

    @Column(name = "PRODUCT_CODE", length = 20, nullable = false, unique = true)
    private String productCode;

    @Column(name = "EQUIPMENT_ID", nullable = false)
    private Long equipmentId;

    @Column(name = "PRODUCT_NAME", length = 200, nullable = false)
    private String productName;

    @Column(name = "MONTHLY_FEE", nullable = false)
    private Long monthlyFee;

    @Column(name = "CONTRACT_MONTHS", nullable = false)
    private Integer contractMonths;

    @Column(name = "DEPOSIT_AMOUNT", nullable = false)
    private Long depositAmount;

    @Column(name = "INSTALL_FEE", nullable = false)
    private Long installFee;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "USE_YN", length = 1, nullable = false)
    private String useYn;

    @Builder
    private Product(String productCode, Long equipmentId, String productName,
                    Long monthlyFee, Integer contractMonths,
                    Long depositAmount, Long installFee, String description) {
        this.productCode    = productCode;
        this.equipmentId    = equipmentId;
        this.productName    = productName;
        this.monthlyFee     = monthlyFee;
        this.contractMonths = contractMonths;
        this.depositAmount  = depositAmount == null ? 0L : depositAmount;
        this.installFee     = installFee == null ? 0L : installFee;
        this.description    = description;
        this.useYn          = "Y";
    }

    // ===== 도메인 행위 =====
    public void changeProductCode(String productCode)        { this.productCode    = productCode; }
    public void changeEquipmentId(Long equipmentId)          { this.equipmentId    = equipmentId; }
    public void changeProductName(String productName)        { this.productName    = productName; }
    public void changeMonthlyFee(Long monthlyFee)            { this.monthlyFee     = monthlyFee; }
    public void changeContractMonths(Integer contractMonths) { this.contractMonths = contractMonths; }
    public void changeDepositAmount(Long depositAmount)      { this.depositAmount  = depositAmount == null ? 0L : depositAmount; }
    public void changeInstallFee(Long installFee)            { this.installFee     = installFee == null ? 0L : installFee; }
    public void changeDescription(String description)        { this.description    = description; }
    public void changeUseYn(String useYn)                    { this.useYn          = useYn; }
    public void deactivate()                                 { this.useYn          = "N"; }
}
