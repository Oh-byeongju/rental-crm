package com.rental.domain.menu.repository;

import com.rental.domain.menu.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    long countByParentMenuId(Long parentMenuId);

    /**
     * 트리 조회용 — 전체 (또는 USE_YN 필터) + SORT_ORDER 순.
     * 자바 측에서 PARENT_MENU_ID 기준 그룹핑하여 트리 빌드.
     */
    @Query("""
        select m from Menu m
         where (:useYn is null or m.useYn = :useYn)
         order by m.menuDepth, m.sortOrder, m.menuId
        """)
    List<Menu> findAllForTree(@Param("useYn") String useYn);

    /**
     * GROUP 메뉴 옵션 (등록 시 PARENT_MENU_ID selectbox).
     */
    @Query("""
        select m from Menu m
         where m.menuType = 'GROUP'
           and m.useYn = 'Y'
         order by m.sortOrder, m.menuId
        """)
    List<Menu> findActiveGroups();

    /**
     * 평탄 검색 페이징 — 메뉴 관리 그리드용.
     */
    @Query("""
        select m from Menu m
         where (:menuName is null or m.menuName like concat('%', :menuName, '%'))
           and (:menuType is null or m.menuType = :menuType)
           and (:useYn    is null or m.useYn = :useYn)
         order by m.menuDepth, m.sortOrder, m.menuId
        """)
    Page<Menu> search(@Param("menuName") String menuName,
                      @Param("menuType") String menuType,
                      @Param("useYn")    String useYn,
                      Pageable pageable);
}
