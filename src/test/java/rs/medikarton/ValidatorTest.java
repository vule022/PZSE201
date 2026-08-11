package rs.medikarton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.util.Validator;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Validacija unosa")
class ValidatorTest {

    @Nested
    @DisplayName("Ime i prezime")
    class Imena {

        @ParameterizedTest(name = "ispravno ime: \"{0}\"")
        @ValueSource(strings = {"Ana", "Nikola", "Petrovic-Jovic", "D'Angelo", "Ana Marija", "Cedomir"})
        void prihvataIspravnaImena(String ime) {
            assertEquals(ime, Validator.validirajIme(ime, "ime"));
        }

        @ParameterizedTest(name = "neispravno ime: \"{0}\"")
        @ValueSource(strings = {
                "A",
                "-Ana",
                "Ana123",
                "Robert'); DROP TABLE pacijent;--",
                "Petrovic--Jovic",
                "D''Angelo"
        })
        void odbijaNeispravnaImena(String ime) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajIme(ime, "ime"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void odbijaPrazanUnos(String ime) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajIme(ime, "ime"));
        }

        @Test
        @DisplayName("granice duzine: 2 znaka prolazi, 40 prolazi, 41 pada")
        void granicneDuzine() {
            assertAll(
                    () -> assertEquals("An", Validator.validirajIme("An", "ime")),
                    () -> assertEquals("A".repeat(40), Validator.validirajIme("A".repeat(40), "ime")),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.validirajIme("A".repeat(41), "ime")));
        }

        @Test
        @DisplayName("uklanja praznine oko unosa")
        void skracujePraznine() {
            assertEquals("Ana", Validator.validirajIme("  Ana  ", "ime"));
        }
    }

    @Nested
    @DisplayName("JMBG")
    class Jmbg {

        @ParameterizedTest(name = "ispravan JMBG: {0}")
        @ValueSource(strings = {"1203985710122", "2507990725157", "0411978710344",
                "1809002725085", "0102968710270"})
        void prihvataIspravanJmbg(String jmbg) {
            assertEquals(jmbg, Validator.validirajJmbg(jmbg));
        }

        @Test
        @DisplayName("odbija pogresnu kontrolnu cifru")
        void odbijaPogresnuKontrolnuCifru() {
            ValidacijaException greska = assertThrows(ValidacijaException.class,
                    () -> Validator.validirajJmbg("1203985710123"));
            assertTrue(greska.getMessage().contains("Kontrolna cifra"));
        }

        @ParameterizedTest(name = "neispravan JMBG: \"{0}\"")
        @ValueSource(strings = {"12039857101", "12039857101222", "12039857101a2", "0000000000000"})
        void odbijaNeispravanFormat(String jmbg) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajJmbg(jmbg));
        }

        @Test
        @DisplayName("odbija nepostojeci datum rodjenja (31.02.)")
        void odbijaNepostojeciDatum() {
            ValidacijaException greska = assertThrows(ValidacijaException.class,
                    () -> Validator.validirajJmbg("3102985710128"));
            assertTrue(greska.getMessage().contains("datum"));
        }

        @Test
        @DisplayName("izvlaci datum rodjenja iz JMBG-a")
        void izvlaciDatum() {
            assertAll(
                    () -> assertEquals(LocalDate.of(1985, 3, 12), Validator.datumIzJmbg("1203985710122")),
                    () -> assertEquals(LocalDate.of(2002, 9, 18), Validator.datumIzJmbg("1809002725085")),
                    () -> assertNull(Validator.datumIzJmbg("nije-jmbg")));
        }

        @Test
        @DisplayName("kontrolna cifra: null i prazan unos nisu validni")
        void kontrolnaCifraNaGranicama() {
            assertAll(
                    () -> assertFalse(Validator.kontrolnaCifraJmbgValidna(null)),
                    () -> assertFalse(Validator.kontrolnaCifraJmbgValidna("")),
                    () -> assertFalse(Validator.kontrolnaCifraJmbgValidna("12345")),
                    () -> assertTrue(Validator.kontrolnaCifraJmbgValidna("1203985710122")));
        }
    }

    @Nested
    @DisplayName("Kontakt podaci")
    class Kontakt {

        @ParameterizedTest
        @ValueSource(strings = {"ana@example.rs", "ana.jovic+test@sub.example.co.uk", "A_B-1@x.io"})
        void prihvataIspravneAdrese(String email) {
            assertEquals(email.toLowerCase(), Validator.validirajEmail(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {"ana", "ana@", "@example.rs", "ana@example", "ana example@x.rs"})
        void odbijaNeispravneAdrese(String email) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajEmail(email));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0631234567", "+381631234567", "063 123 45 67", "064/123-4567"})
        void prihvataIMormalizujeTelefon(String telefon) {
            String rezultat = Validator.validirajTelefon(telefon);
            assertTrue(rezultat.matches("^(\\+381|0)6[0-9]{7,8}$"), "dobijeno: " + rezultat);
        }

        @ParameterizedTest
        @ValueSource(strings = {"0121234567", "12345", "+3811234567890123"})
        void odbijaNeispravanTelefon(String telefon) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajTelefon(telefon));
        }
    }

    @Nested
    @DisplayName("Medicinske sifre")
    class Sifre {

        @ParameterizedTest
        @ValueSource(strings = {"I10", "J06.9", "E11.9", "M54"})
        void prihvataMkbSifre(String sifra) {
            assertEquals(sifra, Validator.validirajMkb(sifra));
        }

        @Test
        void mkbSePrevodiUVelikaSlova() {
            assertEquals("I10", Validator.validirajMkb("i10"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"110", "I1", "I100", "I10.", "I10.999"})
        void odbijaNeispravneMkbSifre(String sifra) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajMkb(sifra));
        }

        @ParameterizedTest
        @ValueSource(strings = {"C09AA05", "N02BE01", "A10BA02"})
        void prihvataAtcSifre(String sifra) {
            assertEquals(sifra, Validator.validirajAtc(sifra));
        }

        @ParameterizedTest
        @ValueSource(strings = {"C09AA0", "C9AA05", "C09A05", "0904AA05"})
        void odbijaNeispravneAtcSifre(String sifra) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajAtc(sifra));
        }

        @ParameterizedTest
        @ValueSource(strings = {"A+", "A-", "B+", "AB+", "0-"})
        void prihvataKrvneGrupe(String grupa) {
            assertEquals(grupa, Validator.validirajKrvnuGrupu(grupa));
        }

        @ParameterizedTest
        @ValueSource(strings = {"C+", "A", "0", "AB", "A++"})
        void odbijaNeispravneKrvneGrupe(String grupa) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajKrvnuGrupu(grupa));
        }

        @Test
        void polPrihvataSamoMiZ() {
            assertAll(
                    () -> assertEquals("M", Validator.validirajPol("m")),
                    () -> assertEquals("Z", Validator.validirajPol("Z")),
                    () -> assertThrows(ValidacijaException.class, () -> Validator.validirajPol("X")));
        }
    }

    @Nested
    @DisplayName("Jacina lozinke")
    class Lozinke {

        @ParameterizedTest
        @ValueSource(strings = {"Klinika-2026!", "Aa1!aaaaaa", "Zdravlje#2026"})
        void prihvataJakeLozinke(String lozinka) {
            assertEquals(lozinka, Validator.validirajLozinku(lozinka));
        }

        @ParameterizedTest(name = "slaba lozinka: \"{0}\"")
        @ValueSource(strings = {
                "Aa1!aaaaa",
                "klinika2026!",
                "KLINIKA2026!",
                "KlinikaKlinika!",
                "Klinika20261",
                "Klinika2026Č"
        })
        void odbijaSlabeLozinke(String lozinka) {
            assertThrows(ValidacijaException.class, () -> Validator.validirajLozinku(lozinka));
        }

        @Test
        void odbijaNullLozinku() {
            assertThrows(ValidacijaException.class, () -> Validator.validirajLozinku(null));
        }
    }

    @Nested
    @DisplayName("Brojevi i opsezi")
    class Opsezi {

        @Test
        void prihvataVrednostiUOpsegu() {
            assertAll(
                    () -> assertEquals(5.0, Validator.zahtevajUOpsegu(5.0, "v", 0, 10)),
                    () -> assertEquals(0.0, Validator.zahtevajUOpsegu(0.0, "v", 0, 10)),
                    () -> assertEquals(10.0, Validator.zahtevajUOpsegu(10.0, "v", 0, 10)),
                    () -> assertEquals(3, Validator.zahtevajUOpsegu(3, "v", 1, 6)));
        }

        @Test
        void odbijaVrednostiVanOpsega() {
            assertAll(
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajUOpsegu(-0.1, "v", 0, 10)),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajUOpsegu(10.1, "v", 0, 10)),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajUOpsegu(7, "v", 1, 6)));
        }

        @Test
        @DisplayName("odbija NaN i beskonacnost")
        void odbijaNekonacneBrojeve() {
            assertAll(
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajUOpsegu(Double.NaN, "v", 0, 10)),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajUOpsegu(Double.POSITIVE_INFINITY, "v", 0, 10)));
        }

        @Test
        void zahtevajDuzinuProveraGranice() {
            assertAll(
                    () -> assertEquals("abc", Validator.zahtevajDuzinu("abc", "p", 3, 5)),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajDuzinu("ab", "p", 3, 5)),
                    () -> assertThrows(ValidacijaException.class,
                            () -> Validator.zahtevajDuzinu("abcdef", "p", 3, 5)));
        }
    }

    @Nested
    @DisplayName("Vreme termina")
    class VremeTermina {

        private final LocalDateTime sada = LocalDateTime.of(2026, 9, 7, 8, 0);

        @Test
        void prihvataRadniDanUToku() {
            LocalDateTime termin = LocalDateTime.of(2026, 9, 8, 10, 0);
            assertEquals(termin, Validator.validirajTerminPocetak(termin, sada));
        }

        @Test
        void odbijaTerminUProslosti() {
            assertThrows(ValidacijaException.class,
                    () -> Validator.validirajTerminPocetak(sada.minusHours(1), sada));
        }

        @Test
        void odbijaTerminDaljeOdGodinuDana() {
            assertThrows(ValidacijaException.class,
                    () -> Validator.validirajTerminPocetak(sada.plusYears(1).plusDays(1), sada));
        }

        @Test
        @DisplayName("radno vreme: 07:00 prolazi, 06:59 i 20:00 padaju")
        void proveraRadnogVremena() {
            assertAll(
                    () -> Validator.validirajTerminPocetak(LocalDateTime.of(2026, 9, 8, 7, 0), sada),
                    () -> assertThrows(ValidacijaException.class, () ->
                            Validator.validirajTerminPocetak(LocalDateTime.of(2026, 9, 8, 6, 59), sada)),
                    () -> assertThrows(ValidacijaException.class, () ->
                            Validator.validirajTerminPocetak(LocalDateTime.of(2026, 9, 8, 20, 0), sada)));
        }

        @Test
        void odbijaVikend() {
            assertAll(
                    () -> assertThrows(ValidacijaException.class, () ->
                            Validator.validirajTerminPocetak(LocalDateTime.of(2026, 9, 12, 10, 0), sada)),
                    () -> assertThrows(ValidacijaException.class, () ->
                            Validator.validirajTerminPocetak(LocalDateTime.of(2026, 9, 13, 10, 0), sada)));
        }

        @Test
        void odbijaNullVreme() {
            assertThrows(ValidacijaException.class, () -> Validator.validirajTerminPocetak(null, sada));
        }
    }
}
