import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { izvestaji, tiketi } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import { useAuth } from '../kontekst/AuthKontekst'
import ListaTiketa from '../komponente/ListaTiketa'
import { Ploca, Poruka, Ucitavanje } from '../komponente/Osnovno'
import { nazivStatusa, REDOSLED_STATUSA } from '../util/format'

/**
 * Pocetni ekran. Nije isti za sve: agentu i administratoru pokazuje stanje
 * celog reda, a korisniku samo njegove tikete - njemu brojevi tudjih tiketa
 * ne znace nista, a i API za izvestaje mu je zatvoren.
 */
export default function Pregled() {
  const { korisnik, jeOsoblje } = useAuth()

  const [statistika, postaviStatistiku] = useState(null)
  const [stranica, postaviStranicu] = useState(null)
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')

  useEffect(() => {
    let otkazano = false

    const poslovi = jeOsoblje
      ? [izvestaji.pregled(), tiketi.pretraga({ page: 0, size: 8 })]
      : [Promise.resolve(null), tiketi.moji({ page: 0, size: 8 })]

    Promise.all(poslovi)
      .then(([stat, lista]) => {
        if (otkazano) return
        postaviStatistiku(stat)
        postaviStranicu(lista)
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
  }, [jeOsoblje])

  if (ucitavanje) return <Ucitavanje />

  const imeZaPozdrav = korisnik?.fullName?.split(' ')[0] ?? ''

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">
            {jeOsoblje ? 'Stanje službe podrške' : 'Vaš pregled'}
          </span>
          <h1 className="natpis">Dobar dan, {imeZaPozdrav}</h1>
        </div>
        <Link to="/tiketi/novi" className="dugme glavno">
          Prijavi problem
        </Link>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>

      <div className="stubac" style={{ marginTop: greska ? 16 : 0 }}>
        {jeOsoblje && statistika && (
          <>
            <div className="pokazatelji">
              <Pokazatelj broj={statistika.totalTickets} opis="tiketa ukupno" />
              <Pokazatelj
                broj={statistika.unassignedTickets}
                opis="čeka na preuzimanje"
                vrsta="cekanje"
              />
              {/* Nula probijenih rokova je dobra vest, pa se ne boji crveno. */}
              <Pokazatelj
                broj={statistika.slaBreached}
                opis="probijen rok odziva"
                vrsta={statistika.slaBreached > 0 ? 'upozorenje' : ''}
              />
              <Pokazatelj broj={statistika.assignedToMe} opis="zaduženja na vama" vrsta="moje" />
            </div>

            <div className="dvostubac">
              <Ploca naslov="Raspodela po statusima">
                <Raspodela
                  stavke={REDOSLED_STATUSA.map((status) => ({
                    kljuc: status,
                    naziv: nazivStatusa(status),
                    broj: statistika.ticketsByStatus?.[status] ?? 0,
                  }))}
                />
              </Ploca>

              <Ploca naslov="Po kategorijama">
                <Raspodela
                  stavke={Object.entries(statistika.ticketsByCategory ?? {}).map(
                    ([naziv, broj]) => ({ kljuc: naziv, naziv, broj }),
                  )}
                  bezBoje
                />
              </Ploca>
            </div>
          </>
        )}

        {!jeOsoblje && (
          <div className="pokazatelji">
            <Pokazatelj broj={stranica?.totalElements ?? 0} opis="vaših prijava ukupno" />
            <Pokazatelj
              broj={
                stranica?.content?.filter((t) => t.status !== 'CLOSED' && t.status !== 'RESOLVED')
                  .length ?? 0
              }
              opis="još u obradi"
              vrsta="moje"
            />
            <Pokazatelj
              broj={stranica?.content?.filter((t) => t.status === 'RESOLVED').length ?? 0}
              opis="čeka vašu potvrdu"
              vrsta="cekanje"
            />
          </div>
        )}

        <section className="ploca">
          <div className="ploca-glava">
            <h2>{jeOsoblje ? 'Poslednje prijavljeno' : 'Vaše poslednje prijave'}</h2>
            <Link to={jeOsoblje ? '/tiketi' : '/moji-tiketi'} className="veza-dugme">
              Prikaži sve
            </Link>
          </div>
          <ListaTiketa
            stranica={stranica}
            ucitavanje={false}
            praznoNaslov={jeOsoblje ? 'Red je prazan' : 'Niste prijavili nijedan problem'}
            praznoOpis={
              jeOsoblje
                ? 'Kada korisnik prijavi problem, tiket će se pojaviti ovde.'
                : 'Kada nešto ne radi, opišite problem — podrška ga preuzima iz reda.'
            }
            praznoDodatak={
              <Link to="/tiketi/novi" className="dugme glavno">
                Prijavi problem
              </Link>
            }
          />
        </section>
      </div>
    </>
  )
}

function Pokazatelj({ broj, opis, vrsta = '' }) {
  return (
    <div className={`pokazatelj ${vrsta}`.trim()}>
      <div className="broj">{broj}</div>
      <div className="opis">{opis}</div>
    </div>
  )
}

function Raspodela({ stavke, bezBoje = false }) {
  const najveci = Math.max(1, ...stavke.map((s) => s.broj))

  if (stavke.length === 0) {
    return <p className="tiho sitno">Još nema podataka.</p>
  }

  return (
    <div className="raspodela">
      {stavke.map((stavka) => (
        <div
          key={stavka.kljuc}
          className="raspodela-red"
          data-status={bezBoje ? undefined : stavka.kljuc}
        >
          <span className="sitno blago">{stavka.naziv}</span>
          <span className="raspodela-sina">
            <span
              className="raspodela-punjenje"
              style={{
                width: `${(stavka.broj / najveci) * 100}%`,
                background: bezBoje ? 'var(--mastilo-blago)' : 'var(--boja)',
              }}
            />
          </span>
          <span className="raspodela-broj">{stavka.broj}</span>
        </div>
      ))}
    </div>
  )
}
