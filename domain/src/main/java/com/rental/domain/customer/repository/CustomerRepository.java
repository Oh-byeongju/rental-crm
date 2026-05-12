package com.rental.domain.customer.repository;

import com.rental.domain.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmail(String email);

    Optional<Customer> findByCustomerNo(String customerNo);

    boolean existsByEmail(String email);

    boolean existsByCustomerNo(String customerNo);

    /**
     * 검색 조건 기반 페이징 조회.
     * 빈 조건은 null 로 전달 → 해당 WHERE 절 스킵 (`is null or like` 패턴).
     * 인덱스 활용:
     *  - IDX_CT_CUSTOMER_NAME : 이름 prefix LIKE
     *  - IDX_CT_CUSTOMER_PHONE : 연락처 검색
     */
    @Query("""
        select c from Customer c
         where (:name  is null or c.customerName like concat(:name,  '%'))
           and (:phone is null or c.phone        like concat('%', :phone, '%'))
           and (:email is null or c.email        like concat('%', :email, '%'))
           and (:useYn is null or c.useYn = :useYn)
        """)
    Page<Customer> search(@Param("name")  String name,
                          @Param("phone") String phone,
                          @Param("email") String email,
                          @Param("useYn") String useYn,
                          Pageable pageable);
}
