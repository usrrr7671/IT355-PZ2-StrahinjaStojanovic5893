import { useCallback, useEffect, useState } from 'react'
import { administracija } from '../api/servisi'
import { greskePolja, porukaGreske } from '../api/klijent'
import { useAuth } from '../kontekst/AuthKontekst'
import { Ploca, Polje, Poruka, Ucitavanje } from '../komponente/Osnovno'
import { datum, nazivUloge } from '../util/format'

const ULOGE = ['USER', 'AGENT', 'ADMIN']

/**
 * Administracija naloga. Ovde nastaju nalozi agenata - kroz javnu registraciju
 * to nije moguce, jer ona uvek dodeljuje ulogu USER.
 *
 * Nalozi se ne brisu nego deaktiviraju, da tiketi, komentari i poruke koje je
 * taj nalog ostavio ne ostanu bez autora.
 */
export default function AdminNalozi() {
  const { korisnik } = useAuth()

  const [stranica, postaviStranicu] = useState(null)
  const [strana, postaviStranu] = useState(0)
  const [pretraga, postaviPretragu] = useState('')
  const [filterUloge, postaviFilterUloge] = useState('')
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')
  const [obavestenje, postaviObavestenje] = useState('')

  const [novi, postaviNovog] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
    role: 'AGENT',
  })
  const [poGreske, postaviPoGreske] = useState({})
  const [salje, postaviSalje] = useState(false)

  const ucitaj = useCallback(async () => {
    postaviUcitavanje(true)
    try {
      const parametri = { page: strana, size: 15 }
      if (pretraga.trim()) parametri.term = pretraga.trim()
      if (filterUloge) parametri.role = filterUloge
      postaviStranicu(await administracija.nalozi(parametri))
      postaviGresku('')
    } catch (e) {
      postaviGresku(porukaGreske(e))
    } finally {
      postaviUcitavanje(false)
    }
  }, [strana, pretraga, filterUloge])

  useEffect(() => {
    ucitaj()
  }, [ucitaj])

  async function kreiraj(dogadjaj) {
    dogadjaj.preventDefault()
    postaviGresku('')
    postaviObavestenje('')
    postaviPoGreske({})
    postaviSalje(true)
    try {
      const nalog = await administracija.kreirajNalog(novi)
      postaviNovog({ fullName: '', username: '', email: '', password: '', role: 'AGENT' })
      postaviObavestenje(`Nalog „${nalog.username}” je otvoren.`)
      ucitaj()
    } catch (e) {
      postaviGresku(porukaGreske(e))
      postaviPoGreske(greskePolja(e))
    } finally {
      postaviSalje(false)
    }
  }

  async function promeniUlogu(nalog, uloga) {
    try {
      await administracija.izmeniNalog(nalog.id, { role: uloga })
      postaviObavestenje(`${nalog.fullName} je sada ${nazivUloge(uloga).toLowerCase()}.`)
      ucitaj()
    } catch (e) {
      postaviGresku(porukaGreske(e))
    }
  }

  async function prebaciAktivnost(nalog) {
    try {
      if (nalog.active) {
        await administracija.deaktiviraj(nalog.id)
        postaviObavestenje(`Nalog „${nalog.username}” je deaktiviran.`)
      } else {
        await administracija.izmeniNalog(nalog.id, { active: true })
        postaviObavestenje(`Nalog „${nalog.username}” je ponovo aktivan.`)
      }
      ucitaj()
    } catch (e) {
      postaviGresku(porukaGreske(e))
    }
  }

  function promena(polje) {
    return (dogadjaj) =>
      postaviNovog((prethodno) => ({ ...prethodno, [polje]: dogadjaj.target.value }))
  }

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Administracija</span>
          <h1 className="natpis">Nalozi</h1>
        </div>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>
      <Poruka vrsta="uspeh">{obavestenje}</Poruka>

      <div className="dvostubac" style={{ marginTop: greska || obavestenje ? 16 : 0 }}>
        <section className="ploca">
          <div className="filteri">
            <div className="polje">
              <label htmlFor="a-pretraga">Pretraga</label>
              <input
                id="a-pretraga"
                type="search"
                placeholder="ime, korisničko ime ili e-adresa"
                value={pretraga}
                onChange={(d) => {
                  postaviStranu(0)
                  postaviPretragu(d.target.value)
                }}
              />
            </div>
            <div className="polje">
              <label htmlFor="a-uloga">Uloga</label>
              <select
                id="a-uloga"
                value={filterUloge}
                onChange={(d) => {
                  postaviStranu(0)
                  postaviFilterUloge(d.target.value)
                }}
              >
                <option value="">sve</option>
                {ULOGE.map((uloga) => (
                  <option key={uloga} value={uloga}>
                    {nazivUloge(uloga)}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {ucitavanje ? (
            <Ucitavanje />
          ) : (
            <>
              <div className="premotaj">
                <table className="tabela">
                  <thead>
                    <tr>
                      <th>Nalog</th>
                      <th>Uloga</th>
                      <th>Otvoren</th>
                      <th style={{ textAlign: 'right' }}>Stanje</th>
                    </tr>
                  </thead>
                  <tbody>
                    {stranica?.content.map((nalog) => {
                      const jaSam = nalog.id === korisnik?.id
                      return (
                        <tr key={nalog.id} style={{ opacity: nalog.active ? 1 : 0.55 }}>
                          <td>
                            <div style={{ fontWeight: 600 }}>{nalog.fullName}</div>
                            <div className="sitno tiho">
                              {nalog.username} · {nalog.email}
                            </div>
                          </td>
                          <td>
                            <select
                              value={nalog.role}
                              disabled={jaSam}
                              title={
                                jaSam ? 'Sopstvenu ulogu ne možete promeniti' : 'Promena uloge'
                              }
                              onChange={(d) => promeniUlogu(nalog, d.target.value)}
                              style={{ padding: '4px 8px', fontSize: 13 }}
                            >
                              {ULOGE.map((uloga) => (
                                <option key={uloga} value={uloga}>
                                  {nazivUloge(uloga)}
                                </option>
                              ))}
                            </select>
                          </td>
                          <td className="podatak sitno tiho">{datum(nalog.createdAt)}</td>
                          <td style={{ textAlign: 'right' }}>
                            {jaSam ? (
                              <span className="sitno tiho">vaš nalog</span>
                            ) : (
                              <button
                                type="button"
                                className={`veza-dugme ${nalog.active ? 'opasna' : ''}`.trim()}
                                onClick={() => prebaciAktivnost(nalog)}
                              >
                                {nalog.active ? 'Deaktiviraj' : 'Aktiviraj'}
                              </button>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>

              {stranica && stranica.totalPages > 1 && (
                <div className="stranicenje">
                  <span>
                    Strana {stranica.page + 1} od {stranica.totalPages} · ukupno{' '}
                    {stranica.totalElements}
                  </span>
                  <div className="potez">
                    <button
                      type="button"
                      className="dugme sporedno sitno-dugme"
                      disabled={stranica.first}
                      onClick={() => postaviStranu(strana - 1)}
                    >
                      Prethodna
                    </button>
                    <button
                      type="button"
                      className="dugme sporedno sitno-dugme"
                      disabled={stranica.last}
                      onClick={() => postaviStranu(strana + 1)}
                    >
                      Sledeća
                    </button>
                  </div>
                </div>
              )}
            </>
          )}
        </section>

        <Ploca naslov="Otvori nalog">
          <form onSubmit={kreiraj} className="stubac" noValidate>
            <Polje oznaka="Ime i prezime" greska={poGreske.fullName}>
              <input type="text" value={novi.fullName} onChange={promena('fullName')} required />
            </Polje>
            <Polje oznaka="Korisničko ime" greska={poGreske.username}>
              <input type="text" value={novi.username} onChange={promena('username')} required />
            </Polje>
            <Polje oznaka="E-adresa" greska={poGreske.email}>
              <input type="email" value={novi.email} onChange={promena('email')} required />
            </Polje>
            <Polje
              oznaka="Početna lozinka"
              greska={poGreske.password}
              pomoc="Najmanje 6 karaktera. Korisnik je menja iz svog profila."
            >
              <input
                type="text"
                value={novi.password}
                onChange={promena('password')}
                required
              />
            </Polje>
            <Polje oznaka="Uloga" greska={poGreske.role}>
              <select value={novi.role} onChange={promena('role')}>
                {ULOGE.map((uloga) => (
                  <option key={uloga} value={uloga}>
                    {nazivUloge(uloga)}
                  </option>
                ))}
              </select>
            </Polje>
            <button type="submit" className="dugme glavno" disabled={salje}>
              {salje ? 'Otvaranje…' : 'Otvori nalog'}
            </button>
          </form>
        </Ploca>
      </div>
    </>
  )
}
