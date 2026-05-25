// =====================================================================
//  Dashboard de exportación
// ---------------------------------------------------------------------
//  - Lee los filtros del formulario.
//  - Pide GET /api/stats para refrescar las tarjetas y la barra por canal.
//  - El botón "Descargar CSV" usa los mismos filtros para construir la
//    URL de /api/reports/publications.csv y disparar la descarga.
// =====================================================================

// Detecta entorno (mismo patrón que script.js de la home).
const API_HOST = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8000'
    : 'https://sandbox.yerson.co';
const API_BASE_URL = `${API_HOST}/api`;

// Apunta los enlaces del header al host correcto (igual que en index.html).
document.querySelectorAll('a[data-api-href]').forEach((a) => {
    a.href = API_HOST + a.dataset.apiHref;
});

// Referencias del DOM
const form              = document.querySelector('#dashboard-form');
const platformSelect    = document.querySelector('#filter-platform');
const channelSelect     = document.querySelector('#filter-channel');
const fromInput         = document.querySelector('#filter-from');
const toInput           = document.querySelector('#filter-to');

// Cache de los canales que devuelve la API. Lo usamos para repoblar el
// select de canales cuando cambia la plataforma sin tener que pedir otra vez.
let allChannels = [];
const statsTotal        = document.querySelector('#stats-total');
const statsYoutube      = document.querySelector('#stats-youtube');
const statsBlog         = document.querySelector('#stats-blog');
const statsPeriod       = document.querySelector('#stats-period');
const bars              = document.querySelector('#dashboard-bars');
const downloadSummary   = document.querySelector('#dashboard-download-summary');
const downloadBtn       = document.querySelector('#download-csv-btn');

// Sanitiza HTML para evitar inyección al construir cards/barras a mano.
function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

// Capitaliza la primera letra (youtube -> Youtube). Para mostrar en el select.
function capitalizeFirst(text) {
    if (!text) return '';
    return text.charAt(0).toUpperCase() + text.slice(1);
}

// Formatea un ISO LocalDateTime a "mes año" abreviado en español.
const monthFormatter = new Intl.DateTimeFormat('es-ES', { month: 'short', year: 'numeric' });
function formatMonth(isoDateTime) {
    if (!isoDateTime) return '';
    return monthFormatter.format(new Date(isoDateTime));
}

// Recoge los valores actuales del formulario y devuelve un URLSearchParams
// con solo los que tienen contenido. Lo usan tanto la carga de stats como
// la descarga del CSV — así los dos botones leen la misma "verdad".
function readFilters() {
    const params = new URLSearchParams();
    const formData = new FormData(form);
    for (const [key, value] of formData.entries()) {
        const trimmed = String(value).trim();
        if (trimmed) params.set(key, trimmed);
    }
    return params;
}

// ────────────────────────────────────────────────
//  Carga inicial: poblar selects de plataforma y canal
// ────────────────────────────────────────────────
async function populateFilterOptions() {
    try {
        const response = await fetch(`${API_BASE_URL}/channels`);
        if (!response.ok) return;
        allChannels = await response.json();

        // Plataformas únicas, ordenadas alfabéticamente.
        const platforms = [...new Set(allChannels.map((c) => c.platform))].sort();
        rebuildSelect(platformSelect, 'Todas', platforms.map((code) => ({
            value: code,
            label: capitalizeFirst(code),
        })));

        // Canales: inicialmente todos. Se refiltrarán al cambiar la plataforma.
        refreshChannelOptions();
    } catch (e) {
        console.warn('No se pudieron cargar las opciones del filtro:', e);
    }
}

// Rellena las opciones del select de canales según la plataforma elegida.
// Si la plataforma está vacía, muestra todos. Si el canal previamente
// seleccionado ya no encaja en la nueva lista, lo resetea.
function refreshChannelOptions() {
    const platform = platformSelect.value;
    const previousChannel = channelSelect.value;

    const filtered = platform
        ? allChannels.filter((c) => c.platform === platform)
        : allChannels;

    // Ordenados por nombre para que el usuario los encuentre fácil.
    filtered.sort((a, b) => a.name.localeCompare(b.name, 'es'));

    const options = filtered.map((c) => ({
        // Mandamos el nombre tal cual; el backend hace LIKE %name% case-insensitive.
        value: c.name,
        // Si NO hay plataforma seleccionada, anotamos la plataforma entre paréntesis
        // para diferenciar canales del mismo nombre en distintas redes.
        label: platform ? c.name : `${c.name} (${capitalizeFirst(c.platform)})`,
    }));

    rebuildSelect(channelSelect, 'Todos', options);

    // Conserva la selección anterior si sigue siendo válida.
    if (options.some((o) => o.value === previousChannel)) {
        channelSelect.value = previousChannel;
    } else {
        channelSelect.value = '';
    }
}

