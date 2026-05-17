package com.rental.backoffice.report.service;

import com.rental.backoffice.report.dto.OverdueReportRow;
import com.rental.backoffice.report.dto.OverdueReportSearchRequest;
import com.rental.domain.common.exception.BusinessException;
import com.rental.domain.common.exception.ErrorCode;
import com.rental.domain.report.repository.OverdueReportRepository;
import com.rental.domain.report.repository.OverdueReportView;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * 미납 현황 리포트 — 화면 페이징 조회 + 엑셀 스트리밍 다운로드 (Ch.2).
 *
 * <p>엑셀: 04 기능명세 §11-1 스트리밍 흐름 — 커서 조회 → SXSSF 행 단위 write →
 * Response OutputStream flush. 0건이면 빈 파일 대신 오류(헤더 쓰기 전 가드).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OverdueReportService {

    /** SXSSF 메모리 유지 행 수 — 이 창을 넘으면 디스크 temp 로 flush (OOM 방지). */
    private static final int SXSSF_WINDOW = 100;

    private static final String[] HEADERS = {
            "청구번호", "청구월", "고객번호", "고객명", "연락처",
            "계약번호", "청구액", "납기일", "상태", "연체일수"
    };

    private final OverdueReportRepository reportRepository;

    public Page<OverdueReportRow> search(OverdueReportSearchRequest req, Pageable pageable) {
        var r = req.normalized();
        return reportRepository
                .searchPaged(r.billingMonth(), r.status(), r.customerName(), pageable)
                .map(OverdueReportRow::from);
    }

    /**
     * 엑셀 스트리밍 다운로드. 호출 측(Controller)이 응답을 그대로 위임.
     * <p>readOnly tx 안에서 Stream 커서를 try-with-resources 로 소비.
     */
    public void exportExcel(OverdueReportSearchRequest req, HttpServletResponse response) {
        var r = req.normalized();

        long total = reportRepository.countMatching(r.billingMonth(), r.status(), r.customerName());
        if (total == 0) {
            // 헤더/바디 쓰기 전 → partial file 없음. @ControllerAdvice 가 JSON 오류로 변환.
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "조회 결과가 없습니다. 다운로드할 데이터가 없습니다.");
        }

        var fileName = "overdue-report-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (SXSSFWorkbook wb = new SXSSFWorkbook(SXSSF_WINDOW);
             Stream<OverdueReportView> rows =
                     reportRepository.streamAll(r.billingMonth(), r.status(), r.customerName())) {

            Sheet sheet = wb.createSheet("미납현황");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }

            int[] idx = {1};
            rows.forEach(v -> {
                Row row = sheet.createRow(idx[0]++);
                row.createCell(0).setCellValue(v.getBillingNo());
                row.createCell(1).setCellValue(v.getBillingMonth());
                row.createCell(2).setCellValue(v.getCustomerNo());
                row.createCell(3).setCellValue(v.getCustomerName());
                row.createCell(4).setCellValue(v.getPhone());
                row.createCell(5).setCellValue(v.getContractNo());
                row.createCell(6).setCellValue(v.getBillingAmount() == null ? 0d : v.getBillingAmount());
                row.createCell(7).setCellValue(v.getDueDate() == null ? "" : v.getDueDate().toString());
                row.createCell(8).setCellValue(statusLabel(v.getBillingStatus()));
                if (v.getOverdueDays() != null) {
                    row.createCell(9).setCellValue(v.getOverdueDays());
                }
            });

            wb.write(response.getOutputStream());
            // try-with-resources 의 close() 가 SXSSF temp 파일까지 정리. (dispose() 는 POI 5.x deprecated)
            log.info("[overdue-report] excel streamed rows={} filter(month={},status={},name={})",
                     idx[0] - 1, r.billingMonth(), r.status(), r.customerName());
        } catch (IOException e) {
            log.error("[overdue-report] excel stream failed", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "엑셀 생성 중 오류가 발생했습니다.");
        }
    }

    private static String statusLabel(String code) {
        return "UNPAID".equals(code) ? "미납"
             : "OVERDUE".equals(code) ? "연체"
             : code;
    }
}
