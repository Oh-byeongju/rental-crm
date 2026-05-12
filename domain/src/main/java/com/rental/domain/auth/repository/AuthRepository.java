package com.rental.domain.auth.repository;

import com.rental.domain.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthRepository extends JpaRepository<Auth, String> {

    /**
     * 매트릭스 화면용 — 사용중(Y) 전체, 메뉴 ID + sortOrder 순.
     */
    List<Auth> findByUseYnOrderByMenuIdAscSortOrderAsc(String useYn);
}
