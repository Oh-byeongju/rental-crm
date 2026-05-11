package com.rental.crm.auth.service;

import com.rental.crm.auth.dto.AuthResponse;
import com.rental.crm.auth.repository.AuthRepository;
import com.rental.crm.menu.entity.Menu;
import com.rental.crm.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 권한 키 마스터 조회 전담 (ADR-008).
 * 본 사이클에서는 CRUD 없음 — 시드 데이터로 운영.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthRepository authRepository;
    private final MenuRepository menuRepository;

    /**
     * 매트릭스 화면용 — 사용중 AUTH 전체 + 메뉴명 매핑.
     * Menu 와 Auth 는 다른 패키지라 JPQL join 대신 두 쿼리 + 자바 매핑.
     */
    public List<AuthResponse> findAllForMatrix() {
        var auths = authRepository.findByUseYnOrderByMenuIdAscSortOrderAsc("Y");

        Map<Long, String> menuNameById = menuRepository.findAll().stream()
                .collect(Collectors.toMap(Menu::getMenuId, Menu::getMenuName, (a, b) -> a));

        return auths.stream()
                .map(a -> AuthResponse.from(a, menuNameById.get(a.getMenuId())))
                .toList();
    }
}
