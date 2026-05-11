const wrapper = document.querySelector('.carrusel__wrapper')
const track =document.querySelector('.carrusel__track');
const btnPrev = document.querySelector('.carrusel__btn--prev');
const btnNext = document.querySelector('.carrusel__btn--next');

const cardWidth = () => track.querySelector('.card').offsetWidth + 24;

btnNext.addEventListener('click', () => {
    wrapper.scrollBy({ left: cardWidth(), behavior: 'smooth'})
});

btnPrev.addEventListener('click', () => {
    wrapper.scrollBy({ left: -cardWidth(), behavior: 'smooth'})
});