import { useCallback, useEffect, useState } from 'react'
import { kategorije as apiKategorije, prioriteti as apiPrioriteti } from '../api/servisi'
import { greskePolja, porukaGreske } from '../api/klijent'
import { Ploca, Polje, Poruka, Ucitavanje } from '../komponente/Osnovno'

/**
 * Kategorije i prioriteti - sifarnici koje vodi administrator.
 *
 * Kategorija koja je vec upotrebljena na tiketima se ne brise nego povlaci iz
 * upotrebe: brisanjem bi postojeci tiketi ostali bez kategorije. Backend to i
 * radi sam, pa se ovde brisanje nudi kao jedna radnja.
 */
export default function AdminSifarnici() {
  const [kategorije, postaviKategorije] = useState([])
  const [prioriteti, postaviPrioritete] = useState([])
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')
  const [obavestenje, postaviObavestenje] = useState('')

  const ucitaj = useCallback(async () => {
    try {
      const [k, p] = await Promise.all([apiKategorije.lista(false), apiPrioriteti.lista()])
      postaviKategorije(k)
      postaviPrioritete(p)
      postaviGresku('')
    } catch (e) {
      postaviGresku(porukaGreske(e))
    } finally {
      postaviUcitavanje(false)
    }
  }, [])

  useEffect(() => {
    ucitaj()
  }, [ucitaj])

  /**
   * Sve izmene sifarnika prolaze kroz istu putanju: izvrsi, javi, osvezi.
   * Kroz `naGresku` obrazac dobija poruke pojedinacnih polja, da bi se ispisale
   * tacno ispod polja na koje se odnose.
   */
  async function radnja(posao, poruka, naGresku) {
    postaviGresku('')
    postaviObavestenje('')
    try {
      await posao()
      postaviObavestenje(poruka)
      await ucitaj()
      return true
    } catch (e) {
      postaviGresku(porukaGreske(e))
      naGresku?.(e)
      return false
    }
  }

  if (ucitavanje) return <Ucitavanje />

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Administracija</span>
          <h1 className="natpis">Šifarnici</h1>
        </div>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>
      <Poruka vrsta="uspeh">{obavestenje}</Poruka>

      <div className="dvostubac" style={{ marginTop: greska || obavestenje ? 16 : 0, gridTemplateColumns: 'minmax(0, 1fr) minmax(0, 1fr)' }}>
        <Kategorije stavke={kategorije} radnja={radnja} />
        <Prioriteti stavke={prioriteti} radnja={radnja} />
      </div>
    </>
  )
}

