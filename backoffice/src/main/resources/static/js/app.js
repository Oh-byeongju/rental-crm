/* ============================================================
 * rental-crm 공통 JS
 * - API 호출 헬퍼 (fetch wrapper)
 * - 토스트 알림
 * ============================================================ */

const App = (() => {
    const BASE = '';

    async function request(method, url, body) {
        const opts = {
            method,
            headers: { 'Content-Type': 'application/json' },
            credentials: 'same-origin',
        };
        if (body !== undefined && body !== null) {
            opts.body = JSON.stringify(body);
        }
        const res = await fetch(BASE + url, opts);
        const isJson = (res.headers.get('content-type') || '').includes('application/json');
        const payload = isJson ? await res.json() : await res.text();
        if (!res.ok) {
            const msg = (payload && payload.message) || `HTTP ${res.status}`;
            throw new ApiError(msg, payload && payload.code, payload && payload.errors);
        }
        return payload;
    }

    class ApiError extends Error {
        constructor(message, code, errors) {
            super(message);
            this.code = code;
            this.errors = errors;
        }
    }

    function toast(message, type = 'info') {
        // Tabler 의 toast 컴포넌트는 별도 init 필요. 간단히 alert 으로 시작.
        // TODO: Tabler `toast-container` 동적 생성으로 교체
        const cls = { info: 'alert-info', success: 'alert-success', warning: 'alert-warning', error: 'alert-danger' }[type] || 'alert-info';
        const div = document.createElement('div');
        div.className = `alert ${cls} alert-dismissible position-fixed`;
        div.style.cssText = 'top:1rem;right:1rem;z-index:9999;min-width:300px;';
        div.innerHTML = `${message}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>`;
        document.body.appendChild(div);
        setTimeout(() => div.remove(), 4000);
    }

    return {
        get:  (url)        => request('GET',    url),
        post: (url, body)  => request('POST',   url, body),
        put:  (url, body)  => request('PUT',    url, body),
        del:  (url)        => request('DELETE', url),
        toast,
        ApiError,
    };
})();
