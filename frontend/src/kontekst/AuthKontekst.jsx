import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { auth } from '../api/servisi'
import { procitajToken, zapamtiToken } from '../api/klijent'

const AuthKontekst = createContext(null)

/**
 * Drzi prijavljenog korisnika i token.
 *
 * Uloga se cuva samo radi prikaza - sta ce se zaista dozvoliti odlucuje
 * backend nad svakim zahtevom. Sakriveno dugme je udobnost za korisnika,
 * a ne zastita: i da se prikaze, server bi odgovorio sa 403.
 */
export function AuthProvider({ children }) {
  const [korisnik, postaviKorisnika] = useState(null)
  const [ucitavanje, postaviUcitavanje] = useState(true)

  // Pri osvezavanju strane token postoji, ali korisnik u memoriji ne - zato se
  // podaci o nalogu ponovo traze od servera, sto usput proverava i da li token
  // jos vazi.
  useEffect(() => {
    const token = procitajToken()
    if (!token) {
      postaviUcitavanje(false)
      return
    }
    auth
      .ja()
      .then(postaviKorisnika)
      .catch(() => {
        zapamtiToken(null)
        postaviKorisnika(null)
      })
      .finally(() => postaviUcitavanje(false))
  }, [])

  const prijava = useCallback(async (podaci) => {
    const odgovor = await auth.prijava(podaci)
    zapamtiToken(odgovor.token)
    postaviKorisnika(odgovor.user)
    return odgovor.user
  }, [])

  const registracija = useCallback(async (podaci) => {
    const odgovor = await auth.registracija(podaci)
    zapamtiToken(odgovor.token)
    postaviKorisnika(odgovor.user)
    return odgovor.user
  }, [])

  const odjava = useCallback(() => {
    zapamtiToken(null)
    postaviKorisnika(null)
  }, [])

  const vrednost = useMemo(
    () => ({
      korisnik,
      ucitavanje,
      prijava,
      registracija,
      odjava,
      jePrijavljen: Boolean(korisnik),
      jeOsoblje: korisnik?.role === 'AGENT' || korisnik?.role === 'ADMIN',
      jeAdmin: korisnik?.role === 'ADMIN',
    }),
    [korisnik, ucitavanje, prijava, registracija, odjava],
  )

  return <AuthKontekst.Provider value={vrednost}>{children}</AuthKontekst.Provider>
}

export function useAuth() {
  const kontekst = useContext(AuthKontekst)
  if (!kontekst) {
    throw new Error('useAuth se koristi samo unutar AuthProvider komponente')
  }
  return kontekst
}
