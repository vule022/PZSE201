package rs.medikarton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.LabNalaz;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.Pregled;
import rs.medikarton.model.Recept;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;
import rs.medikarton.model.Uloga;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Domenski model")
class ModelTest {

    @Nested
    @DisplayName("Pacijent")
    class PacijentTest {

        private Pacijent nikola() {
            return new Pacijent("1203985710122", "Nikola", "Petrovic",
                    LocalDate.of(1985, 3, 12), "M", "n@example.rs", "0631234567");
        }

        @Test
        void racunaStarost() {
            assertAll(
                    () -> assertEquals(41, nikola().starost(LocalDate.of(2026, 8, 10))),
                    () -> assertEquals(40, nikola().starost(LocalDate.of(2026, 3, 11))),
                    () -> assertEquals(41, nikola().starost(LocalDate.of(2026, 3, 12))));
        }

        @Test
        @DisplayName("starost: datum pre rodjenja i nedostajuci datum su greske")
        void starostGranicniSlucajevi() {
            Pacijent p = nikola();
            assertThrows(IllegalArgumentException.class, () -> p.starost(LocalDate.of(1980, 1, 1)));
            assertThrows(NullPointerException.class, () -> p.starost(null));

            Pacijent bezDatuma = new Pacijent();
            assertThrows(IllegalStateException.class, () -> bezDatuma.starost(LocalDate.of(2026, 1, 1)));
        }

        @Test
        void prepoznajeAlergije() {
            Pacijent p = nikola();
            assertFalse(p.imaAlergije());
            p.setAlergije("   ");
            assertFalse(p.imaAlergije());
            p.setAlergije("penicilin");
            assertTrue(p.imaAlergije());
        }

        @Test
        @DisplayName("jednakost se odredjuje po JMBG-u, ne po identifikatoru iz baze")
        void jednakostPoJmbg() {
            Pacijent a = nikola();
            Pacijent b = nikola();
            b.setId(99);
            assertAll(
                    () -> assertEquals(a, b),
                    () -> assertEquals(a.hashCode(), b.hashCode()),
                    () -> assertEquals(a, a),
                    () -> assertNotEquals(a, "nije pacijent"),
                    () -> assertTrue(a.toString().contains("Nikola Petrovic")));
        }

        @Test
        void punoImeSpajaImeIPrezime() {
            assertEquals("Nikola Petrovic", nikola().punoIme());
        }
    }

    @Nested
    @DisplayName("Lekar")
    class LekarTest {

        @Test
        void prikazujeSpecijalizaciju() {
            Lekar l = new Lekar("LK-1", "Ana", "Jovic", "kardiologija", "a@x.rs");
            assertAll(
                    () -> assertEquals("dr Ana Jovic", l.punoIme()),
                    () -> assertEquals("dr Ana Jovic (kardiologija)", l.prikazSaSpecijalizacijom()),
                    () -> assertEquals(20, l.getTrajanjePregledaMin()),
                    () -> assertTrue(l.toString().contains("kardiologija")));
        }

        @Test
        void jednakostPoBrojuLicence() {
            Lekar a = new Lekar("LK-1", "Ana", "Jovic", "kardiologija", "a@x.rs");
            Lekar b = new Lekar("LK-1", "Ana", "Jovic-Peric", "interna", "b@x.rs");
            assertAll(
                    () -> assertEquals(a, b),
                    () -> assertEquals(a.hashCode(), b.hashCode()),
                    () -> assertNotEquals(a, new Lekar("LK-2", "Ana", "Jovic", "kardiologija", "a@x.rs")),
                    () -> assertNotEquals(a, null));
        }
    }

    @Nested
    @DisplayName("Korisnik i zastita naloga")
    class KorisnikTest {

        private final LocalDateTime sada = LocalDateTime.of(2026, 9, 7, 8, 0);

        private Korisnik nalog() {
            return new Korisnik("ana.jovic", "hash", "so", Uloga.LEKAR);
        }

        @Test
        @DisplayName("nov nalog nije zakljucan i ima svih 5 pokusaja")
        void novNalog() {
            Korisnik k = nalog();
            assertAll(
                    () -> assertFalse(k.jeZakljucan(sada)),
                    () -> assertEquals(5, k.preostaloPokusaja()),
                    () -> assertTrue(k.isAktivan()));
        }

