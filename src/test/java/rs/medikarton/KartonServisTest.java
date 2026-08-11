package rs.medikarton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.medikarton.dogadjaj.EmailPosmatrac;
import rs.medikarton.dogadjaj.RevizijaPosmatrac;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.EntitetNijeNadjenException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.Pregled;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;
import rs.medikarton.model.Uloga;
import rs.medikarton.servis.Karton;
import rs.medikarton.servis.KartonServis;
import rs.medikarton.servis.ZakazivanjeServis;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Zdravstveni karton")
class KartonServisTest extends OsnovniTest {

    private static final LocalDateTime SADA = LocalDateTime.of(2026, 9, 7, 8, 0);
    private static final LocalDateTime VREME_PREGLEDA = LocalDateTime.of(2026, 9, 8, 9, 0);
    private static final LocalDate DAN = LocalDate.of(2026, 9, 8);

    private KartonServis karton;
    private ZakazivanjeServis zakazivanje;
    private RevizijaPosmatrac revizija;
    private EmailPosmatrac email;
    private Pacijent pacijent;
    private Pacijent alergicanPacijent;
    private Lekar lekar;
    private Lekar drugiLekar;
    private Korisnik lekarNalog;
    private Korisnik adminNalog;

    @BeforeEach
    void pripremiServise() {
        Subjekat dogadjaji = new Subjekat();
        revizija = new RevizijaPosmatrac();
        email = new EmailPosmatrac();
        dogadjaji.registruj(revizija);
        dogadjaji.registruj(email);

        karton = new KartonServis(fabrika.pregledi(), fabrika.recepti(), fabrika.nalazi(),
                fabrika.termini(), fabrika.pacijenti(), dogadjaji);
        zakazivanje = new ZakazivanjeServis(fabrika.termini(), fabrika.pacijenti(),
                fabrika.lekari(), dogadjaji);

        pacijent = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
        alergicanPacijent = dodajPacijenta(JMBG_MILICA, "Milica", "Stankovic");
        alergicanPacijent.setAlergije("penicilin, aspirin");
        fabrika.pacijenti().azuriraj(alergicanPacijent);

        lekar = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
        drugiLekar = dodajLekara("LK-2", "Marko", "Ilic", "opsta medicina");
        lekarNalog = nalog("ana.jovic", Uloga.LEKAR, null);
        lekarNalog.setLekarId(lekar.getId());
        adminNalog = nalog("admin", Uloga.ADMIN, null);
    }

    private Termin zakazan(Pacijent p) {
        return zakazivanje.zakazi(p.getId(), lekar.getId(), VREME_PREGLEDA, "Kontrola", SADA);
    }

    private Pregled evidentiran(Pacijent p) {
        Termin t = zakazan(p);
        return karton.evidentirajPregled(t.getId(),
                "Pacijent navodi glavobolju vec nedelju dana.", "I10", "Kontrola za mesec dana",
                VREME_PREGLEDA);
    }

    private Korisnik nalog(String ime, Uloga uloga, Integer pacijentId) {
        Korisnik korisnik = new Korisnik(ime, "test-hash", "test-so", uloga);
        korisnik.setPacijentId(pacijentId);
        return korisnik;
    }

