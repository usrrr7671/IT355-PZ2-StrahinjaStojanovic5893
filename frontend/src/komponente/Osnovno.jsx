import { brojTiketa, nazivStatusa, potrosenSla, relativno } from '../util/format'

/** Oznaka statusa tiketa. Boja dolazi iz CSS-a preko data-status atributa. */
export function Status({ status }) {
  return (
    <span className="oznaka" data-status={status}>
      {nazivStatusa(status)}
    </span>
  )
}

/** Broj tiketa u obliku utisnute plocice. */
export function Plocica({ id, krupna = false }) {
  return <span className={krupna ? 'plocica krupna' : 'plocica'}>{brojTiketa(id)}</span>
}

/**
 * Merac SLA roka: koliko je vremena od prijave do roka potroseno.
 * Zatvoreni i reseni tiketi ga ne prikazuju - rok tada vise nista ne znaci.
 */
export function Sla({ createdAt, slaDeadline, slaBreached, status }) {
  // Za resen i zatvoren tiket rok vise nista ne meri, pa se prikazuje samo
  // ishod - prazna sina bi izgledala kao merac zaglavljen na nuli.
  if (status === 'CLOSED' || status === 'RESOLVED') {
    return (
      <div className="sla">
        <div className="sla-tekst">
          <span>Rok</span>
          <span>{slaBreached ? 'probijen' : 'ispunjen'}</span>
        </div>
      </div>
    )
  }

  const potroseno = potrosenSla(createdAt, slaDeadline)
  if (potroseno === null) return null

  const stanje = slaBreached ? 'probijen' : potroseno > 75 ? 'blizu' : ''

  return (
    <div className={`sla ${stanje}`.trim()}>
      <div className="sla-tekst">
        <span>{slaBreached ? 'Probijen' : 'Rok'}</span>
        <span>{relativno(slaDeadline)}</span>
      </div>
      <div className="sla-sina">
        <div className="sla-punjenje" style={{ width: `${Math.min(100, potroseno)}%` }} />
      </div>
    </div>
  )
}

/** Poruka o gresci ili uspehu, jedinstvenog oblika kroz celu aplikaciju. */
export function Poruka({ vrsta = 'greska', children }) {
  if (!children) return null
  return (
    <div className={`obavestenje ${vrsta}`} role={vrsta === 'greska' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}

/** Polje obrasca sa oznakom, pomocnim tekstom i porukom o gresci. */
export function Polje({ oznaka, greska, pomoc, children }) {
  return (
    <div className={`polje ${greska ? 'ima-gresku' : ''}`.trim()}>
      {oznaka && <label>{oznaka}</label>}
      {children}
      {greska && <span className="greska-polja">{greska}</span>}
      {!greska && pomoc && <span className="pomoc">{pomoc}</span>}
    </div>
  )
}

/** Prazan ekran je poziv na akciju, a ne obavestenje da nema podataka. */
export function Prazno({ naslov, opis, children }) {
  return (
    <div className="prazno">
      <h3>{naslov}</h3>
      {opis && <p className="sitno">{opis}</p>}
      {children && <div style={{ marginTop: 16 }}>{children}</div>}
    </div>
  )
}

export function Ucitavanje({ tekst = 'Učitavanje' }) {
  return (
    <div className="ucitavanje" role="status">
      {tekst}…
    </div>
  )
}

export function Ploca({ naslov, dodatak, children, bezOkvira = false }) {
  return (
    <section className="ploca">
      {(naslov || dodatak) && (
        <header className="ploca-glava">
          <h2>{naslov}</h2>
          {dodatak}
        </header>
      )}
      {bezOkvira ? children : <div className="ploca-telo">{children}</div>}
    </section>
  )
}
