package com.rental.backoffice.admin.seeder;

import com.rental.domain.admin.entity.AdminUser;
import com.rental.domain.admin.entity.UserAuth;
import com.rental.domain.admin.repository.AdminUserRepository;
import com.rental.domain.admin.repository.UserAuthRepository;
import com.rental.domain.auth.entity.Role;
import com.rental.domain.auth.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 admin 자동 시드 — ADR-009 §2-5, ADR-010 §2-6.
 *
 * <p>첫 부팅 시 {@code admin@rental.com} 없으면 INSERT.
 * 비밀번호 {@code Admin1234!} BCrypt 인코드 (학습용 — 평문 공유).
 * SUPER_ADMIN 역할 매핑.
 *
 * <p>샘플 CM_USER_AUTH 1행 (학습 데모) — admin 의 CUSTOMER_EXPORT REVOKE 시범
 * (실제 SUPER_ADMIN 이라 권한 판정엔 영향 없으나, UI 흐름 검증용).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private static final String DEFAULT_EMAIL    = "admin@rental.com";
    private static final String DEFAULT_PASSWORD = "Admin1234!";
    private static final String DEFAULT_NAME     = "최고관리자";
    private static final String SAMPLE_AUTH_CODE = "CUSTOMER_EXPORT";

    private final AdminUserRepository userRepository;
    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(DEFAULT_EMAIL)) {
            log.info("AdminSeeder: 기존 admin 발견 — 시드 스킵 ({})", DEFAULT_EMAIL);
            return;
        }

        Long superAdminRoleId = roleRepository.findByRoleCode(Role.CODE_SUPER_ADMIN)
                .map(Role::getRoleId)
                .orElse(null);
        if (superAdminRoleId == null) {
            log.warn("AdminSeeder: SUPER_ADMIN 역할 시드 없음 — 03-seed-codes.sql 실행 확인 필요. admin 생성 스킵.");
            return;
        }

        var admin = AdminUser.builder()
                .email(DEFAULT_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .userName(DEFAULT_NAME)
                .phone(null)
                .roleId(superAdminRoleId)
                .build();
        var saved = userRepository.save(admin);

        // 학습 데모 — 권한 미세 조정 행 1개
        userAuthRepository.save(UserAuth.builder()
                .userId(saved.getUserId())
                .authCode(SAMPLE_AUTH_CODE)
                .grantType(UserAuth.TYPE_GRANT)
                .build());

        log.info("""
                AdminSeeder: 초기 admin 생성 완료
                  email     = {}
                  password  = Admin1234! (학습용 평문)
                  userId    = {}
                  role      = SUPER_ADMIN ({})
                  샘플 GRANT = {} (UI 검증용 — SUPER_ADMIN 이라 권한 판정엔 영향 없음)""",
                DEFAULT_EMAIL, saved.getUserId(), superAdminRoleId, SAMPLE_AUTH_CODE);
    }
}
