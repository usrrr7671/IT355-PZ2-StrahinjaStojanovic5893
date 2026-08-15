import { Link } from 'react-router-dom'
import { Plocica, Prazno, Sla, Status, Ucitavanje } from './Osnovno'
import { datum } from '../util/format'

/**
 * Red tiketa, a ne kartica. Podrska ceo dan gleda ovaj spisak, pa mu je vazno
 * da u jednom pogledu vidi sto vise redova; boja na levoj ivici je najbrzi
 * put do stanja tiketa, pre nego sto se procita i jedno slovo naslova.
 */
export default function ListaTiketa({
  stranica,
  ucitavanje,
  praznoNaslov = 'Ovde još nema tiketa',
  praznoOpis,
  praznoDodatak,
  naStranu,
}) {
  if (ucitavanje) return <Ucitavanje />

  if (!stranica || stranica.content.length === 0) {
    return (
      <Prazno naslov={praznoNaslov} opis={praznoOpis}>
        {praznoDodatak}
      </Prazno>
    )
  }

  return (
    <>
      <div>
        {stranica.content.map((tiket) => (
          <Link
            key={tiket.id}
            to={`/tiketi/${tiket.id}`}
            className="red-tiketa"
            data-status={tiket.status}
          >
            <div className="red-levo">
              <Plocica id={tiket.id} />
              <Status status={tiket.status} />
            </div>

            <div>
              <div className="ceo-naslov">{tiket.title}</div>
              <div className="podnaslov">
                <span>{tiket.categoryName}</span>
                <span>·</span>
                <span>{tiket.priorityName}</span>
                <span>·</span>
                <span>prijavio {tiket.reporter?.fullName ?? '—'}</span>
                <span>·</span>
                <span>{datum(tiket.createdAt)}</span>
              </div>
            </div>

            <div className="red-desno">
              <span className="sitno tiho" style={{ minWidth: 116, textAlign: 'right' }}>
                {tiket.assignee ? tiket.assignee.fullName : 'nije dodeljen'}
              </span>
              <Sla
                createdAt={tiket.createdAt}
                slaDeadline={tiket.slaDeadline}
                slaBreached={tiket.slaBreached}
                status={tiket.status}
              />
            </div>
          </Link>
        ))}
      </div>

      {stranica.totalPages > 1 && (
        <div className="stranicenje">
          <span>
            Strana {stranica.page + 1} od {stranica.totalPages} · ukupno{' '}
            {stranica.totalElements}
          </span>
          <div className="potez">
            <button
              type="button"
              className="dugme sporedno sitno-dugme"
              disabled={stranica.first}
              onClick={() => naStranu(stranica.page - 1)}
            >
              Prethodna
            </button>
            <button
              type="button"
              className="dugme sporedno sitno-dugme"
              disabled={stranica.last}
              onClick={() => naStranu(stranica.page + 1)}
            >
              Sledeća
            </button>
          </div>
        </div>
      )}
    </>
  )
}