        @Test
        @DisplayName("nalog se zakljucava tacno na petom promasaju")
        void zakljucavanjeNaPetomPokusaju() {
            Korisnik k = nalog();
            for (int i = 1; i <= 4; i++) {
                k.zabeleziNeuspeluPrijavu(sada);
                assertFalse(k.jeZakljucan(sada), "posle " + i + ". promasaja jos ne sme biti zakljucan");
            }
            k.zabeleziNeuspeluPrijavu(sada);
            assertAll(
                    () -> assertTrue(k.jeZakljucan(sada)),
                    () -> assertEquals(0, k.preostaloPokusaja()),
                    () -> assertEquals(sada.plusMinutes(15), k.getZakljucanDo()));
        }

        @Test
        @DisplayName("zakljucavanje istice posle 15 minuta")
        void zakljucavanjeIstice() {
            Korisnik k = nalog();
            for (int i = 0; i < 5; i++) {
                k.zabeleziNeuspeluPrijavu(sada);
            }
            assertAll(
                    () -> assertTrue(k.jeZakljucan(sada.plusMinutes(14))),
                    () -> assertFalse(k.jeZakljucan(sada.plusMinutes(15))),
                    () -> assertFalse(k.jeZakljucan(sada.plusMinutes(16))));
        }

        @Test
        void resetPonistavaBrojacIZakljucavanje() {
            Korisnik k = nalog();
            for (int i = 0; i < 5; i++) {
                k.zabeleziNeuspeluPrijavu(sada);
            }
            k.resetujBrojacPrijava();
            assertAll(
                    () -> assertFalse(k.jeZakljucan(sada)),
                    () -> assertEquals(0, k.getBrojNeuspelihPrijava()),
                    () -> assertEquals(5, k.preostaloPokusaja()));
        }

        @Test
        @DisplayName("toString ne sme da otkrije hash ni so")
        void toStringNeCuriPodatke() {
            String tekst = nalog().toString();
            assertAll(
                    () -> assertFalse(tekst.contains("hash")),
                    () -> assertFalse(tekst.contains("so")),
                    () -> assertTrue(tekst.contains("ana.jovic")));
        }

        @Test
        void ulogaIzTeksta() {
            assertAll(
                    () -> assertEquals(Uloga.ADMIN, Uloga.izTeksta("admin")),
                    () -> assertEquals(Uloga.LEKAR, Uloga.izTeksta(" LEKAR ")),
                    () -> assertThrows(IllegalArgumentException.class, () -> Uloga.izTeksta(null)),
                    () -> assertThrows(IllegalArgumentException.class, () -> Uloga.izTeksta("PORTIR")));
        }
    }

    @Nested
    @DisplayName("Termin")
    class TerminTest {

        private Termin termin(int sat, int minut, int trajanje) {
            return new Termin(1, 1, LocalDateTime.of(2026, 9, 8, sat, minut), trajanje, "kontrola");
        }

        @Test
        void racunaKraj() {
            assertEquals(LocalDateTime.of(2026, 9, 8, 9, 20), termin(9, 0, 20).kraj());
        }

        @Test
        void krajBezPocetkaJeGreska() {
            assertThrows(IllegalStateException.class, () -> new Termin().kraj());
        }

        @Test
        @DisplayName("preklapanje: dodirivanje krajeva nije preklapanje")
        void preklapanje() {
            Termin a = termin(9, 0, 20);
            assertAll(
                    () -> assertTrue(a.preklapaSe(termin(9, 10, 20))),
                    () -> assertTrue(a.preklapaSe(termin(8, 50, 20))),
                    () -> assertTrue(a.preklapaSe(termin(9, 0, 20))),
                    () -> assertTrue(a.preklapaSe(termin(8, 45, 60))),
                    () -> assertFalse(a.preklapaSe(termin(9, 20, 20))),
                    () -> assertFalse(a.preklapaSe(termin(8, 40, 20))));
        }

        @Test
        void preklapanjeSaNullBacaGresku() {
            assertThrows(NullPointerException.class, () -> termin(9, 0, 20).preklapaSe(null));
        }

