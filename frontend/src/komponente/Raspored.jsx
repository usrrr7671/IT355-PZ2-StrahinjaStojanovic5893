import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../kontekst/AuthKontekst'
import { prepiske } from '../api/servisi'
import { nazivUloge } from '../util/format'

/**
 * Osnovni raspored prijavljenog dela aplikacije: tamna traka sa navigacijom
 * levo, sadrzaj desno. Stavke menija se biraju prema ulozi - korisnik ne
 * vidi radni red agenta niti administraciju.
 */
export default function Raspored() {
  const { korisnik, jeOsoblje, jeAdmin, odjava } = useAuth()
  const navigacija = useNavigate()
  const lokacija = useLocation()
  const [neprocitano, postaviNeprocitano] = useState(0)

  // Znacka sa brojem neprocitanih poruka se osvezava pri svakoj promeni strane,
  // pa se broj menja odmah po otvaranju prepiske.
  useEffect(() => {
    let otkazano = false
    prepiske
      .neprocitane()
      .then((odgovor) => {
        if (!otkazano) postaviNeprocitano(odgovor.unreadCount ?? 0)
      })
      .catch(() => {})
    return () => {
      otkazano = true
    }
  }, [lokacija.pathname])

  function odjaviSe() {
    odjava()
    navigacija('/prijava', { replace: true })
  }

  return (
    <div className="ljuska">
      <nav className="traka">
        <NavLink to="/" className="znak">
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
        </NavLink>

        <div className="grupa-veza">
          <Stavka to="/" tacno>
            Pregled
          </Stavka>
          <Stavka to="/tiketi">Svi tiketi</Stavka>
          <Stavka to="/moji-tiketi">Moje prijave</Stavka>
          {jeOsoblje && <Stavka to="/radni-red">Moj radni red</Stavka>}
          <Stavka to="/poruke" znacka={neprocitano}>
            Poruke
          </Stavka>
        </div>

        {jeAdmin && (
          <div className="grupa-veza">
            <span className="naslov-grupe">Administracija</span>
            <Stavka to="/admin/nalozi">Nalozi</Stavka>
            <Stavka to="/admin/sifarnici">Šifarnici</Stavka>
          </div>
        )}

        <div className="nalog">
          <div>
            <div className="nalog-ime">{korisnik?.fullName}</div>
            <div className="nalog-uloga">{nazivUloge(korisnik?.role)}</div>
          </div>
          <button type="button" className="odjava" onClick={odjaviSe}>
            Odjavi se
          </button>
        </div>
      </nav>

      <main className="sadrzaj">
        <div className="okvir">
          <Outlet />
        </div>
      </main>
    </div>
  )
}

function Stavka({ to, tacno = false, znacka = 0, children }) {
  return (
    <NavLink
      to={to}
      end={tacno}
      className={({ isActive }) => (isActive ? 'veza tekuca' : 'veza')}
    >
      <span>{children}</span>
      {znacka > 0 && <span className="znacka">{znacka}</span>}
    </NavLink>
  )
}
