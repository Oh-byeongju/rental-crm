package com.rental.backoffice.admin.service;

import com.rental.domain.auth.entity.Auth;
import com.rental.domain.auth.repository.AuthRepository;
import com.rental.backoffice.menu.dto.MenuTreeNode;
import com.rental.backoffice.menu.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 사이드바 동적 렌더링 — 사용자 권한 기반 메뉴 필터링.
 *
 * <p>판정 (ADR-008 §2-2): LEAF 메뉴 진입권 = 해당 메뉴의 `*_VIEW` 권한 보유.
 * GROUP 메뉴: 자식 중 하나라도 표시 가능하면 표시.
 *
 * <p>X-User-Id 시뮬레이션 미적용 (userId=null) 시 전체 트리 표시 — ADR-010 §2-8 정신.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SidebarService {

    private final MenuService menuService;
    private final AuthRepository authRepository;
    private final PermissionService permissionService;

    public List<MenuTreeNode> findVisibleMenuTree(Long userId) {
        List<MenuTreeNode> tree = menuService.findMenuTree("Y");

        if (userId == null) {
            // 비인증 / X-User-Id 없음 — 전체 메뉴 (개발·학습 편의)
            return tree;
        }

        Set<String> userAuths = permissionService.getEffectiveAuthCodes(userId);

        // 사용자가 가진 *_VIEW 권한의 MENU_ID 셋 = 진입권 보유 LEAF
        Set<Long> visibleLeafIds = authRepository.findAll().stream()
                .filter(a -> userAuths.contains(a.getAuthCode()))
                .filter(a -> "VIEW".equals(a.getAuthType()))
                .map(Auth::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return filterTree(tree, visibleLeafIds);
    }

    private List<MenuTreeNode> filterTree(List<MenuTreeNode> nodes, Set<Long> visibleLeafIds) {
        List<MenuTreeNode> result = new ArrayList<>();
        for (MenuTreeNode node : nodes) {
            if ("LEAF".equals(node.menuType())) {
                if (visibleLeafIds.contains(node.menuId())) {
                    result.add(node);
                }
            } else {
                // GROUP — children 필터링 후 비어있지 않으면 표시
                List<MenuTreeNode> visibleChildren = filterTree(node.children(), visibleLeafIds);
                if (!visibleChildren.isEmpty()) {
                    result.add(new MenuTreeNode(
                            node.menuId(), node.parentMenuId(), node.menuDepth(),
                            node.menuName(), node.menuType(), node.menuUrl(),
                            node.iconClass(), node.sortOrder(), node.useYn(),
                            visibleChildren
                    ));
                }
            }
        }
        return result;
    }
}
