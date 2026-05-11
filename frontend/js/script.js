const wrapper = document.querySelector('.carrusel__wrapper');
const track = document.querySelector('#channels-track');
const btnPrev = document.querySelector('.carrusel__btn--prev');
const btnNext = document.querySelector('.carrusel__btn--next');
const channelsState = document.querySelector('#channels-state');
const publicationsState = document.querySelector('#publications-state');
const publicationsBody = document.querySelector('#publications-body');

const API_BASE_URL = 'https://sandbox.yerson.co/api';

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

function renderChannel(channel) {
    return `
        <article class="card">
            <div class="card__banner">
                <img src="${channel.imageUrl || fallbackBanner}" alt="${channel.name}">
            </div>
            <img class="card__avatar" src="${channel.imageUrl || fallbackAvatar}" alt="${channel.name}">
            <div class="card__body">
                <div class="card__meta">
                    <span class="card__pill">${channel.platform}</span>
                    <span class="card__pill">${channel.publicationsCount} publicaciones</span>
                </div>
                <h2 class="card__username">${channel.name}</h2>
                <p class="card__desc">${channel.description}</p>
                <ul class="card__platforms">
                    <li><a href="${channel.channelUrl}" target="_blank" rel="noreferrer">Canal</a></li>
                    <li><a href="${channel.latestPublicationUrl}" target="_blank" rel="noreferrer">Ultima publicacion</a></li>
                </ul>
            </div>
        </article>
    `;
}

function renderPublication(publication) {
    return `
        <tr>
            <td>${publication.title}</td>
            <td>${publication.channelName}</td>
            <td>${publication.platform}</td>
            <td>${formatDate(publication.datePublished)}</td>
            <td><a href="${publication.url}" target="_blank" rel="noreferrer">Abrir</a></td>
        </tr>
    `;
}

async function loadData() {
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
        channelsState.textContent = 'Error al cargar los canales desde el backend.';
        publicationsState.textContent = 'Error al cargar las publicaciones desde el backend.';
        console.error(error);
    }
}

loadData();
