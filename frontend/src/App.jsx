import { Navigate, Route, Routes } from 'react-router-dom'
import Raspored from './komponente/Raspored'
import { SamoAdmin, SamoOsoblje, Zasticena } from './komponente/Zastita'
import Prijava from './strane/Prijava'
import Registracija from './strane/Registracija'
import Pregled from './strane/Pregled'
import Tiketi from './strane/Tiketi'
import MojiTiketi from './strane/MojiTiketi'
import RadniRed from './strane/RadniRed'
import NoviTiket from './strane/NoviTiket'
import DetaljiTiketa from './strane/DetaljiTiketa'
import Poruke from './strane/Poruke'
import AdminNalozi from './strane/AdminNalozi'
import AdminSifarnici from './strane/AdminSifarnici'

/**
 * Rute aplikacije. Prijava i registracija stoje van rasporeda sa navigacijom,
 * jer su jedine strane dostupne bez naloga; sve ostalo je unutar Zasticene
 * komponente, a admin strane i unutar dodatne provere uloge.
 */
export default function App() {
  return (
    <Routes>
      <Route path="/prijava" element={<Prijava />} />
      <Route path="/registracija" element={<Registracija />} />

      <Route
        element={
          <Zasticena>
            <Raspored />
          </Zasticena>
        }
      >
        <Route index element={<Pregled />} />
        <Route path="tiketi" element={<Tiketi />} />
        <Route path="tiketi/novi" element={<NoviTiket />} />
        <Route path="tiketi/:id" element={<DetaljiTiketa />} />
        <Route path="moji-tiketi" element={<MojiTiketi />} />
        <Route
          path="radni-red"
          element={
            <SamoOsoblje>
              <RadniRed />
            </SamoOsoblje>
          }
        />
        <Route path="poruke" element={<Poruke />} />
        <Route
          path="admin/nalozi"
          element={
            <SamoAdmin>
              <AdminNalozi />
            </SamoAdmin>
          }
        />
        <Route
          path="admin/sifarnici"
          element={
            <SamoAdmin>
              <AdminSifarnici />
            </SamoAdmin>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
