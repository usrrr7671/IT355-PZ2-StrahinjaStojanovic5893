import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { tiketi } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import ListaTiketa from '../komponente/ListaTiketa'
import { Poruka } from '../komponente/Osnovno'

/**
 * Radni red agenta - tiketi koji su dodeljeni njemu.
 * Ruta je na serveru otvorena samo za uloge AGENT i ADMIN.
 */
export default function RadniRed() {
  const [stranica, postaviStranicu] = useState(null)
  const [strana, postaviStranu] = useState(0)
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')

  useEffect(() => {
    let otkazano = false
    postaviUcitavanje(true)
    tiketi
      .dodeljeniMeni({ page: strana, size: 15 })
      .then((odgovor) => {
        if (!otkazano) postaviStranicu(odgovor)
      })
      .catch((e) => {
        if (!otkazano) postaviGresku(porukaGreske(e))
      })
      .finally(() => {
        if (!otkazano) postaviUcitavanje(false)
      })
    return () => {
      otkazano = true
    }
  }, [strana])

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Zaduženi ste za njih</span>
          <h1 className="natpis">Moj radni red</h1>
        </div>
        <Link to="/tiketi" className="dugme sporedno">
          Otvori ceo red
        </Link>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>

      <section className="ploca" style={{ marginTop: greska ? 16 : 0 }}>
        <ListaTiketa
          stranica={stranica}
          ucitavanje={ucitavanje}
          naStranu={postaviStranu}
          praznoNaslov="Nemate zaduženja"
          praznoOpis="Preuzmite tiket iz zajedničkog reda i pojaviće se ovde."
          praznoDodatak={
            <Link to="/tiketi" className="dugme glavno">
              Otvori red tiketa
            </Link>
          }
        />
      </section>
    </>
  )
}
