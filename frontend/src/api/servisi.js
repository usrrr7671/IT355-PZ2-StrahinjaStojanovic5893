import { klijent } from './klijent'

/*
  Sve adrese API-ja stoje na jednom mestu. Komponente pozivaju imenovane
  funkcije, pa promena putanje na backendu ne trazi izmenu po ekranima.
*/

export const auth = {
  prijava: (podaci) => klijent.post('/auth/login', podaci).then((o) => o.data),
  registracija: (podaci) => klijent.post('/auth/register', podaci).then((o) => o.data),
  ja: () => klijent.get('/auth/me').then((o) => o.data),
}

export const tiketi = {
  pretraga: (parametri) => klijent.get('/tickets', { params: parametri }).then((o) => o.data),
  moji: (parametri) => klijent.get('/tickets/my', { params: parametri }).then((o) => o.data),
  dodeljeniMeni: (parametri) =>
    klijent.get('/tickets/assigned-to-me', { params: parametri }).then((o) => o.data),
  jedan: (id) => klijent.get(`/tickets/${id}`).then((o) => o.data),
  kreiraj: (podaci) => klijent.post('/tickets', podaci).then((o) => o.data),
  izmeni: (id, podaci) => klijent.put(`/tickets/${id}`, podaci).then((o) => o.data),
  promeniStatus: (id, podaci) =>
    klijent.patch(`/tickets/${id}/status`, podaci).then((o) => o.data),
  dodeli: (id, agentId) =>
    klijent.patch(`/tickets/${id}/assign`, { agentId: agentId ?? null }).then((o) => o.data),
  ponovoOtvori: (id, razlog) =>
    klijent
      .patch(`/tickets/${id}/reopen`, null, { params: razlog ? { reason: razlog } : {} })
      .then((o) => o.data),
  obrisi: (id) => klijent.delete(`/tickets/${id}`),
}

export const komentari = {
  zaTiket: (ticketId) => klijent.get(`/tickets/${ticketId}/comments`).then((o) => o.data),
  dodaj: (ticketId, podaci) =>
    klijent.post(`/tickets/${ticketId}/comments`, podaci).then((o) => o.data),
  obrisi: (commentId) => klijent.delete(`/comments/${commentId}`),
}

export const prepiske = {
  moje: () => klijent.get('/conversations').then((o) => o.data),
  neprocitane: () => klijent.get('/conversations/unread-count').then((o) => o.data),
  jedna: (id) => klijent.get(`/conversations/${id}`).then((o) => o.data),
  zapocni: (podaci) => klijent.post('/conversations', podaci).then((o) => o.data),
  posalji: (id, sadrzaj) =>
    klijent.post(`/conversations/${id}/messages`, { content: sadrzaj }).then((o) => o.data),
}

export const kategorije = {
  lista: (samoAktivne = true) =>
    klijent.get('/categories', { params: { onlyActive: samoAktivne } }).then((o) => o.data),
  kreiraj: (podaci) => klijent.post('/categories', podaci).then((o) => o.data),
  izmeni: (id, podaci) => klijent.put(`/categories/${id}`, podaci).then((o) => o.data),
  obrisi: (id) => klijent.delete(`/categories/${id}`),
}

export const prioriteti = {
  lista: () => klijent.get('/priorities').then((o) => o.data),
  kreiraj: (podaci) => klijent.post('/priorities', podaci).then((o) => o.data),
  izmeni: (id, podaci) => klijent.put(`/priorities/${id}`, podaci).then((o) => o.data),
  obrisi: (id) => klijent.delete(`/priorities/${id}`),
}

export const korisnici = {
  agenti: () => klijent.get('/users/agents').then((o) => o.data),
  promeniLozinku: (podaci) => klijent.post('/users/me/change-password', podaci),
}

export const administracija = {
  nalozi: (parametri) => klijent.get('/admin/users', { params: parametri }).then((o) => o.data),
  kreirajNalog: (podaci) => klijent.post('/admin/users', podaci).then((o) => o.data),
  izmeniNalog: (id, podaci) => klijent.put(`/admin/users/${id}`, podaci).then((o) => o.data),
  deaktiviraj: (id) => klijent.delete(`/admin/users/${id}`),
}

export const izvestaji = {
  pregled: () => klijent.get('/reports/dashboard').then((o) => o.data),
}
