/*
  Nazivi statusa i uloga se prevode na jednom mestu. Backend kroz API salje
  kljuceve (NEW, IN_PROGRESS...), a korisnik ih nikada ne vidi u tom obliku.
*/

export const NAZIV_STATUSA = {
  NEW: 'Nov',
  OPEN: 'Otvoren',
  IN_PROGRESS: 'U radu',
  RESOLVED: 'Rešen',
  CLOSED: 'Zatvoren',
  REOPENED: 'Ponovo otvoren',
}

/** Redosled kojim se statusi prikazuju na traci zivotnog ciklusa. */
export const REDOSLED_STATUSA = [
  'NEW',
  'OPEN',
  'IN_PROGRESS',
  'RESOLVED',
  'REOPENED',
  'CLOSED',
]

export const NAZIV_ULOGE = {
  USER: 'Korisnik',
  AGENT: 'Agent',
  ADMIN: 'Administrator',
}

export function nazivStatusa(status) {
  return NAZIV_STATUSA[status] ?? status
}

export function nazivUloge(uloga) {
  return NAZIV_ULOGE[uloga] ?? uloga
}

/** Broj tiketa kao plocica: 42 -> "#0042". */
export function brojTiketa(id) {
  return `#${String(id).padStart(4, '0')}`
}

const DATUM_VREME = new Intl.DateTimeFormat('sr-Latn-RS', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})

const SAMO_DATUM = new Intl.DateTimeFormat('sr-Latn-RS', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

const SAMO_VREME = new Intl.DateTimeFormat('sr-Latn-RS', {
  hour: '2-digit',
  minute: '2-digit',
})

export function datumVreme(vrednost) {
  if (!vrednost) return '—'
  return DATUM_VREME.format(new Date(vrednost))
}

export function datum(vrednost) {
  if (!vrednost) return '—'
  return SAMO_DATUM.format(new Date(vrednost))
}

export function vreme(vrednost) {
  if (!vrednost) return '—'
  return SAMO_VREME.format(new Date(vrednost))
}

/** "pre 3 sata", "za 2 dana" - kratak oblik za listu i SLA. */
export function relativno(vrednost) {
  if (!vrednost) return '—'
  const razlika = new Date(vrednost).getTime() - Date.now()
  const minuti = Math.round(razlika / 60000)
  const apsolutni = Math.abs(minuti)

  if (apsolutni < 1) return 'sada'
  if (apsolutni < 60) return oblik(minuti, apsolutni, 'min')
  if (apsolutni < 1440) return oblik(minuti, Math.round(apsolutni / 60), 'h')
  return oblik(minuti, Math.round(apsolutni / 1440), 'd')
}

function oblik(predznak, vrednost, jedinica) {
  return predznak < 0 ? `pre ${vrednost}${jedinica}` : `za ${vrednost}${jedinica}`
}

/**
 * Koliko je SLA roka potroseno, u procentima. Racuna se od nastanka tiketa do
 * roka; preko 100 znaci da je rok probijen.
 */
export function potrosenSla(createdAt, slaDeadline) {
  if (!createdAt || !slaDeadline) return null
  const pocetak = new Date(createdAt).getTime()
  const rok = new Date(slaDeadline).getTime()
  const ukupno = rok - pocetak
  if (ukupno <= 0) return 100
  return ((Date.now() - pocetak) / ukupno) * 100
}

/** Skracuje dugacak tekst za prikaz u listi. */
export function skrati(tekst, duzina = 80) {
  if (!tekst) return ''
  return tekst.length > duzina ? `${tekst.slice(0, duzina).trimEnd()}…` : tekst
}
