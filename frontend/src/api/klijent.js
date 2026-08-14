import axios from 'axios'

const KLJUC_TOKENA = 'helpdesk.token'

export function procitajToken() {
  return localStorage.getItem(KLJUC_TOKENA)
}

export function zapamtiToken(token) {
  if (token) localStorage.setItem(KLJUC_TOKENA, token)
  else localStorage.removeItem(KLJUC_TOKENA)
}

export const klijent = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
})

/*
  Token se dodaje na jednom mestu, a ne u svakom pozivu. Da je ostavljen
  pojedinacnim pozivima, dovoljno bi bilo da se na jednom zaboravi pa da
  zahtev ode neautentifikovan i vrati 401 bez ocitog razloga.
*/
klijent.interceptors.request.use((konfiguracija) => {
  const token = procitajToken()
  if (token) {
    konfiguracija.headers.Authorization = `Bearer ${token}`
  }
  return konfiguracija
})

/*
  Istekao ili neispravan token znaci da sesija vise ne postoji. Umesto da
  svaka strana zasebno hvata 401, ovde se token brise i korisnik se vraca na
  prijavu. Sam ekran prijave je izuzet: tamo 401 znaci pogresnu lozinku, a to
  je poruka koju korisnik treba da vidi, ne preusmeravanje.
*/
klijent.interceptors.response.use(
  (odgovor) => odgovor,
  (greska) => {
    const status = greska?.response?.status
    const putanja = greska?.config?.url ?? ''
    const jePrijava = putanja.includes('/auth/login') || putanja.includes('/auth/register')

    if (status === 401 && !jePrijava) {
      zapamtiToken(null)
      if (window.location.pathname !== '/prijava') {
        window.location.assign('/prijava')
      }
    }
    return Promise.reject(greska)
  },
)

/**
 * Backend za svaku gresku vraca isti oblik (ApiError), pa se poruka cita s
 * jednog mesta. Kod greske validacije se skupljaju poruke pojedinacnih polja.
 */
export function porukaGreske(greska, rezervna = 'Došlo je do greške. Pokušajte ponovo.') {
  const telo = greska?.response?.data
  if (!telo) {
    return greska?.message === 'Network Error'
      ? 'Server nije dostupan. Proverite da li backend radi na portu 8080.'
      : rezervna
  }
  if (telo.fieldErrors && Object.keys(telo.fieldErrors).length > 0) {
    return Object.values(telo.fieldErrors).join(' ')
  }
  return telo.message || rezervna
}

/** Mapa naziv polja -> poruka, za ispis ispod samog polja u obrascu. */
export function greskePolja(greska) {
  return greska?.response?.data?.fieldErrors ?? {}
}