    @Test
    @DisplayName("PP-05: evidentiranje pregleda prevodi termin u REALIZOVAN")
    void evidentiranjePregleda() {
        Termin t = zakazan(pacijent);
        Pregled p = karton.evidentirajPregled(t.getId(),
                "Pacijent navodi glavobolju vec nedelju dana.", "i10", "Mirovanje", VREME_PREGLEDA);

        assertAll(
                () -> assertNotNull(p.getId()),
                () -> assertEquals("I10", p.getDijagnozaMkb(), "MKB sifra se normalizuje"),
                () -> assertEquals(pacijent.getId(), p.getPacijentId()),
                () -> assertEquals(lekar.getId(), p.getLekarId()),
                () -> assertEquals(StatusTermina.REALIZOVAN,
                        fabrika.termini().nadjiPoId(t.getId()).orElseThrow().getStatus()),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.PREGLED_EVIDENTIRAN)));
    }

    @Test
    @DisplayName("PP-05: nad otkazanim ili vec realizovanim terminom pregled se ne evidentira")
    void pregledNadPogresnimStatusom() {
        Termin otkazan = zakazan(pacijent);
        zakazivanje.otkazi(otkazan.getId(), SADA);
        assertThrows(ValidacijaException.class, () -> karton.evidentirajPregled(
                otkazan.getId(), "Duga anamneza teksta.", "I10", "t", VREME_PREGLEDA));

        Termin realizovan = zakazan(alergicanPacijent);
        karton.evidentirajPregled(realizovan.getId(), "Duga anamneza teksta.", "I10", "t", VREME_PREGLEDA);
        assertThrows(ValidacijaException.class, () -> karton.evidentirajPregled(
                realizovan.getId(), "Duga anamneza teksta.", "I10", "t", VREME_PREGLEDA));
    }

    @Test
    void pregledOdbijaNeispravanUnos() {
        Termin t = zakazan(pacijent);
        assertAll(
                () -> assertThrows(ValidacijaException.class, () -> karton.evidentirajPregled(
                        t.getId(), "Duga anamneza teksta.", "nije-mkb", "t", VREME_PREGLEDA)),
                () -> assertThrows(ValidacijaException.class, () -> karton.evidentirajPregled(
                        t.getId(), "kratko", "I10", "t", VREME_PREGLEDA)),
                () -> assertThrows(EntitetNijeNadjenException.class, () -> karton.evidentirajPregled(
                        999, "Duga anamneza teksta.", "I10", "t", VREME_PREGLEDA)));
    }

    @Test
    void izdavanjeRecepta() {
        Pregled p = evidentiran(pacijent);
        var recept = karton.izdajRecept(p.getId(), lekar.getId(), "Ramipril 5mg",
                "c09aa05", "1x1 tableta ujutru", 2, DAN);

        assertAll(
                () -> assertNotNull(recept.getId()),
                () -> assertEquals("C09AA05", recept.getAtcSifra()),
                () -> assertEquals(DAN.plusDays(KartonServis.DANA_VAZENJA_RECEPTA), recept.getVaziDo()),
                () -> assertTrue(recept.vaziNaDan(DAN)),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.RECEPT_IZDAT)));
    }

    @Test
    @DisplayName("PP-06: recept moze da izda samo lekar koji je obavio pregled")
    void tudjLekarNeMozeDaIzdaRecept() {
        Pregled p = evidentiran(pacijent);
        ValidacijaException greska = assertThrows(ValidacijaException.class,
                () -> karton.izdajRecept(p.getId(), drugiLekar.getId(), "Ramipril",
                        "C09AA05", "1x1", 1, DAN));
        assertTrue(greska.getMessage().contains("obavio pregled"));
    }

    @Test
    @DisplayName("lek na koji je pacijent alergican se ne izdaje")
    void alergijaBlokiraRecept() {
        Pregled p = evidentiran(alergicanPacijent);
        ValidacijaException greska = assertThrows(ValidacijaException.class,
                () -> karton.izdajRecept(p.getId(), lekar.getId(), "Penicilin G",
                        "J01CE01", "2x1", 1, DAN));
        assertAll(
                () -> assertTrue(greska.getMessage().contains("alergican")),
                () -> assertEquals(0, fabrika.recepti().prebroj()));
    }

    @Test
    @DisplayName("lek koji nije na listi alergija prolazi")
    void nealergenLekProlazi() {
        Pregled p = evidentiran(alergicanPacijent);
        assertNotNull(karton.izdajRecept(p.getId(), lekar.getId(), "Ramipril",
                "C09AA05", "1x1", 1, DAN).getId());
    }

    @Test
    void receptOdbijaNeispravanUnos() {
        Pregled p = evidentiran(pacijent);
        assertAll(
                () -> assertThrows(ValidacijaException.class, () -> karton.izdajRecept(
                        p.getId(), lekar.getId(), "R", "C09AA05", "1x1", 1, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.izdajRecept(
                        p.getId(), lekar.getId(), "Ramipril", "XX", "1x1", 1, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.izdajRecept(
                        p.getId(), lekar.getId(), "Ramipril", "C09AA05", "1x1", 7, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.izdajRecept(
                        p.getId(), lekar.getId(), "Ramipril", "C09AA05", "1x1", 0, DAN)),
                () -> assertThrows(EntitetNijeNadjenException.class, () -> karton.izdajRecept(
                        999, lekar.getId(), "Ramipril", "C09AA05", "1x1", 1, DAN)));
    }

    @Test
    @DisplayName("nalaz van referentnog opsega pokrece obavestenje")
    void nalazVanOpsegaObavestava() {
        var visok = karton.dodajNalaz(pacijent.getId(), null, "Holesterol ukupni",
                7.4, "mmol/L", 3.0, 5.2, DAN);
        var normalan = karton.dodajNalaz(pacijent.getId(), null, "Glukoza",
                5.0, "mmol/L", 3.9, 6.1, DAN);

        assertAll(
                () -> assertFalse(visok.uReferentnomOpsegu()),
                () -> assertEquals("H", visok.oznaka()),
                () -> assertTrue(normalan.uReferentnomOpsegu()),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.NALAZ_VAN_OPSEGA),
                        "obavestenje se salje samo za nalaz van opsega"));
    }

    @Test
    void nalazOdbijaNeispravanUnos() {
        assertAll(
                () -> assertThrows(EntitetNijeNadjenException.class, () -> karton.dodajNalaz(
                        999, null, "Glukoza", 5.0, "mmol/L", 3.9, 6.1, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.dodajNalaz(
                        pacijent.getId(), null, "G", 5.0, "mmol/L", 3.9, 6.1, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.dodajNalaz(
                        pacijent.getId(), null, "Glukoza", 5.0, "mmol/L", 6.1, 3.9, DAN)),
                () -> assertThrows(ValidacijaException.class, () -> karton.dodajNalaz(
                        pacijent.getId(), null, "Glukoza", 1e9, "mmol/L", 3.9, 6.1, DAN)),
                () -> assertThrows(NullPointerException.class, () -> karton.dodajNalaz(
                        pacijent.getId(), null, "Glukoza", 5.0, "mmol/L", 3.9, 6.1, null)));
    }

    @Test
    @DisplayName("nalaz ne moze da se veze za pregled drugog pacijenta")
    void nalazMoraPripadatiIstomPacijentu() {
        Pregled tudjPregled = evidentiran(alergicanPacijent);

        assertThrows(ValidacijaException.class, () -> karton.dodajNalaz(
                pacijent.getId(), tudjPregled.getId(), "Glukoza", 5.0,
                "mmol/L", 3.9, 6.1, DAN));
    }

    @Test
    @DisplayName("karton objedinjuje preglede, recepte i nalaze")
    void dohvatanjeKartona() {
        Pregled p = evidentiran(pacijent);
        karton.izdajRecept(p.getId(), lekar.getId(), "Ramipril", "C09AA05", "1x1", 1, DAN);
        karton.dodajNalaz(pacijent.getId(), p.getId(), "Holesterol", 7.4, "mmol/L", 3.0, 5.2, DAN);
        karton.dodajNalaz(pacijent.getId(), p.getId(), "Glukoza", 5.0, "mmol/L", 3.9, 6.1, DAN);

        Karton k = karton.dohvatiKarton(lekarNalog, pacijent.getId(), SADA);
        assertAll(
                () -> assertEquals(1, k.brojPregleda()),
                () -> assertEquals(1, k.recepti().size()),
                () -> assertEquals(2, k.nalazi().size()),
                () -> assertEquals(1, k.nalaziVanOpsega().size()),
                () -> assertTrue(k.rezime().contains("Nikola Petrovic")),
                () -> assertTrue(k.rezime().contains("pregleda: 1")));
    }

    @Test
    void kartonAlergicnogPacijentaIsticeAlergije() {
        Karton k = karton.dohvatiKarton(adminNalog, alergicanPacijent.getId(), SADA);
        assertAll(
                () -> assertTrue(k.rezime().contains("ALERGIJE")),
                () -> assertEquals(0, k.brojPregleda()),
                () -> assertTrue(k.nalaziVanOpsega().isEmpty()));
    }

    @Test
    void kartonNepostojecegPacijentaJeGreska() {
        assertThrows(EntitetNijeNadjenException.class,
                () -> karton.dohvatiKarton(adminNalog, 999, SADA));
    }

    @Test
    @DisplayName("PP-01: kartonu pristupa lekar, administrator i sam pacijent")
    void kontrolaPristupaKartonu() {
        int id = pacijent.getId();
        Korisnik svojNalog = nalog("nikola", Uloga.PACIJENT, id);
        Korisnik tudjNalog = nalog("milica", Uloga.PACIJENT, id + 1);
        Korisnik recepcija = nalog("vesna", Uloga.RECEPCIONER, null);
        assertAll(
                () -> assertTrue(karton.smePristupiti(lekarNalog, id)),
                () -> assertTrue(karton.smePristupiti(adminNalog, id)),
                () -> assertTrue(karton.smePristupiti(svojNalog, id)),
                () -> assertFalse(karton.smePristupiti(tudjNalog, id),
                        "pacijent ne sme da vidi tudj karton"),
                () -> assertFalse(karton.smePristupiti(recepcija, id)),
                () -> assertFalse(karton.smePristupiti(null, id)),
                () -> assertThrows(ValidacijaException.class,
                        () -> karton.dohvatiKarton(recepcija, id, SADA)));

        karton.dohvatiKarton(svojNalog, id, SADA);
        assertEquals(1, revizija.brojPoTipu(TipDogadjaja.KARTON_OTVOREN));
    }

    @Test
    void servisOdbijaNullZavisnosti() {
        Subjekat d = new Subjekat();
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        null, fabrika.recepti(), fabrika.nalazi(), fabrika.termini(), fabrika.pacijenti(), d)),
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        fabrika.pregledi(), null, fabrika.nalazi(), fabrika.termini(), fabrika.pacijenti(), d)),
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        fabrika.pregledi(), fabrika.recepti(), null, fabrika.termini(), fabrika.pacijenti(), d)),
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        fabrika.pregledi(), fabrika.recepti(), fabrika.nalazi(), null, fabrika.pacijenti(), d)),
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        fabrika.pregledi(), fabrika.recepti(), fabrika.nalazi(), fabrika.termini(), null, d)),
                () -> assertThrows(NullPointerException.class, () -> new KartonServis(
                        fabrika.pregledi(), fabrika.recepti(), fabrika.nalazi(), fabrika.termini(),
                        fabrika.pacijenti(), null)));
    }

    @Test
    @DisplayName("e-posta stize pacijentu za termin, recept i nalaz van opsega")
    void obavestenjaStizuNaEposu() {
        Pregled p = evidentiran(pacijent);
        karton.izdajRecept(p.getId(), lekar.getId(), "Ramipril", "C09AA05", "1x1", 1, DAN);
        karton.dodajNalaz(pacijent.getId(), p.getId(), "Holesterol", 7.4, "mmol/L", 3.0, 5.2, DAN);

        assertEquals(3, email.brojPoslatih(), "zakazan termin + recept + nalaz van opsega");
    }
}
