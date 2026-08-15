import { useCallback, useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { korisnici, prepiske } from '../api/servisi'
import { porukaGreske } from '../api/klijent'
import { useAuth } from '../kontekst/AuthKontekst'
import { Poruka, Prazno, Ucitavanje } from '../komponente/Osnovno'
import { brojTiketa, datum, nazivUloge, relativno, skrati, vreme } from '../util/format'

/**
 * Prepiska izmedju korisnika i podrske.
 *
 * Pristup razgovoru se ovde ne odredjuje ulogom nego ucescem: server pusta
 * samo sagovornike, pa ni administrator ne moze da otvori tudju prepisku.
 * Zato ovaj ekran nema nikakvu proveru uloge - lista koju vrati server vec
 * sadrzi iskljucivo razgovore prijavljenog naloga.
 */
export default function Poruke() {
  const { jeOsoblje } = useAuth()
  const [parametri, postaviParametre] = useSearchParams()
  const izabranId = parametri.get('razgovor')

  const [lista, postaviListu] = useState([])
  const [razgovor, postaviRazgovor] = useState(null)
  const [agenti, postaviAgente] = useState([])
  const [ucitavanje, postaviUcitavanje] = useState(true)
  const [greska, postaviGresku] = useState('')
  const [tekst, postaviTekst] = useState('')
  const [salje, postaviSalje] = useState(false)

  const dnoToka = useRef(null)

  const ucitajListu = useCallback(async () => {
    try {
      const razgovori = await prepiske.moje()
      postaviListu(razgovori)
      return razgovori
    } catch (e) {
      postaviGresku(porukaGreske(e))
      return []
    }
  }, [])

  useEffect(() => {
    ucitajListu().finally(() => postaviUcitavanje(false))
  }, [ucitajListu])

  // Korisnik pise agentima, agent pise korisnicima - spisak agenata treba
  // samo onome ko nije osoblje, da bi imao kome da se obrati.
  useEffect(() => {
    if (jeOsoblje) return
    korisnici.agenti().then(postaviAgente).catch(() => {})
  }, [jeOsoblje])

  // Otvaranje razgovora usput obelezava tudje poruke kao procitane, pa se
  // posle toga osvezava i lista da bi znacka pala na nulu.
  useEffect(() => {
    if (!izabranId) {
      postaviRazgovor(null)
      return
    }
    let otkazano = false
    prepiske
      .jedna(izabranId)
      .then((podaci) => {
        if (otkazano) return
        postaviRazgovor(podaci)
        postaviGresku('')
        ucitajListu()
      })
      .catch((e) => {
        if (!otkazano) postaviGresku(porukaGreske(e, 'Nemate pristup ovoj prepisci.'))
      })
    return () => {
      otkazano = true
    }
  }, [izabranId, ucitajListu])

  useEffect(() => {
    dnoToka.current?.scrollIntoView({ block: 'end' })
  }, [razgovor?.messages?.length])

  function otvori(id) {
    postaviParametre({ razgovor: String(id) })
  }

  async function posalji(dogadjaj) {
    dogadjaj.preventDefault()
    if (!tekst.trim() || !razgovor) return
    postaviSalje(true)
    try {
      const poruka = await prepiske.posalji(razgovor.id, tekst.trim())
      postaviRazgovor((prethodno) => ({
        ...prethodno,
        messages: [...(prethodno.messages ?? []), poruka],
      }))
      postaviTekst('')
      ucitajListu()
    } catch (e) {
      postaviGresku(porukaGreske(e))
    } finally {
      postaviSalje(false)
    }
  }

  async function zapocniSa(agentId) {
    try {
      const novi = await prepiske.zapocni({ recipientId: Number(agentId), content: null })
      await ucitajListu()
      otvori(novi.id)
    } catch (e) {
      postaviGresku(porukaGreske(e))
    }
  }

  if (ucitavanje) return <Ucitavanje tekst="Učitavanje prepiski" />

  return (
    <>
      <header className="glava-strane">
        <div>
          <span className="nadnaslov">Privatna prepiska</span>
          <h1 className="natpis">Poruke</h1>
        </div>
        {!jeOsoblje && agenti.length > 0 && (
          <div className="polje" style={{ minWidth: 240 }}>
            <label htmlFor="novi-razgovor">Pišite agentu podrške</label>
            <select
              id="novi-razgovor"
              value=""
              onChange={(d) => d.target.value && zapocniSa(d.target.value)}
            >
              <option value="">— izaberite agenta —</option>
              {agenti.map((agent) => (
                <option key={agent.id} value={agent.id}>
                  {agent.fullName} ({nazivUloge(agent.role)})
                </option>
              ))}
            </select>
          </div>
        )}
      </header>

      <Poruka vrsta="greska">{greska}</Poruka>

      <section className="ploca" style={{ marginTop: greska ? 16 : 0, overflow: 'hidden' }}>
        {lista.length === 0 ? (
          <Prazno
            naslov="Nemate nijednu prepisku"
            opis={
              jeOsoblje
                ? 'Kada vam se korisnik obrati porukom, razgovor će se pojaviti ovde.'
                : 'Izaberite agenta gore i pošaljite prvu poruku.'
            }
          />
        ) : (
          <div className="razgovor">
            <div className="razgovor-lista">
              {lista.map((stavka) => (
                <button
                  key={stavka.id}
                  type="button"
                  className={`stavka-liste ${
                    String(stavka.id) === izabranId ? 'izabrana' : ''
                  }`.trim()}
                  onClick={() => otvori(stavka.id)}
                >
                  <span style={{ minWidth: 0 }}>
                    <span style={{ display: 'block', fontWeight: 600, fontSize: 14 }}>
                      {stavka.counterpart?.fullName}
                    </span>
                    <span className="sitno tiho" style={{ display: 'block' }}>
                      {stavka.lastMessagePreview
                        ? skrati(stavka.lastMessagePreview, 38)
                        : 'nema poruka'}
                    </span>
                    {stavka.ticketId && (
                      <span className="plocica" style={{ marginTop: 4, display: 'inline-block' }}>
                        {brojTiketa(stavka.ticketId)}
                      </span>
                    )}
                  </span>
                  <span style={{ textAlign: 'right', flex: 'none' }}>
                    <span className="sitno tiho" style={{ display: 'block' }}>
                      {stavka.lastMessageAt ? relativno(stavka.lastMessageAt) : ''}
                    </span>
                    {stavka.unreadCount > 0 && (
                      <span className="znacka" style={{ marginTop: 4, display: 'inline-block' }}>
                        {stavka.unreadCount}
                      </span>
                    )}
                  </span>
                </button>
              ))}
            </div>

            <div className="razgovor-tok">
              {!razgovor ? (
                <Prazno
                  naslov="Izaberite prepisku"
                  opis="Razgovori sa leve strane otvaraju se jednim klikom."
                />
              ) : (
                <>
                  <div className="ploca-glava">
                    <div>
                      <h2>{razgovor.counterpart?.fullName}</h2>
                      <span className="sitno tiho">
                        {nazivUloge(razgovor.counterpart?.role)}
                        {razgovor.ticketTitle
                          ? ` · povodom ${brojTiketa(razgovor.ticketId)} ${razgovor.ticketTitle}`
                          : ''}
                      </span>
                    </div>
                    <span className="sitno tiho">od {datum(razgovor.createdAt)}</span>
                  </div>

                  <div className="tok-poruke">
                    {razgovor.messages?.length === 0 ? (
                      <p className="tiho sitno" style={{ textAlign: 'center', margin: 'auto' }}>
                        Napišite prvu poruku.
                      </p>
                    ) : (
                      razgovor.messages.map((poruka) => (
                        <div
                          key={poruka.id}
                          className={`oblacic ${poruka.mine ? 'moja' : ''}`.trim()}
                        >
                          <div className="zaglavlje">
                            <span className="ko">
                              {poruka.mine ? 'Vi' : poruka.sender?.fullName}
                            </span>
                            <span className="kada">{vreme(poruka.sentAt)}</span>
                          </div>
                          <div style={{ whiteSpace: 'pre-wrap' }}>{poruka.content}</div>
                        </div>
                      ))
                    )}
                    <div ref={dnoToka} />
                  </div>

                  <form className="tok-slanje" onSubmit={posalji}>
                    <input
                      type="text"
                      value={tekst}
                      onChange={(d) => postaviTekst(d.target.value)}
                      placeholder="Napišite poruku…"
                      maxLength={2000}
                      aria-label="Tekst poruke"
                    />
                    <button
                      type="submit"
                      className="dugme glavno"
                      disabled={salje || !tekst.trim()}
                    >
                      Pošalji
                    </button>
                  </form>
                </>
              )}
            </div>
          </div>
        )}
      </section>
    </>
  )
}
