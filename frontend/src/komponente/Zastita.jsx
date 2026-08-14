import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../kontekst/AuthKontekst'
import { Ucitavanje } from './Osnovno'

/**
 * Zastita ruta na strani pregledaca.
 *
 * Ovo je samo udobnost: sprecava da korisnik otvori ekran koji mu ionako ne
 * bi vratio podatke. Prava zastita je na serveru - i da neko rucno upise
 * adresu /admin/nalozi, API bi na svaki zahtev odgovorio sa 403.
 */
export function Zasticena({ children }) {
  const { jePrijavljen, ucitavanje } = useAuth()
  const lokacija = useLocation()

  if (ucitavanje) return <Ucitavanje tekst="Provera sesije" />
  if (!jePrijavljen) return <Navigate to="/prijava" state={{ odakle: lokacija }} replace />
  return children
}

export function SamoOsoblje({ children }) {
  const { jeOsoblje, ucitavanje } = useAuth()
  if (ucitavanje) return <Ucitavanje tekst="Provera sesije" />
  return jeOsoblje ? children : <Zabranjeno />
}

export function SamoAdmin({ children }) {
  const { jeAdmin, ucitavanje } = useAuth()
  if (ucitavanje) return <Ucitavanje tekst="Provera sesije" />
  return jeAdmin ? children : <Zabranjeno />
}

function Zabranjeno() {
  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Pristup odbijen</span>
          <h1 className="natpis">Nemate pravo na ovu stranu</h1>
        </div>
      </header>
      <section className="ploca">
        <div className="ploca-telo">
          <p className="blago">
            Ova strana je otvorena samo za određene uloge. Ako vam je pristup potreban,
            zatražite ga od administratora sistema.
          </p>
        </div>
      </section>
    </>
  )
}
