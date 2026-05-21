const wrapper = document.querySelector('.carrusel__wrapper');
const track = document.querySelector('#channels-track');
const btnPrev = document.querySelector('.carrusel__btn--prev');
const btnNext = document.querySelector('.carrusel__btn--next');
const channelsState = document.querySelector('#channels-state');
const publicationsState = document.querySelector('#publications-state');
const publicationsBody = document.querySelector('#publications-body');
const paginationContainer = document.querySelector('#pagination-container');
const publicationsSection = document.querySelector('#publicaciones');

// Tamaño de página del listado de publicaciones. Coincide con el default del backend.
const PAGE_SIZE = 12;
let currentPage = 0;
let totalPages = 0;

// Detecta entorno automáticamente:
//   - Si la página se sirve desde localhost (Docker o servidor local) → backend local en 8000.
//   - Si se sirve desde sandbox.yerson.co (despliegue compartido) → backend remoto.
const API_HOST = (location.hostname === 'localhost' || location.hostname === '127.0.0.1')
    ? 'http://localhost:8000'
    : 'https://sandbox.yerson.co';
const API_BASE_URL = `${API_HOST}/api`;

// Refresca los enlaces estáticos del HTML (botón "Exportar CSV", enlace a "API")
// para que apunten al mismo host que la API.
document.querySelectorAll('a[data-api-href]').forEach((a) => {
    a.href = API_HOST + a.dataset.apiHref;
});

const fallbackBanner = 'img/evasiontv_banner.jpg';
const fallbackAvatar = 'img/evasiontv_logo.jpg';

const formatDate = (value) => new Intl.DateTimeFormat('es-ES', {
    dateStyle: 'medium'
}).format(new Date(value));

const cardWidth = () => {
    const card = track.querySelector('.card');
    return card ? card.offsetWidth + 24 : 260;
};

btnNext.addEventListener('click', () => {
    wrapper.scrollBy({ left: cardWidth(), behavior: 'smooth' });
});

btnPrev.addEventListener('click', () => {
    wrapper.scrollBy({ left: -cardWidth(), behavior: 'smooth' });
});

// Sustituye comillas y caracteres peligrosos para evitar inyección al
// construir HTML por concatenación.
function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function renderChannel(channel) {
    const safeName = escapeHtml(channel.name);
    const bannerSrc = escapeHtml(channel.imageUrl || fallbackBanner);
    const avatarSrc = escapeHtml(channel.imageUrl || fallbackAvatar);
    return `
        <article class="card">
            <div class="card__banner">
                <img src="${bannerSrc}" alt="${safeName}" loading="lazy" decoding="async">
            </div>
            <img class="card__avatar" src="${avatarSrc}" alt="${safeName}" loading="lazy" decoding="async">
            <div class="card__body">
                <div class="card__meta">
                    <span class="card__pill">${escapeHtml(channel.platform)}</span>
                    <span class="card__pill">${channel.publicationsCount} publicaciones</span>
                </div>
                <h2 class="card__username">${safeName}</h2>
                <p class="card__desc">${escapeHtml(channel.description)}</p>
                <ul class="card__platforms">
                    <li><a href="${escapeHtml(channel.channelUrl)}" target="_blank" rel="noreferrer">Canal</a></li>
                    <li><a href="${escapeHtml(channel.latestPublicationUrl)}" target="_blank" rel="noreferrer">Ultima publicacion</a></li>
                </ul>
            </div>
        </article>
    `;
}

