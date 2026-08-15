import { nazivStatusa, REDOSLED_STATUSA } from '../util/format'

/**
 * Traka zivotnog ciklusa tiketa - sredisnji element aplikacije.
 *
 * Skup dozvoljenih prelaza ne racuna se ovde nego stize iz odgovora servera
 * (polje allowedTransitions), pa je ono sto korisnik vidi uvek isto ono sto
 * ce server i dozvoliti. Frontend to samo crta:
 *
 *   - popunjena sina  -> stanje u kome je tiket sada
 *   - isprekidana     -> stanje u koje sme da se predje, dugme radi
 *   - ugasena         -> prelaz nije dozvoljen ili nemate pravo na njega
 *
 * Korisniku bez uloge agenta traka se prikazuje bez ijednog aktivnog koraka.
 * To je namerno: pravilo da status menja iskljucivo osoblje podrske vidi se
 * na ekranu, umesto da se sazna tek kada server odbije zahtev.
 */
export default function ZivotniCiklus({
  trenutni,
  dozvoljeni = [],
  smePromeniti = false,
  naPromenu,
  zauzeto = false,
}) {
  const skup = new Set(dozvoljeni)
  const indeksTrenutnog = REDOSLED_STATUSA.indexOf(trenutni)

  return (
    <div>
      <div className="ciklus">
        {REDOSLED_STATUSA.map((status, indeks) => {
          const jeTrenutni = status === trenutni
          const jeMoguc = smePromeniti && skup.has(status)
          const jeProslo = !jeTrenutni && !jeMoguc && indeks < indeksTrenutnog

          const klase = ['ciklus-korak']
          if (jeTrenutni) klase.push('tekuce')
          else if (jeMoguc) klase.push('moguce')
          else if (jeProslo) klase.push('proslo')
          else klase.push('zatvoreno')

          const naziv = nazivStatusa(status)
          const objasnjenje = jeTrenutni
            ? `Trenutno stanje: ${naziv}`
            : jeMoguc
              ? `Prebaci u stanje ${naziv}`
              : `Prelaz u stanje ${naziv} nije dozvoljen`

          return (
            <button
              key={status}
              type="button"
              data-status={status}
              className={klase.join(' ')}
              disabled={!jeMoguc || zauzeto}
              onClick={jeMoguc ? () => naPromenu(status) : undefined}
              title={objasnjenje}
              aria-label={objasnjenje}
              aria-current={jeTrenutni ? 'step' : undefined}
            >
              <span className="ciklus-sina" />
              <span className="ciklus-oznaka">{naziv}</span>
            </button>
          )
        })}
      </div>

      <div className="ciklus-legenda">
        <span>
          <i style={{ background: 'var(--mastilo)' }} />
          sada
        </span>
        <span>
          <i
            style={{
              background:
                'repeating-linear-gradient(90deg, var(--mastilo-tiho) 0 5px, transparent 5px 9px)',
            }}
          />
          moguć prelaz
        </span>
        <span>
          <i style={{ background: 'var(--papir-dubok)' }} />
          nedostupno
        </span>
      </div>
    </div>
  )
}
