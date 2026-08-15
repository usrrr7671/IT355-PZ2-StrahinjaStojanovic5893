import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { kategorije as apiKategorije, prioriteti as apiPrioriteti, tiketi } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import { useAuth } from '../kontekst/AuthKontekst'
import ListaTiketa from '../komponente/ListaTiketa'
import { Poruka } from '../komponente/Osnovno'
import { NAZIV_STATUSA, REDOSLED_STATUSA } from '../util/format'

/**
 * Pretraga tiketa sa filterima.
 *
 * Korisniku se ne prikazuje nikakav filter po prijaviocu, ali ni da ga rucno
 * doda u adresu ne bi mu koristilo: server korisniku uvek postavlja filter na
 * njegov nalog i vraca samo njegove tikete.
 */
export default function Tiketi() {
  const { jeOsoblje } = useAuth()

  const [filteri, postaviFiltere] = useState({ status: '', categoryId: '', priorityId: '', term: '' })
  const [strana, postaviStranu] = useState(0)
  const [stranica, postaviStranicu] = useState(null)
  const [kategorije, postaviKategorije] = useState([])
  const [prioriteti, postaviPrioritete] = useState([])
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')

  useEffect(() => {
    Promise.all([apiKategorije.lista(), apiPrioriteti.lista()])
      .then(([k, p]) => {
        postaviKategorije(k)
        postaviPrioritete(p)
      })
      .catch(() => {})
  }, [])

  const ucitaj = useCallback(async () => {
    postaviUcitavanje(true)
    postaviGresku('')
    try {
      const parametri = { page: strana, size: 15 }
      if (filteri.status) parametri.status = filteri.status
      if (filteri.categoryId) parametri.categoryId = filteri.categoryId
      if (filteri.priorityId) parametri.priorityId = filteri.priorityId
      if (filteri.term.trim()) parametri.term = filteri.term.trim()
      postaviStranicu(await tiketi.pretraga(parametri))
    } catch (e) {
      postaviGresku(porukaGreske(e))
    } finally {
      postaviUcitavanje(false)
    }
  }, [filteri, strana])

  useEffect(() => {
    ucitaj()
  }, [ucitaj])

  function promeni(polje) {
    return (dogadjaj) => {
      postaviStranu(0)
      postaviFiltere((prethodno) => ({ ...prethodno, [polje]: dogadjaj.target.value }))
    }
  }

  const imaFilter = Object.values(filteri).some(Boolean)

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">{jeOsoblje ? 'Red podrške' : 'Vaše prijave'}</span>
          <h1 className="natpis">{jeOsoblje ? 'Svi tiketi' : 'Moji tiketi'}</h1>
        </div>
        <Link to="/tiketi/novi" className="dugme glavno">
          Prijavi problem
        </Link>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>

      <section className="ploca" style={{ marginTop: greska ? 16 : 0 }}>
        <div className="filteri">
          <div className="polje">
            <label htmlFor="f-pretraga">Pretraga</label>
            <input
              id="f-pretraga"
              type="search"
              placeholder="naslov ili opis"
              value={filteri.term}
              onChange={promeni('term')}
            />
          </div>

          <div className="polje">
            <label htmlFor="f-status">Status</label>
            <select id="f-status" value={filteri.status} onChange={promeni('status')}>
              <option value="">svi</option>
              {REDOSLED_STATUSA.map((status) => (
                <option key={status} value={status}>
                  {NAZIV_STATUSA[status]}
                </option>
              ))}
            </select>
          </div>

          <div className="polje">
            <label htmlFor="f-kategorija">Kategorija</label>
            <select id="f-kategorija" value={filteri.categoryId} onChange={promeni('categoryId')}>
              <option value="">sve</option>
              {kategorije.map((k) => (
                <option key={k.id} value={k.id}>
                  {k.name}
                </option>
              ))}
            </select>
          </div>

          <div className="polje">
            <label htmlFor="f-prioritet">Prioritet</label>
            <select id="f-prioritet" value={filteri.priorityId} onChange={promeni('priorityId')}>
              <option value="">svi</option>
              {prioriteti.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name}
                </option>
              ))}
            </select>
          </div>
        </div>

        <ListaTiketa
          stranica={stranica}
          ucitavanje={ucitavanje}
          naStranu={postaviStranu}
          praznoNaslov={imaFilter ? 'Nijedan tiket ne odgovara filterima' : 'Red je prazan'}
          praznoOpis={
            imaFilter
              ? 'Promenite ili poništite filtere da vidite više tiketa.'
              : 'Kada neko prijavi problem, tiket će se pojaviti ovde.'
          }
          praznoDodatak={
            imaFilter ? (
              <button
                type="button"
                className="dugme sporedno"
                onClick={() => postaviFiltere({ status: '', categoryId: '', priorityId: '', term: '' })}
              >
                Poništi filtere
              </button>
            ) : (
              <Link to="/tiketi/novi" className="dugme glavno">
                Prijavi problem
              </Link>
            )
          }
        />
      </section>
    </>
  )
}