        @Test
        void statusiDozvoljavajuIspravnePrelaze() {
            assertAll(
                    () -> assertTrue(StatusTermina.ZAKAZAN.moguceOtkazati()),
                    () -> assertTrue(StatusTermina.POTVRDJEN.moguceOtkazati()),
                    () -> assertFalse(StatusTermina.OTKAZAN.moguceOtkazati()),
                    () -> assertFalse(StatusTermina.REALIZOVAN.moguceOtkazati()),
                    () -> assertTrue(StatusTermina.POTVRDJEN.moguceRealizovati()),
                    () -> assertFalse(StatusTermina.REALIZOVAN.moguceRealizovati()),
                    () -> assertEquals(StatusTermina.ZAKAZAN, StatusTermina.izTeksta("zakazan")),
                    () -> assertThrows(IllegalArgumentException.class, () -> StatusTermina.izTeksta(null)));
        }

        @Test
        void jednakostPoIdentifikatoru() {
            Termin a = termin(9, 0, 20);
            Termin b = termin(10, 0, 20);
            assertNotEquals(a, b);
            a.setId(1);
            b.setId(1);
            assertAll(
                    () -> assertEquals(a, b),
                    () -> assertEquals(a.hashCode(), b.hashCode()),
                    () -> assertTrue(a.toString().contains("ZAKAZAN")));
        }
    }

    @Nested
    @DisplayName("Pregled, recept i laboratorijski nalaz")
    class ZapisiKartona {

        @Test
        void pregledIzvlaciKategorijuDijagnoze() {
            Pregled p = new Pregled(1, 1, 1, LocalDateTime.now(), "anamneza", "I10", "terapija");
            assertEquals("I", p.kategorijaDijagnoze());

            p.setDijagnozaMkb(null);
            assertEquals("?", p.kategorijaDijagnoze());

            p.setDijagnozaMkb("  ");
            assertEquals("?", p.kategorijaDijagnoze());
        }

        @Test
        void pregledJednakostPoIdentifikatoru() {
            Pregled a = new Pregled(1, 1, 1, LocalDateTime.now(), "a", "I10", "t");
            Pregled b = new Pregled(2, 2, 2, LocalDateTime.now(), "b", "J06", "t");
            a.setId(7);
            b.setId(7);
            assertAll(
                    () -> assertEquals(a, b),
                    () -> assertEquals(a.hashCode(), b.hashCode()),
                    () -> assertNotEquals(a, "tekst"),
                    () -> assertTrue(a.toString().contains("I10")));
        }

        @Test
        @DisplayName("recept vazi od dana izdavanja do poslednjeg dana, ukljucujuci granice")
        void vazenjeRecepta() {
            LocalDate izdat = LocalDate.of(2026, 9, 8);
            Recept r = new Recept(1, "Ramipril", "C09AA05", "1x1", 2, izdat, izdat.plusDays(30));
            assertAll(
                    () -> assertTrue(r.vaziNaDan(izdat)),
                    () -> assertTrue(r.vaziNaDan(izdat.plusDays(30))),
                    () -> assertFalse(r.vaziNaDan(izdat.minusDays(1))),
                    () -> assertFalse(r.vaziNaDan(izdat.plusDays(31))));
        }

        @Test
        void receptBezDatumaBacaGresku() {
            Recept r = new Recept();
            assertThrows(IllegalStateException.class, () -> r.vaziNaDan(LocalDate.now()));
            assertThrows(NullPointerException.class,
                    () -> new Recept(1, "L", "C09AA05", "1x1", 1,
                            LocalDate.now(), LocalDate.now()).vaziNaDan(null));
        }

        @Test
        @DisplayName("nalaz oznacava L / N / H prema referentnom opsegu")
        void oznakaNalaza() {
            assertAll(
                    () -> assertEquals("N", nalaz(4.5).oznaka()),
                    () -> assertEquals("N", nalaz(3.0).oznaka()),
                    () -> assertEquals("N", nalaz(5.2).oznaka()),
                    () -> assertEquals("L", nalaz(2.9).oznaka()),
                    () -> assertEquals("H", nalaz(7.4).oznaka()),
                    () -> assertTrue(nalaz(4.5).uReferentnomOpsegu()),
                    () -> assertFalse(nalaz(7.4).uReferentnomOpsegu()),
                    () -> assertTrue(nalaz(7.4).toString().contains("[H]")));
        }

        private LabNalaz nalaz(double vrednost) {
            return new LabNalaz(1, 1, "Holesterol", vrednost, "mmol/L", 3.0, 5.2,
                    LocalDate.of(2026, 9, 8));
        }
    }
}
