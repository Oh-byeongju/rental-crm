/* ============================================================
 * 대시보드 차트 — Chart.js
 * 04 §9-1 요구사항 + 05 §2-2 매핑
 * ============================================================ */

(async function initDashboard() {
    // 월별 수납 추이 (라인)
    const trendCtx = document.getElementById('paymentTrendChart');
    if (trendCtx) {
        // TODO: 실제 API 연동 — GET /api/reports/payment/monthly
        const placeholderData = {
            labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
            datasets: [{
                label: '수납액 (원)',
                data: [0, 0, 0, 0, 0, 0],
                borderColor: '#206bc4',
                backgroundColor: 'rgba(32, 107, 196, 0.1)',
                tension: 0.3,
                fill: true,
            }],
        };
        new Chart(trendCtx, {
            type: 'line',
            data: placeholderData,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { callback: (v) => v.toLocaleString('ko-KR') },
                    },
                },
            },
        });
    }

    // 청구 상태 분포 (도넛)
    const statusCtx = document.getElementById('billingStatusChart');
    if (statusCtx) {
        // TODO: 실제 API 연동 — GET /api/dashboard/summary
        const placeholderData = {
            labels: ['미납', '연체', '수납완료', '취소'],
            datasets: [{
                data: [0, 0, 0, 0],
                backgroundColor: ['#6c757d', '#f59f00', '#2fb344', '#adb5bd'],
            }],
        };
        new Chart(statusCtx, {
            type: 'doughnut',
            data: placeholderData,
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
            },
        });
    }

    // 추후: GET /api/dashboard/summary 호출로 위젯 카드 + 차트 데이터 갱신
    // try {
    //   const data = await App.get('/api/dashboard/summary');
    //   ...
    // } catch (e) { App.toast(e.message, 'error'); }
})();
