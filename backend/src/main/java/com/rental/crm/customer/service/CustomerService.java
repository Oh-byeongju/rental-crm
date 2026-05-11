package com.rental.crm.customer.service;

import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.customer.dto.CustomerCreateRequest;
import com.rental.crm.customer.dto.CustomerResponse;
import com.rental.crm.customer.dto.CustomerSearchRequest;
import com.rental.crm.customer.dto.CustomerUpdateRequest;
import com.rental.crm.customer.entity.Customer;
import com.rental.crm.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final DateTimeFormatter NO_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    // ===== Create =====
    @Transactional
    public CustomerResponse register(CustomerCreateRequest req) {
        if (customerRepository.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.ALREADY_EXISTS, "이미 가입된 이메일입니다");
        }
        if (!"Y".equals(req.termsAgreeYn())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "약관 동의 필수");
        }

        var customer = Customer.builder()
                .customerNo(generateCustomerNo())
                .customerName(req.customerName())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .birthDate(req.birthDate())
                .addressZip(req.addressZip())
                .address(req.address())
                .termsAgreeYn(req.termsAgreeYn())
                .build();

        var saved = customerRepository.save(customer);
        return CustomerResponse.from(saved);
    }

    // ===== Read =====
    public Page<CustomerResponse> search(CustomerSearchRequest req, Pageable pageable) {
        return customerRepository.search(
                req.hasName()  ? req.name()  : null,
                req.hasPhone() ? req.phone() : null,
                req.hasEmail() ? req.email() : null,
                req.hasUseYn() ? req.useYn() : null,
                pageable
        ).map(CustomerResponse::from);
    }

    public CustomerResponse findById(Long customerId) {
        return CustomerResponse.from(loadCustomer(customerId));
    }

    // ===== Update =====
    @Transactional
    public CustomerResponse update(Long customerId, CustomerUpdateRequest req) {
        var customer = loadCustomer(customerId);
        // 도메인 메서드를 통해 상태 변경 (Setter 미노출 원칙)
        customer.changePhone(req.phone());
        customer.changeAddress(req.addressZip(), req.address());
        customer.changeUseYn(req.useYn());
        // TODO: 이름·생년월일 도메인 메서드 추가 시 호출
        return CustomerResponse.from(customer);
    }

    // ===== Delete (Soft) =====
    @Transactional
    public void deactivate(Long customerId) {
        var customer = loadCustomer(customerId);
        customer.deactivate();
    }

    // ===== Private =====
    private Customer loadCustomer(Long customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "고객을 찾을 수 없습니다: " + customerId));
    }

    /**
     * 고객번호 생성 — `CUST-YYYYMMDD-NNNNN` (학습 단순화).
     * 운영 환경에선 별도 시퀀스 (SEQ_CT_CUSTOMER_NO) 권장.
     */
    private String generateCustomerNo() {
        String date = LocalDate.now().format(NO_DATE_FMT);
        int suffix = ThreadLocalRandom.current().nextInt(100000);
        return "CUST-" + date + "-" + String.format("%05d", suffix);
    }
}