// Helper para reconstruir un <select> manteniendo la primera opción "todos"/"todas".
function rebuildSelect(selectEl, defaultLabel, items) {
    // Vaciamos pero conservamos la opción 0.
    while (selectEl.options.length > 1) {
        selectEl.remove(1);
    }
    // Asegura que la primera opción tiene el label correcto (por si el HTML inicial lo cambió).
    selectEl.options[0].value = '';
    selectEl.options[0].textContent = defaultLabel;
    for (const item of items) {
        const option = document.createElement('option');
        option.value = item.value;
        option.textContent = item.label;
        selectEl.appendChild(option);
    }
}

// ────────────────────────────────────────────────
//  Refresca las stats con los filtros actuales
// ────────────────────────────────────────────────
async function refreshStats() {
    const params = readFilters();
    const query = params.toString();
    const url = `${API_BASE_URL}/stats${query ? '?' + query : ''}`;
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error('No se pudieron cargar las estadisticas');
        const stats = await response.json();
        renderStats(stats);
    } catch (e) {
        console.error(e);
        statsTotal.textContent = '–';
        bars.innerHTML = '<li class="dashboard__bars-empty">Error cargando estadisticas.</li>';
    }
}

function renderStats(stats) {
    statsTotal.textContent   = stats.totalPublications;
    statsYoutube.textContent = stats.byPlatform.youtube ?? 0;
    statsBlog.textContent    = stats.byPlatform.blog    ?? 0;

    if (stats.earliestDate && stats.latestDate) {
        statsPeriod.textContent = `${formatMonth(stats.earliestDate)} – ${formatMonth(stats.latestDate)}`;
    } else {
        statsPeriod.textContent = '—';
    }

    // Distribución por canal con barras CSS proporcionales al canal más activo.
    if (!stats.byChannel.length) {
        bars.innerHTML = '<li class="dashboard__bars-empty">No hay publicaciones que coincidan con los filtros.</li>';
    } else {
        const max = stats.byChannel[0].count;  // viene ordenado desc
        bars.innerHTML = stats.byChannel.map((item) => {
            const width = max > 0 ? Math.max(4, Math.round((item.count / max) * 100)) : 0;
            return `
                <li class="dashboard__bar">
                    <span class="dashboard__bar-label">${escapeHtml(item.name)}</span>
                    <span class="dashboard__bar-platform">${escapeHtml(capitalizeFirst(item.platform))}</span>
                    <span class="dashboard__bar-track">
                        <span class="dashboard__bar-fill" style="width: ${width}%"></span>
                    </span>
                    <span class="dashboard__bar-count">${item.count}</span>
                </li>
            `;
        }).join('');
    }

    // Resumen textual encima del botón de descarga.
    const filterDesc = describeActiveFilters();
    downloadSummary.textContent = filterDesc
        ? `Vas a descargar ${stats.totalPublications} publicaciones (${filterDesc}).`
        : `Vas a descargar ${stats.totalPublications} publicaciones (sin filtros).`;
}

function describeActiveFilters() {
    const parts = [];
    if (platformSelect.value) parts.push(`plataforma: ${capitalizeFirst(platformSelect.value)}`);
    if (channelSelect.value) parts.push(`canal: ${channelSelect.value}`);
    if (fromInput.value) parts.push(`desde ${fromInput.value}`);
    if (toInput.value) parts.push(`hasta ${toInput.value}`);
    return parts.join(' · ');
}

// ────────────────────────────────────────────────
//  Manejadores de los botones
// ────────────────────────────────────────────────
form.addEventListener('submit', (event) => {
    event.preventDefault();
    refreshStats();
});

form.addEventListener('reset', () => {
    // Damos un tick para que el reset deje los inputs vacíos antes de re-pedir stats.
    setTimeout(() => {
        refreshChannelOptions();
        refreshStats();
    }, 0);
});

// Cuando cambia la plataforma, el select de canales se refiltra para que
// solo aparezcan los de esa red. Sin necesidad de pulsar "Aplicar" todavía.
platformSelect.addEventListener('change', refreshChannelOptions);

downloadBtn.addEventListener('click', () => {
    const params = readFilters();
    const query = params.toString();
    const url = `${API_BASE_URL}/reports/publications.csv${query ? '?' + query : ''}`;
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'publications.csv';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
});

// ────────────────────────────────────────────────
//  Arranque
// ────────────────────────────────────────────────
(async () => {
    await populateFilterOptions();
    await refreshStats();
})();
