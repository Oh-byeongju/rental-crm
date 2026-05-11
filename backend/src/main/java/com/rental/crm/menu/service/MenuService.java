package com.rental.crm.menu.service;

import com.rental.crm.common.exception.BusinessException;
import com.rental.crm.common.exception.ErrorCode;
import com.rental.crm.menu.dto.MenuCreateRequest;
import com.rental.crm.menu.dto.MenuResponse;
import com.rental.crm.menu.dto.MenuSearchRequest;
import com.rental.crm.menu.dto.MenuTreeNode;
import com.rental.crm.menu.dto.MenuUpdateRequest;
import com.rental.crm.menu.entity.Menu;
import com.rental.crm.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 메뉴 도메인 — 04 §1-1.
 *
 * <p>2-depth 트리. PARENT_MENU_ID NULL = 루트. 학습 단순화로 GROUP↔LEAF / parent 변경 비허용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    // ===================== Create =====================
    @Transactional
    public MenuResponse register(MenuCreateRequest req) {
        // GROUP 은 URL 없음 / LEAF 는 URL 필수
        if ("GROUP".equals(req.menuType()) && req.menuUrl() != null && !req.menuUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "GROUP 메뉴는 URL 을 가질 수 없습니다");
        }
        if ("LEAF".equals(req.menuType()) && (req.menuUrl() == null || req.menuUrl().isBlank())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "LEAF 메뉴는 URL 이 필수입니다");
        }

        int depth = 1;
        if (req.parentMenuId() != null) {
            Menu parent = menuRepository.findById(req.parentMenuId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                            "상위 메뉴 없음: " + req.parentMenuId()));
            if (!parent.isGroup()) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE, "상위 메뉴는 GROUP 타입이어야 합니다");
            }
            if (parent.getMenuDepth() >= 2) {
                throw new BusinessException(ErrorCode.BUSINESS_RULE, "2-depth 까지만 지원합니다");
            }
            depth = parent.getMenuDepth() + 1;
        }

        var menu = Menu.builder()
                .parentMenuId(req.parentMenuId())
                .menuDepth(depth)
                .menuName(req.menuName())
                .menuType(req.menuType())
                .menuUrl(req.menuUrl())
                .iconClass(req.iconClass())
                .sortOrder(req.sortOrder())
                .build();
        return MenuResponse.from(menuRepository.save(menu));
    }

    // ===================== Read =====================
    public Page<MenuResponse> search(MenuSearchRequest req, Pageable pageable) {
        return menuRepository.search(
                req.hasMenuName() ? req.menuName() : null,
                req.hasMenuType() ? req.menuType() : null,
                req.hasUseYn()    ? req.useYn()    : null,
                pageable
        ).map(MenuResponse::from);
    }

    public MenuResponse findById(Long menuId) {
        return MenuResponse.from(loadMenu(menuId));
    }

    /**
     * 전체 메뉴 트리 — 사이드바 / 권한 매트릭스 공통.
     * useYn=null 이면 비활성 포함 (관리 화면용), "Y" 면 사용중만 (사이드바용).
     */
    public List<MenuTreeNode> findMenuTree(String useYn) {
        List<Menu> flat = menuRepository.findAllForTree(useYn);
        Map<Long, MenuTreeNode> byId = new HashMap<>();
        List<MenuTreeNode> roots = new ArrayList<>();

        // 1차: 노드 생성
        for (Menu m : flat) {
            byId.put(m.getMenuId(), MenuTreeNode.of(m));
        }
        // 2차: 부모-자식 연결
        for (Menu m : flat) {
            MenuTreeNode node = byId.get(m.getMenuId());
            if (m.getParentMenuId() == null) {
                roots.add(node);
            } else {
                MenuTreeNode parent = byId.get(m.getParentMenuId());
                if (parent != null) {
                    parent.children().add(node);
                }
            }
        }
        return roots;
    }

    /** 등록 시 PARENT 선택용 — 사용중 GROUP 만. */
    public List<MenuResponse> findActiveGroups() {
        return menuRepository.findActiveGroups()
                .stream()
                .map(MenuResponse::from)
                .toList();
    }

    // ===================== Update =====================
    @Transactional
    public MenuResponse update(Long menuId, MenuUpdateRequest req) {
        var menu = loadMenu(menuId);
        // LEAF 는 URL 필수
        if (menu.isLeaf() && (req.menuUrl() == null || req.menuUrl().isBlank())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, "LEAF 메뉴는 URL 이 필수입니다");
        }
        menu.changeName(req.menuName());
        menu.changeUrl(menu.isGroup() ? null : req.menuUrl());
        menu.changeIcon(req.iconClass());
        menu.changeSortOrder(req.sortOrder());
        menu.changeUseYn(req.useYn());
        return MenuResponse.from(menu);
    }

    // ===================== Delete =====================
    @Transactional
    public void delete(Long menuId) {
        var menu = loadMenu(menuId);
        long childCount = menuRepository.countByParentMenuId(menuId);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "하위 메뉴 " + childCount + "건이 있어 삭제 불가. 자식 메뉴 먼저 삭제 또는 비활성화 권장.");
        }
        menuRepository.delete(menu);
    }

    // ===================== Private =====================
    private Menu loadMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "메뉴 없음: " + menuId));
    }
}
