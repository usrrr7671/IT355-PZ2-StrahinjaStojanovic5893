import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../kontekst/AuthKontekst'
import { greskePolja, porukaGreske } from '../api/klijent'
import { Poruka, Polje } from '../komponente/Osnovno'

/**
 * Javna registracija uvek pravi nalog sa ulogom USER. Uloga se ne salje sa
 * obrasca - da se salje, bilo bi dovoljno izmeniti zahtev u pregledacu i
 * napraviti sebi administratorski nalog. Naloge agenata otvara administrator.
 */
export default function Registracija() {
  const { registracija, jePrijavljen } = useAuth()
  const navigacija = useNavigate()

  const [podaci, postaviPodatke] = useState({
    fullName: '',
    username: '',
    email: '',
    password: '',
  })
  const [greska, postaviGresku] = useState('')
  const [poGreske, postaviPoGreske] = useState({})
  const [salje, postaviSalje] = useState(false)

  if (jePrijavljen) return <Navigate to="/" replace />

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
      await registracija(podaci)
      navigacija('/', { replace: true })
    } catch (e) {
      postaviGresku(porukaGreske(e))
      postaviPoGreske(greskePolja(e))
    } finally {
      postaviSalje(false)
    }
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
            Otvorite
            <br />
            nalog
          </h1>
          <p className="kapija-podnaslov">
            Nalog vam daje pravo da prijavite kvar, pratite svoje tikete i pišete
            agentima podrške. Naloge agenata i administratora otvara administrator.
          </p>
        </div>
      </section>

      <section className="kapija-desno">
        <div className="obrazac">
          <span className="nadnaslov">Novi nalog</span>
          <h2 className="natpis">Nekoliko podataka</h2>

          <form onSubmit={posalji} className="stubac" noValidate>
            <Poruka vrsta="greska">{greska}</Poruka>

            <Polje oznaka="Ime i prezime" greska={poGreske.fullName}>
              <input type="text" value={podaci.fullName} onChange={promena('fullName')} autoFocus required />
            </Polje>

            <Polje
              oznaka="Korisničko ime"
              greska={poGreske.username}
              pomoc="Najmanje 3 karaktera. Njime se prijavljujete."
            >
              <input
                type="text"
                value={podaci.username}
                onChange={promena('username')}
                autoComplete="username"
                required
              />
            </Polje>

            <Polje oznaka="E-adresa" greska={poGreske.email}>
              <input
                type="email"
                value={podaci.email}
                onChange={promena('email')}
                autoComplete="email"
                required
              />
            </Polje>

            <Polje oznaka="Lozinka" greska={poGreske.password} pomoc="Najmanje 6 karaktera.">
              <input
                type="password"
                value={podaci.password}
                onChange={promena('password')}
                autoComplete="new-password"
                required
              />
            </Polje>

            <button type="submit" className="dugme glavno" disabled={salje}>
              {salje ? 'Otvaranje naloga…' : 'Otvori nalog'}
            </button>

            <p className="sitno tiho">
              Već imate nalog? <Link to="/prijava">Prijavite se.</Link>
            </p>
          </form>
        </div>
      </section>
    </div>
  )
}
