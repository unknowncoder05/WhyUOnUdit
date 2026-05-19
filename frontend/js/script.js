const wrapper = document.querySelector('.carrusel__wrapper');
const track = document.querySelector('#channels-track');
const btnPrev = document.querySelector('.carrusel__btn--prev');
const btnNext = document.querySelector('.carrusel__btn--next');
const channelsState = document.querySelector('#channels-state');
const publicationsState = document.querySelector('#publications-state');
const publicationsBody = document.querySelector('#publications-body');

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

function renderPublication(publication) {
    return `
        <tr>
            <td>${escapeHtml(publication.title)}</td>
            <td>${escapeHtml(publication.channelName)}</td>
            <td>${escapeHtml(publication.platform)}</td>
            <td>${formatDate(publication.datePublished)}</td>
            <td><a href="${escapeHtml(publication.url)}" target="_blank" rel="noreferrer">Abrir</a></td>
        </tr>
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

function skeletonRow() {
    return `
        <tr class="skeleton-row" aria-hidden="true">
            <td><span class="skeleton"></span></td>
            <td><span class="skeleton"></span></td>
            <td><span class="skeleton"></span></td>
            <td><span class="skeleton"></span></td>
            <td><span class="skeleton"></span></td>
        </tr>
    `;
}

function showSkeletons() {
    track.innerHTML = Array.from({ length: 4 }, skeletonCard).join('');
    publicationsBody.innerHTML = Array.from({ length: 6 }, skeletonRow).join('');
    channelsState.textContent = '';
    publicationsState.textContent = '';
}

async function loadData() {
    showSkeletons();
    try {
        const [channelsResponse, publicationsResponse] = await Promise.all([
            fetch(`${API_BASE_URL}/channels`),
            fetch(`${API_BASE_URL}/publications`)
        ]);

        if (!channelsResponse.ok || !publicationsResponse.ok) {
            throw new Error('No se pudo cargar la API');
        }

        const channels = await channelsResponse.json();
        const publications = await publicationsResponse.json();

        track.innerHTML = channels.map(renderChannel).join('');
        publicationsBody.innerHTML = publications.slice(0, 10).map(renderPublication).join('');

        document.querySelector('#channels-count').textContent = channels.length;
        document.querySelector('#publications-count').textContent = publications.length;

        channelsState.textContent = channels.length ? '' : 'No hay canales disponibles.';
        publicationsState.textContent = publications.length ? '' : 'No hay publicaciones disponibles.';
    } catch (error) {
        track.innerHTML = '';
        publicationsBody.innerHTML = '';
        channelsState.textContent = 'Error al cargar los canales desde el backend.';
        publicationsState.textContent = 'Error al cargar las publicaciones desde el backend.';
        console.error(error);
    }
}

loadData();