// Convierte segundos en "mm:ss" o "h:mm:ss" para mostrar duración en YouTube.
function formatDuration(seconds) {
    if (seconds == null || seconds < 0) return '';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    const pad = (n) => String(n).padStart(2, '0');
    return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`;
}

// Decide la imagen de cabecera de cada card:
//   1) imagen del post/video (image_url del JSON)
//   2) avatar del autor (solo blogs que lo tengan)
//   3) degradado de marca (placeholder CSS, sin <img>)
function buildPublicationMedia(publication) {
    const safeTitle = escapeHtml(publication.title);
    const duration = publication.durationSeconds
        ? `<span class="publication-card__duration">${formatDuration(publication.durationSeconds)}</span>`
        : '';

    if (publication.imageUrl) {
        return `
            <div class="publication-card__media">
                <img src="${escapeHtml(publication.imageUrl)}" alt="${safeTitle}" loading="lazy" decoding="async">
                ${duration}
            </div>
        `;
    }
    if (publication.authorImageUrl) {
        return `
            <div class="publication-card__media publication-card__media--avatar">
                <img src="${escapeHtml(publication.authorImageUrl)}" alt="${escapeHtml(publication.authorName || '')}" loading="lazy" decoding="async">
                ${duration}
            </div>
        `;
    }
    return `<div class="publication-card__media">${duration}</div>`;
}

function renderPublication(publication) {
    const channelLine = publication.authorName
        ? `<strong>${escapeHtml(publication.channelName)}</strong> · ${escapeHtml(publication.authorName)}`
        : `<strong>${escapeHtml(publication.channelName)}</strong>`;

    return `
        <article class="publication-card">
            ${buildPublicationMedia(publication)}
            <div class="publication-card__body">
                <div class="publication-card__meta">
                    <span class="publication-card__platform">${escapeHtml(publication.platform)}</span>
                    <span>${formatDate(publication.datePublished)}</span>
                </div>
                <h3 class="publication-card__title">${escapeHtml(publication.title)}</h3>
                <p class="publication-card__channel">${channelLine}</p>
                <a class="publication-card__link" href="${escapeHtml(publication.url)}" target="_blank" rel="noreferrer">Abrir →</a>
            </div>
        </article>
    `;
}

// ────────────────────────────────────────────────
//  Skeletons mientras llegan los datos del backend
// ────────────────────────────────────────────────
function skeletonCard() {
    return `
        <article class="skeleton-card" aria-hidden="true">
            <div class="skeleton-card__banner skeleton"></div>
            <div class="skeleton-card__avatar"></div>
            <div class="skeleton-card__line skeleton long"></div>
            <div class="skeleton-card__line skeleton short"></div>
            <div class="skeleton-card__line skeleton long"></div>
        </article>
    `;
}

function skeletonPublicationCard() {
    return `
        <article class="skeleton-publication-card" aria-hidden="true">
            <div class="skeleton-publication-card__media skeleton"></div>
            <div class="skeleton-publication-card__body">
                <div class="skeleton-publication-card__line skeleton short"></div>
                <div class="skeleton-publication-card__line skeleton long"></div>
                <div class="skeleton-publication-card__line skeleton long"></div>
                <div class="skeleton-publication-card__line skeleton short"></div>
            </div>
        </article>
    `;
}

function showSkeletons() {
    track.innerHTML = Array.from({ length: 4 }, skeletonCard).join('');
    publicationsBody.innerHTML = Array.from({ length: 6 }, skeletonPublicationCard).join('');
    channelsState.textContent = '';
    publicationsState.textContent = '';
}

async function loadData() {
    showSkeletons();
    try {
        const [channelsResponse, publicationsResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/channels`),
            fetch(`${API_BASE_URL}/publications?page=0&size=${PAGE_SIZE}`)
        ]);

        if (!channelsResponse.ok || !publicationsResponse.ok) {
            throw new Error('No se pudo cargar la API');
        }

        const channels = await channelsResponse.json();
        const publicationsPage = await publicationsResponse.json();

        track.innerHTML = channels.map(renderChannel).join('');

        renderPublicationsPage(publicationsPage);

        document.querySelector('#channels-count').textContent = channels.length;
        document.querySelector('#publications-count').textContent = publicationsPage.totalItems;

        channelsState.textContent = channels.length ? '' : 'No hay canales disponibles.';
        publicationsState.textContent = publicationsPage.totalItems ? '' : 'No hay publicaciones disponibles.';
    } catch (error) {
        track.innerHTML = '';
        publicationsBody.innerHTML = '';
        paginationContainer.innerHTML = '';
        channelsState.textContent = 'Error al cargar los canales desde el backend.';
        publicationsState.textContent = 'Error al cargar las publicaciones desde el backend.';
        console.error(error);
    }
}

// Pinta una página completa de publicaciones (reemplaza lo que había)
// y actualiza los controles de paginación.
function renderPublicationsPage(page) {
    currentPage = page.currentPage;
    totalPages = page.totalPages;
    publicationsBody.innerHTML = page.content.map(renderPublication).join('');
    renderPagination();
}

// Pide al backend la página N y la pinta. Hace scroll suave al inicio de
// la sección para que el usuario no se quede a media pantalla.
async function goToPage(page) {
    if (page < 0 || page >= totalPages || page === currentPage) return;
    try {
        const response = await fetch(`${API_BASE_URL}/publications?page=${page}&size=${PAGE_SIZE}`);
        if (!response.ok) throw new Error('No se pudo cargar la página');
        const data = await response.json();
        renderPublicationsPage(data);
        publicationsSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (error) {
        console.error(error);
        publicationsState.textContent = 'Error al cargar la página.';
    }
}

// Calcula qué números de página mostrar (con '...' cuando hay muchas).
//   total = 5,  current = 2  → [0, 1, 2, 3, 4]
//   total = 10, current = 0  → [0, 1, 2, '...', 9]
//   total = 10, current = 4  → [0, '...', 3, 4, 5, '...', 9]
//   total = 10, current = 9  → [0, '...', 7, 8, 9]
function getPageNumbers(current, total) {
    if (total <= 7) {
        return Array.from({ length: total }, (_, i) => i);
    }
    const pages = [0];
    if (current > 2) pages.push('...');

    const start = Math.max(1, current - 1);
    const end = Math.min(total - 2, current + 1);
    for (let i = start; i <= end; i++) pages.push(i);

    if (current < total - 3) pages.push('...');
    pages.push(total - 1);
    return pages;
}

function renderPagination() {
    if (totalPages <= 1) {
        paginationContainer.innerHTML = '';
        return;
    }

    const numbers = getPageNumbers(currentPage, totalPages);
    const prevDisabled = currentPage === 0 ? 'disabled' : '';
    const nextDisabled = currentPage === totalPages - 1 ? 'disabled' : '';

    const parts = [`<nav class="pagination" aria-label="Paginacion de publicaciones">`];
    parts.push(`<button class="pagination__btn" data-page="${currentPage - 1}" ${prevDisabled} aria-label="Anterior">&laquo;</button>`);

    for (const n of numbers) {
        if (n === '...') {
            parts.push(`<span class="pagination__ellipsis">...</span>`);
        } else {
            const isActive = n === currentPage;
            const activeClass = isActive ? ' pagination__btn--active' : '';
            const ariaCurrent = isActive ? ' aria-current="page"' : '';
            parts.push(`<button class="pagination__btn${activeClass}" data-page="${n}"${ariaCurrent}>${n + 1}</button>`);
        }
    }

    parts.push(`<button class="pagination__btn" data-page="${currentPage + 1}" ${nextDisabled} aria-label="Siguiente">&raquo;</button>`);
    parts.push(`</nav>`);

    paginationContainer.innerHTML = parts.join('');

    paginationContainer.querySelectorAll('.pagination__btn:not(:disabled):not(.pagination__btn--active)')
        .forEach((btn) => {
            btn.addEventListener('click', () => goToPage(parseInt(btn.dataset.page, 10)));
        });
}

loadData();
