import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../kontekst/AuthKontekst'
import { greskePolja, porukaGreske } from '../api/klijent'
import { Poruka, Polje } from '../komponente/Osnovno'
import { NAZIV_STATUSA, REDOSLED_STATUSA } from '../util/format'

/*
  Nalozi iz DataSeeder-a. Stoje na ekranu zato sto je ovo projektni zadatak
  koji se brani: onaj ko ga pregleda treba da moze da udje kao sve tri uloge
  bez trazenja lozinki po dokumentaciji.
*/
const NALOZI_ZA_PROBU = [
  { username: 'pera', opis: 'korisnik' },
  { username: 'agent1', opis: 'agent' },
  { username: 'admin', opis: 'administrator' },
]

const LOZINKA_ZA_PROBU = 'lozinka123'

export default function Prijava() {
  const { prijava, jePrijavljen } = useAuth()
  const navigacija = useNavigate()
  const lokacija = useLocation()

  const [podaci, postaviPodatke] = useState({ username: '', password: '' })
  const [greska, postaviGresku] = useState('')
  const [greskePolja_, postaviGreskePolja] = useState({})
  const [salje, postaviSalje] = useState(false)

  if (jePrijavljen) return <Navigate to="/" replace />

  function promena(polje) {
    return (dogadjaj) => postaviPodatke((prethodno) => ({ ...prethodno, [polje]: dogadjaj.target.value }))
  }

  async function posalji(dogadjaj) {
    dogadjaj.preventDefault()
    postaviGresku('')
    postaviGreskePolja({})
    postaviSalje(true)
    try {
      await prijava(podaci)
      navigacija(lokacija.state?.odakle?.pathname ?? '/', { replace: true })
    } catch (e) {
      postaviGresku(porukaGreske(e, 'Pogrešno korisničko ime ili lozinka.'))
      postaviGreskePolja(greskePolja(e))
    } finally {
      postaviSalje(false)
    }
  }

  function popuni(username) {
    postaviPodatke({ username, password: LOZINKA_ZA_PROBU })
  }

  return (
    <div className="kapija">
      <section className="kapija-levo">
        <div>
          <div className="znak" style={{ marginBottom: 0, padding: 0 }}>
            <span className="znak-simbol" aria-hidden="true">
              <span />
              <span />
              <span />
              <span />
            </span>
            <span>
              <span className="znak-ime">Pult</span>
              <span className="znak-opis">Služba podrške</span>
            </span>
          </div>

          <h1 className="kapija-naslov">
            Svaki tiket
            <br />
            ima svoje
            <br />
            mesto u redu
          </h1>
          <p className="kapija-podnaslov">
            Prijavite kvar, pratite dokle se stiglo i pišite osobi koja radi na njemu.
            Ko sme da pomeri tiket dalje, određuje uloga naloga.
          </p>
        </div>

        {/* Donji blok objašnjava sistem u dva reda: kuda tiket ide i ko ga vodi. */}
        <div>
          <span className="nadnaslov">Put tiketa</span>
          <div className="put-tiketa" aria-hidden="true">
            {REDOSLED_STATUSA.map((status) => (
              <div key={status} className="put-korak" data-status={status}>
                <i />
                <span>{NAZIV_STATUSA[status]}</span>
              </div>
            ))}
          </div>

          <span className="nadnaslov">Ko šta sme</span>
          <div className="uloge" style={{ marginTop: 12 }}>
            <div className="uloga" style={{ '--boja': 'var(--st-open)' }}>
              <span className="uloga-ime">Korisnik</span>
              <span className="uloga-opis">Prijavljuje kvar i prati svoje tikete.</span>
            </div>
            <div className="uloga" style={{ '--boja': 'var(--st-inprogress)' }}>
              <span className="uloga-ime">Agent</span>
              <span className="uloga-opis">Preuzima tikete iz reda i menja im status.</span>
            </div>
            <div className="uloga" style={{ '--boja': 'var(--st-new)' }}>
              <span className="uloga-ime">Admin</span>
              <span className="uloga-opis">Vodi naloge, kategorije i prioritete.</span>
            </div>
          </div>
        </div>
      </section>

      <section className="kapija-desno">
        <div className="obrazac">
          <span className="nadnaslov">Prijava na sistem</span>
          <h2 className="natpis">Dobro došli nazad</h2>

          <form onSubmit={posalji} className="stubac" noValidate>
            <Poruka vrsta="greska">{greska}</Poruka>

            <Polje oznaka="Korisničko ime" greska={greskePolja_.username}>
              <input
                type="text"
                value={podaci.username}
                onChange={promena('username')}
                autoComplete="username"
                autoFocus
                required
              />
            </Polje>

            <Polje oznaka="Lozinka" greska={greskePolja_.password}>
              <input
                type="password"
                value={podaci.password}
                onChange={promena('password')}
                autoComplete="current-password"
                required
              />
            </Polje>

            <button type="submit" className="dugme glavno" disabled={salje}>
              {salje ? 'Prijavljivanje…' : 'Prijavi se'}
            </button>

            <p className="sitno tiho">
              Nemate nalog? <Link to="/registracija">Otvorite ga za nekoliko sekundi.</Link>
            </p>
          </form>

          <div className="nalozi-za-probu">
            <span className="nadnaslov">Nalozi za prikaz</span>
            <div style={{ marginTop: 8 }}>
              {NALOZI_ZA_PROBU.map((nalog) => (
                <button key={nalog.username} type="button" onClick={() => popuni(nalog.username)}>
                  <span>{nalog.username}</span>
                  <span className="tiho">{nalog.opis}</span>
                </button>
              ))}
            </div>
            <p className="pomoc" style={{ marginTop: 8 }}>
              Lozinka za sve: {LOZINKA_ZA_PROBU}
            </p>
          </div>
        </div>
      </section>
    </div>
  )
}