function Kategorije({ stavke, radnja }) {
  const [nova, postaviNovu] = useState({ name: '', description: '' })
  const [poGreske, postaviPoGreske] = useState({})

  async function dodaj(dogadjaj) {
    dogadjaj.preventDefault()
    postaviPoGreske({})
    const uspelo = await radnja(
      () => apiKategorije.kreiraj({ ...nova, active: true }),
      `Kategorija „${nova.name}” je dodata.`,
      (e) => postaviPoGreske(greskePolja(e)),
    )
    if (uspelo) postaviNovu({ name: '', description: '' })
  }

  return (
    <Ploca naslov={`Kategorije (${stavke.length})`}>
      <div className="premotaj">
        <table className="tabela">
          <thead>
            <tr>
              <th>Naziv</th>
              <th style={{ textAlign: 'right' }}>Stanje</th>
            </tr>
          </thead>
          <tbody>
            {stavke.map((k) => (
              <tr key={k.id} style={{ opacity: k.active ? 1 : 0.55 }}>
                <td>
                  <div style={{ fontWeight: 600 }}>{k.name}</div>
                  {k.description && <div className="sitno tiho">{k.description}</div>}
                </td>
                <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                  {k.active ? (
                    <button
                      type="button"
                      className="veza-dugme opasna"
                      onClick={() =>
                        radnja(
                          () => apiKategorije.obrisi(k.id),
                          `Kategorija „${k.name}” je povučena iz upotrebe.`,
                        )
                      }
                    >
                      Povuci
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="veza-dugme"
                      onClick={() =>
                        radnja(
                          () => apiKategorije.izmeni(k.id, { name: k.name, description: k.description, active: true }),
                          `Kategorija „${k.name}” je vraćena u upotrebu.`,
                        )
                      }
                    >
                      Vrati
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form onSubmit={dodaj} className="stubac" style={{ marginTop: 20 }} noValidate>
        <Polje oznaka="Nova kategorija" greska={poGreske.name}>
          <input
            type="text"
            value={nova.name}
            onChange={(d) => postaviNovu((p) => ({ ...p, name: d.target.value }))}
            placeholder="npr. Mrežni problemi"
            required
          />
        </Polje>
        <Polje oznaka="Opis" greska={poGreske.description}>
          <input
            type="text"
            value={nova.description}
            onChange={(d) => postaviNovu((p) => ({ ...p, description: d.target.value }))}
            placeholder="kratko objašnjenje za korisnike"
          />
        </Polje>
        <button type="submit" className="dugme sporedno" disabled={!nova.name.trim()}>
          Dodaj kategoriju
        </button>
      </form>
    </Ploca>
  )
}

function Prioriteti({ stavke, radnja }) {
  const [novi, postaviNovi] = useState({ name: '', level: 2, slaHours: 24 })
  const [poGreske, postaviPoGreske] = useState({})

  async function dodaj(dogadjaj) {
    dogadjaj.preventDefault()
    postaviPoGreske({})
    const uspelo = await radnja(
      () =>
        apiPrioriteti.kreiraj({
          name: novi.name,
          level: Number(novi.level),
          slaHours: Number(novi.slaHours),
        }),
      `Prioritet „${novi.name}” je dodat.`,
      (e) => postaviPoGreske(greskePolja(e)),
    )
    if (uspelo) postaviNovi({ name: '', level: 2, slaHours: 24 })
  }

  return (
    <Ploca naslov={`Prioriteti (${stavke.length})`}>
      <div className="premotaj">
        <table className="tabela">
          <thead>
            <tr>
              <th>Naziv</th>
              <th>Nivo</th>
              <th>Rok</th>
              <th style={{ textAlign: 'right' }} />
            </tr>
          </thead>
          <tbody>
            {stavke.map((p) => (
              <tr key={p.id}>
                <td style={{ fontWeight: 600 }}>{p.name}</td>
                <td className="podatak">{p.level}</td>
                <td className="podatak">{p.slaHours} h</td>
                <td style={{ textAlign: 'right' }}>
                  <button
                    type="button"
                    className="veza-dugme opasna"
                    onClick={() =>
                      radnja(() => apiPrioriteti.obrisi(p.id), `Prioritet „${p.name}” je obrisan.`)
                    }
                  >
                    Obriši
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <form onSubmit={dodaj} className="stubac" style={{ marginTop: 20 }} noValidate>
        <Polje oznaka="Novi prioritet" greska={poGreske.name}>
          <input
            type="text"
            value={novi.name}
            onChange={(d) => postaviNovi((p) => ({ ...p, name: d.target.value }))}
            placeholder="npr. Kritičan"
            required
          />
        </Polje>
        <div className="red-polja">
          <Polje oznaka="Nivo (1–10)" greska={poGreske.level} pomoc="Manji broj = viši prioritet.">
            <input
              type="number"
              min={1}
              max={10}
              value={novi.level}
              onChange={(d) => postaviNovi((p) => ({ ...p, level: d.target.value }))}
              required
            />
          </Polje>
          <Polje oznaka="Rok odziva (h)" greska={poGreske.slaHours}>
            <input
              type="number"
              min={1}
              max={720}
              value={novi.slaHours}
              onChange={(d) => postaviNovi((p) => ({ ...p, slaHours: d.target.value }))}
              required
            />
          </Polje>
        </div>
        <button type="submit" className="dugme sporedno" disabled={!novi.name.trim()}>
          Dodaj prioritet
        </button>
      </form>
    </Ploca>
  )
}
