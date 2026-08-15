import { useCallback, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { komentari as apiKomentari, korisnici, prepiske, tiketi } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import { useAuth } from '../kontekst/AuthKontekst'
import ZivotniCiklus from '../komponente/ZivotniCiklus'
import { Ploca, Plocica, Poruka, Sla, Status, Ucitavanje } from '../komponente/Osnovno'
import { datumVreme, nazivStatusa, nazivUloge, relativno } from '../util/format'

export default function DetaljiTiketa() {
  const { id } = useParams()
  const navigacija = useNavigate()
  const { korisnik, jeOsoblje, jeAdmin } = useAuth()

  const [tiket, postaviTiket] = useState(null)
  const [agenti, postaviAgente] = useState([])
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')
  const [obavestenje, postaviObavestenje] = useState('')
  const [zauzeto, postaviZauzeto] = useState(false)

  const [noviKomentar, postaviNoviKomentar] = useState('')
  const [interna, postaviInternu] = useState(false)

  const ucitaj = useCallback(async () => {
    try {
      postaviTiket(await tiketi.jedan(id))
      postaviGresku('')
    } catch (e) {
      postaviGresku(porukaGreske(e, 'Tiket nije pronađen ili nemate pristup njemu.'))
    } finally {
      postaviUcitavanje(false)
    }
  }, [id])

  useEffect(() => {
    postaviUcitavanje(true)
    ucitaj()
  }, [ucitaj])

  useEffect(() => {
    if (!jeOsoblje) return
    korisnici.agenti().then(postaviAgente).catch(() => {})
  }, [jeOsoblje])

  async function izvrsi(radnja, poruka) {
    postaviZauzeto(true)
    postaviGresku('')
    postaviObavestenje('')
    try {
      const osvezen = await radnja()
      if (osvezen) postaviTiket(osvezen)
      else await ucitaj()
      if (poruka) postaviObavestenje(poruka)
    } catch (e) {
      postaviGresku(porukaGreske(e))
    } finally {
      postaviZauzeto(false)
    }
  }

  function promeniStatus(status) {
    const napomena = window.prompt(
      `Prelazak u stanje „${nazivStatusa(status)}”. Napomena za istoriju (nije obavezna):`,
      '',
    )
    if (napomena === null) return
    izvrsi(
      () => tiketi.promeniStatus(id, { status, note: napomena || null }),
      `Tiket je prebačen u stanje „${nazivStatusa(status)}”.`,
    )
  }

  function dodajKomentar(dogadjaj) {
    dogadjaj.preventDefault()
    if (!noviKomentar.trim()) return
    izvrsi(async () => {
      await apiKomentari.dodaj(id, { content: noviKomentar.trim(), internal: interna })
      postaviNoviKomentar('')
      postaviInternu(false)
      return null
    }, interna ? 'Interna beleška je sačuvana.' : 'Komentar je objavljen.')
  }

  async function pisiAgentu(primalac) {
    try {
      const razgovor = await prepiske.zapocni({
        recipientId: primalac.id,
        ticketId: Number(id),
        content: null,
      })
      navigacija(`/poruke?razgovor=${razgovor.id}`)
    } catch (e) {
      postaviGresku(porukaGreske(e))
    }
  }

  if (ucitavanje) return <Ucitavanje tekst="Učitavanje tiketa" />

  if (!tiket) {
    return (
      <>
        <header className="glava-strane">
          <div>
            <span className="nadnaslov">Tiket</span>
            <h1 className="natpis">Nedostupno</h1>
          </div>
          <Link to="/tiketi" className="dugme sporedno">
            Nazad na listu
          </Link>
        </header>
        <Poruka vrsta="greska">{greska}</Poruka>
      </>
    )
  }

  const jePrijavilac = tiket.reporter?.id === korisnik?.id
  const smeDaMenjaStatus = jeOsoblje
  const smeDaPonovoOtvori = jePrijavilac && tiket.status === 'RESOLVED'

  return (
    <>
      <header className="glava-strane">
        <div>
          <Link to="/tiketi" className="nadnaslov" style={{ display: 'inline-block' }}>
            ← Svi tiketi
          </Link>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 8 }}>
            <Plocica id={tiket.id} krupna />
            <Status status={tiket.status} />
          </div>
          <h1 className="natpis" style={{ marginTop: 10, maxWidth: '24ch' }}>
            {tiket.title}
          </h1>
        </div>

        <div className="potez">
          {smeDaPonovoOtvori && (
            <button
              type="button"
              className="dugme sporedno"
              disabled={zauzeto}
              onClick={() => {
                const razlog = window.prompt('Zašto tiket treba ponovo otvoriti?', '')
                if (razlog === null) return
                izvrsi(() => tiketi.ponovoOtvori(id, razlog), 'Tiket je ponovo otvoren.')
              }}
            >
              Ponovo otvori
            </button>
          )}
          {jeAdmin && (
            <button
              type="button"
              className="dugme opasno"
              disabled={zauzeto}
              onClick={() => {
                if (!window.confirm('Trajno obrisati ovaj tiket sa komentarima i istorijom?')) return
                tiketi
                  .obrisi(id)
                  .then(() => navigacija('/tiketi'))
                  .catch((e) => postaviGresku(porukaGreske(e)))
              }}
            >
              Obriši tiket
            </button>
          )}
        </div>
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>
      <Poruka vrsta="uspeh">{obavestenje}</Poruka>

      {/*
        Traka zivotnog ciklusa stoji preko cele sirine, iznad svega ostalog.
        Prvo pitanje koje se postavlja o tiketu jeste dokle se stiglo, pa je i
        odgovor na njega prvo na ekranu - a ne stisnut u bocnu kolonu.
      */}
      <section className="ploca ciklus-ploca" style={{ marginTop: greska || obavestenje ? 16 : 0 }}>
        <div className="ploca-glava">
          <h2>Životni ciklus</h2>
          <span className="sitno tiho">
            {smeDaMenjaStatus
              ? 'Kliknite na dozvoljeno stanje da pomerite tiket'
              : 'Status menja isključivo podrška'}
          </span>
        </div>
        <div className="ploca-telo">
          <ZivotniCiklus
            trenutni={tiket.status}
            dozvoljeni={tiket.allowedTransitions ?? []}
            smePromeniti={smeDaMenjaStatus}
            naPromenu={promeniStatus}
            zauzeto={zauzeto}
          />

          {!smeDaMenjaStatus && (
            <p className="pomoc" style={{ marginTop: 14 }}>
              {smeDaPonovoOtvori
                ? 'Rešen tiket možete ponovo otvoriti dugmetom iznad ako problem i dalje postoji.'
                : 'Ako rešenje ne odgovara, javite se agentu porukom ili komentarom.'}
            </p>
          )}
        </div>
      </section>

      <div className="dvostubac" style={{ marginTop: 24 }}>
        {/* ---------------------------------------------------- levo: prepiska */}
        <div className="stubac">
          <Ploca naslov="Opis problema">
            <p style={{ whiteSpace: 'pre-wrap' }} className="blago">
              {tiket.description}
            </p>
            <p className="sitno tiho" style={{ marginTop: 16 }}>
              Prijavio {tiket.reporter?.fullName} · {datumVreme(tiket.createdAt)}
            </p>
          </Ploca>

          <Ploca
            naslov={`Komentari (${tiket.comments?.length ?? 0})`}
            dodatak={
              jeOsoblje ? (
                <span className="sitno tiho">Interne beleške vidi samo podrška</span>
              ) : null
            }
          >
            {tiket.comments?.length === 0 ? (
              <p className="tiho sitno">
                Još nema komentara. Pitanje ili dopuna opisa ovde pomažu podršci da brže reši
                problem.
              </p>
            ) : (
              <div>
                {tiket.comments.map((komentar) => (
                  <article
                    key={komentar.id}
                    className={`komentar ${komentar.internal ? 'interna' : ''}`.trim()}
                  >
                    <div className="komentar-glava">
                      <span className="komentar-ime">{komentar.author?.fullName}</span>
                      <span className="nadnaslov">{nazivUloge(komentar.author?.role)}</span>
                      <span className="sitno tiho">{datumVreme(komentar.createdAt)}</span>
                      {komentar.internal && (
                        <span className="oznaka" data-status="NEW">
                          interna beleška
                        </span>
                      )}
                      {(komentar.author?.id === korisnik?.id || jeAdmin) && (
                        <button
                          type="button"
                          className="veza-dugme opasna"
                          style={{ marginLeft: 'auto' }}
                          onClick={() => {
                            if (!window.confirm('Obrisati ovaj komentar?')) return
                            izvrsi(async () => {
                              await apiKomentari.obrisi(komentar.id)
                              return null
                            }, 'Komentar je obrisan.')
                          }}
                        >
                          Obriši
                        </button>
                      )}
                    </div>
                    <p className="komentar-tekst">{komentar.content}</p>
                  </article>
                ))}
              </div>
            )}

            <form onSubmit={dodajKomentar} className="stubac" style={{ marginTop: 20 }}>
              <div className="polje">
                <label htmlFor="komentar">Napišite komentar</label>
                <textarea
                  id="komentar"
                  value={noviKomentar}
                  onChange={(d) => postaviNoviKomentar(d.target.value)}
                  placeholder={
                    jeOsoblje
                      ? 'Odgovor korisniku ili beleška za kolege…'
                      : 'Dopuna opisa, odgovor na pitanje agenta…'
                  }
                  maxLength={2000}
                />
              </div>

              <div className="potez" style={{ justifyContent: 'space-between' }}>
                {jeOsoblje ? (
                  <label
                    className="sitno"
                    style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}
                  >
                    <input
                      type="checkbox"
                      checked={interna}
                      onChange={(d) => postaviInternu(d.target.checked)}
                      style={{ width: 'auto' }}
                    />
                    Sačuvaj kao internu belešku
                  </label>
                ) : (
                  <span />
                )}
                <button
                  type="submit"
                  className="dugme glavno"
                  disabled={zauzeto || !noviKomentar.trim()}
                >
                  {interna ? 'Sačuvaj belešku' : 'Objavi komentar'}
                </button>
              </div>
            </form>
          </Ploca>
        </div>

        {/* ------------------------------------------------ desno: radni podaci */}
        <div className="stubac">
          <Ploca naslov="Rok odziva">
            <Sla
              createdAt={tiket.createdAt}
              slaDeadline={tiket.slaDeadline}
              slaBreached={tiket.slaBreached}
              status={tiket.status}
            />
            <p className="pomoc" style={{ marginTop: 10 }}>
              Prioritet „{tiket.priority?.name}” nosi rok od {tiket.priority?.slaHours} h — do{' '}
              {datumVreme(tiket.slaDeadline)}.
            </p>
          </Ploca>

          <Ploca naslov="Podaci">
            <div className="spisak-podataka">
              <div>
                <span className="kljuc">Kategorija</span>
                <span className="vrednost">{tiket.category?.name}</span>
              </div>
              <div>
                <span className="kljuc">Prioritet</span>
                <span className="vrednost">{tiket.priority?.name}</span>
              </div>
              <div>
                <span className="kljuc">Prijavio</span>
                <span className="vrednost">{tiket.reporter?.fullName}</span>
              </div>
              <div>
                <span className="kljuc">Zadužen</span>
                <span className="vrednost">{tiket.assignee?.fullName ?? 'nije dodeljen'}</span>
              </div>
              <div>
                <span className="kljuc">Prijavljen</span>
                <span className="vrednost">{datumVreme(tiket.createdAt)}</span>
              </div>
              <div>
                <span className="kljuc">Poslednja izmena</span>
                <span className="vrednost">{relativno(tiket.updatedAt)}</span>
              </div>
              {tiket.closedAt && (
                <div>
                  <span className="kljuc">Zatvoren</span>
                  <span className="vrednost">{datumVreme(tiket.closedAt)}</span>
                </div>
              )}
            </div>

            {tiket.assignee && !jeOsoblje && (
              <button
                type="button"
                className="dugme sporedno"
                style={{ marginTop: 16, width: '100%' }}
                onClick={() => pisiAgentu(tiket.assignee)}
              >
                Piši agentu {tiket.assignee.fullName}
              </button>
            )}
          </Ploca>

          {jeOsoblje && (
            <Ploca naslov="Dodela">
              <div className="stubac">
                <div className="polje">
                  <label htmlFor="agent">Zaduženi agent</label>
                  <select
                    id="agent"
                    value={tiket.assignee?.id ?? ''}
                    disabled={zauzeto}
                    onChange={(d) =>
                      izvrsi(
                        () => tiketi.dodeli(id, d.target.value ? Number(d.target.value) : null),
                        'Tiket je dodeljen.',
                      )
                    }
                  >
                    <option value="">— nije dodeljen —</option>
                    {agenti.map((agent) => (
                      <option key={agent.id} value={agent.id}>
                        {agent.fullName} ({nazivUloge(agent.role)})
                      </option>
                    ))}
                  </select>
                </div>
                <button
                  type="button"
                  className="dugme sporedno"
                  disabled={zauzeto}
                  onClick={() =>
                    izvrsi(() => tiketi.dodeli(id, null), 'Tiket je dodeljen najmanje opterećenom agentu.')
                  }
                >
                  Dodeli automatski
                </button>
                <p className="pomoc">
                  Automatska dodela bira agenta sa najmanje otvorenih tiketa.
                </p>
              </div>
            </Ploca>
          )}

          <Ploca naslov={`Istorija (${tiket.history?.length ?? 0})`}>
            {tiket.history?.length === 0 ? (
              <p className="tiho sitno">Status još nije menjan.</p>
            ) : (
              <div className="istorija">
                {tiket.history.map((stavka) => (
                  <div key={stavka.id} className="istorija-stavka">
                    <span className="istorija-kada">{datumVreme(stavka.changedAt)}</span>
                    <div>
                      <div className="istorija-prelaz">
                        {stavka.oldStatus && (
                          <>
                            <span className="oznaka" data-status={stavka.oldStatus}>
                              {nazivStatusa(stavka.oldStatus)}
                            </span>
                            <span className="tiho">→</span>
                          </>
                        )}
                        <span className="oznaka" data-status={stavka.newStatus}>
                          {nazivStatusa(stavka.newStatus)}
                        </span>
                      </div>
                      <div className="sitno tiho" style={{ marginTop: 3 }}>
                        {stavka.changedBy?.fullName}
                        {stavka.note ? ` — ${stavka.note}` : ''}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Ploca>
        </div>
      </div>
    </>
  )
}
