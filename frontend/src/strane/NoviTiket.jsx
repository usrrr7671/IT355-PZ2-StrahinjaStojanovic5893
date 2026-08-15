import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { kategorije as apiKategorije, prioriteti as apiPrioriteti, tiketi } from '../api/servisi'
import { greskePolja, porukaGreske } from '../api/klijent'
import { Ploca, Polje, Poruka, Ucitavanje } from '../komponente/Osnovno'

/**
 * Prijava novog tiketa.
 *
 * Prijavilac se ne bira u obrascu - server ga uzima iz tokena, pa nije moguce
 * prijaviti tiket u tudje ime. Isto vazi i za pocetni status: novi tiket uvek
 * krece iz stanja NEW.
 */
export default function NoviTiket() {
  const navigacija = useNavigate()

  const [kategorije, postaviKategorije] = useState([])
  const [prioriteti, postaviPrioritete] = useState([])
  const [podaci, postaviPodatke] = useState({
    title: '',
    description: '',
    categoryId: '',
    priorityId: '',
  })
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [salje, postaviSalje] = useState(false)
  const [greska, postaviGresku] = useState('')
  const [poGreske, postaviPoGreske] = useState({})

  useEffect(() => {
    Promise.all([apiKategorije.lista(), apiPrioriteti.lista()])
      .then(([k, p]) => {
        postaviKategorije(k)
        postaviPrioritete(p)
        // Podrazumeva se srednji prioritet, da korisnik ne bira "najhitnije" iz navike.
        const podrazumevani = p.find((s) => s.level === 2) ?? p[0]
        postaviPodatke((prethodno) => ({
          ...prethodno,
          categoryId: k[0] ? String(k[0].id) : '',
          priorityId: podrazumevani ? String(podrazumevani.id) : '',
        }))
      })
      .catch((e) => postaviGresku(porukaGreske(e)))
      .finally(() => postaviUcitavanje(false))
  }, [])

  function promena(polje) {
    return (dogadjaj) =>
      postaviPodatke((prethodno) => ({ ...prethodno, [polje]: dogadjaj.target.value }))
  }

  async function posalji(dogadjaj) {
    dogadjaj.preventDefault()
    postaviGresku('')
    postaviPoGreske({})
    postaviSalje(true)
    try {
      const tiket = await tiketi.kreiraj({
        title: podaci.title,
        description: podaci.description,
        categoryId: Number(podaci.categoryId),
        priorityId: Number(podaci.priorityId),
      })
      navigacija(`/tiketi/${tiket.id}`, { replace: true })
    } catch (e) {
      postaviGresku(porukaGreske(e))
      postaviPoGreske(greskePolja(e))
    } finally {
      postaviSalje(false)
    }
  }

  if (ucitavanje) return <Ucitavanje tekst="Priprema obrasca" />

  const izabraniPrioritet = prioriteti.find((p) => String(p.id) === podaci.priorityId)

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Nova prijava</span>
          <h1 className="natpis">Prijavi problem</h1>
        </div>
        <Link to="/tiketi" className="dugme sporedno">
          Odustani
        </Link>
      </header>

      <div style={{ maxWidth: 720 }}>
        <Ploca naslov="Šta ne radi">
          <form onSubmit={posalji} className="stubac" noValidate>
            <Poruka vrsta="greska">{greska}</Poruka>

            <Polje
              oznaka="Naslov"
              greska={poGreske.title}
              pomoc="Jedna rečenica koja opisuje problem. Vidi se u redu podrške."
            >
              <input
                type="text"
                value={podaci.title}
                onChange={promena('title')}
                maxLength={150}
                autoFocus
                required
              />
            </Polje>

            <Polje
              oznaka="Opis"
              greska={poGreske.description}
              pomoc="Šta ste radili, šta se dogodilo i šta ste očekivali. Poruka o grešci pomaže."
            >
              <textarea
                value={podaci.description}
                onChange={promena('description')}
                maxLength={4000}
                rows={8}
                required
              />
            </Polje>

            <div className="red-polja">
              <Polje oznaka="Kategorija" greska={poGreske.categoryId}>
                <select value={podaci.categoryId} onChange={promena('categoryId')} required>
                  {kategorije.map((k) => (
                    <option key={k.id} value={k.id}>
                      {k.name}
                    </option>
                  ))}
                </select>
              </Polje>

              <Polje
                oznaka="Prioritet"
                greska={poGreske.priorityId}
                pomoc={
                  izabraniPrioritet
                    ? `Rok odziva: ${izabraniPrioritet.slaHours} h`
                    : undefined
                }
              >
                <select value={podaci.priorityId} onChange={promena('priorityId')} required>
                  {prioriteti.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </Polje>
            </div>

            <div className="potez">
              <button type="submit" className="dugme glavno" disabled={salje}>
                {salje ? 'Slanje…' : 'Pošalji prijavu'}
              </button>
              <Link to="/tiketi" className="dugme sporedno">
                Odustani
              </Link>
            </div>
          </form>
        </Ploca>
      </div>
    </>
  )
}
