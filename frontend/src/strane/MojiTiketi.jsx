import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { tiketi } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import ListaTiketa from '../komponente/ListaTiketa'
import { Poruka } from '../komponente/Osnovno'

/** Tiketi koje je prijavio ulogovani nalog, bez obzira na njegovu ulogu. */
export default function MojiTiketi() {
  const [stranica, postaviStranicu] = useState(null)
  const [strana, postaviStranu] = useState(0)
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')

  useEffect(() => {
    let otkazano = false
    postaviUcitavanje(true)
    tiketi
      .moji({ page: strana, size: 15 })
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
          <span className="nadnaslov">Prijavili ste ih vi</span>
          <h1 className="natpis">Moje prijave</h1>
        </div>
        <Link to="/tiketi/novi" className="dugme glavno">
          Prijavi problem
        </Link>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>

      <section className="ploca" style={{ marginTop: greska ? 16 : 0 }}>
        <ListaTiketa
          stranica={stranica}
          ucitavanje={ucitavanje}
          naStranu={postaviStranu}
          praznoNaslov="Niste prijavili nijedan problem"
          praznoOpis="Kada nešto ne radi, opišite problem — podrška ga preuzima iz reda."
          praznoDodatak={
            <Link to="/tiketi/novi" className="dugme glavno">
              Prijavi problem
            </Link>
          }
        />
      </section>
    </>
  )
}
