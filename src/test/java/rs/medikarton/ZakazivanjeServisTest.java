package rs.medikarton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import rs.medikarton.dogadjaj.EmailPosmatrac;
import rs.medikarton.dogadjaj.RevizijaPosmatrac;
import rs.medikarton.dogadjaj.Subjekat;
import rs.medikarton.dogadjaj.TipDogadjaja;
import rs.medikarton.izuzeci.EntitetNijeNadjenException;
import rs.medikarton.izuzeci.TerminZauzetException;
import rs.medikarton.izuzeci.ValidacijaException;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;
import rs.medikarton.model.StatusTermina;
import rs.medikarton.model.Termin;
import rs.medikarton.servis.ZakazivanjeServis;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Zakazivanje termina")
class ZakazivanjeServisTest extends OsnovniTest {

    private static final LocalDateTime SADA = LocalDateTime.of(2026, 9, 7, 8, 0);

    private static final LocalDate SUTRA = LocalDate.of(2026, 9, 8);

    private ZakazivanjeServis servis;
    private Subjekat dogadjaji;
    private RevizijaPosmatrac revizija;
    private EmailPosmatrac email;
    private Pacijent pacijent;
    private Pacijent drugiPacijent;
    private Lekar lekar;

    @BeforeEach
    void pripremiServis() {
        dogadjaji = new Subjekat();
        revizija = new RevizijaPosmatrac();
        email = new EmailPosmatrac();
        dogadjaji.registruj(revizija);
        dogadjaji.registruj(email);

        servis = new ZakazivanjeServis(fabrika.termini(), fabrika.pacijenti(),
                fabrika.lekari(), dogadjaji);

        pacijent = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
        drugiPacijent = dodajPacijenta(JMBG_MILICA, "Milica", "Stankovic");
        lekar = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
    }

    @Test
    @DisplayName("uspesno zakazivanje upisuje termin i salje obavestenje")
    void uspesnoZakazivanje() {
        Termin t = servis.zakazi(pacijent.getId(), lekar.getId(),
                SUTRA.atTime(9, 0), "Kontrola pritiska", SADA);

        assertAll(
                () -> assertNotNull(t.getId()),
                () -> assertEquals(StatusTermina.ZAKAZAN, t.getStatus()),
                () -> assertEquals(20, t.getTrajanjeMin()),
                () -> assertEquals(1, fabrika.termini().prebroj()),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.TERMIN_ZAKAZAN)),
                () -> assertEquals(1, email.brojPoslatih()));
    }

    @Test
    @DisplayName("PP-02: preklapanje kod istog lekara je odbijeno")
    void odbijaPreklapanje() {
        servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);

        assertAll(
                () -> assertThrows(TerminZauzetException.class, () -> servis.zakazi(
                        drugiPacijent.getId(), lekar.getId(), SUTRA.atTime(9, 10), "Preklapa", SADA)),
                () -> assertThrows(TerminZauzetException.class, () -> servis.zakazi(
                        drugiPacijent.getId(), lekar.getId(), SUTRA.atTime(8, 50), "Preklapa", SADA)),
                () -> assertEquals(1, fabrika.termini().prebroj()));
    }

    @Test
    @DisplayName("termin odmah posle prethodnog nije preklapanje")
    void dodirivanjeKrajevaJeDozvoljeno() {
        servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);
        Termin drugi = servis.zakazi(drugiPacijent.getId(), lekar.getId(),
                SUTRA.atTime(9, 20), "Odmah posle", SADA);

        assertAll(
                () -> assertNotNull(drugi.getId()),
                () -> assertEquals(2, fabrika.termini().prebroj()));
    }

    @Test
    @DisplayName("PP-04: isti pacijent ne moze dva termina kod istog lekara istog dana")
    void odbijaDvaTerminaIstogDana() {
        servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);

        ValidacijaException greska = assertThrows(ValidacijaException.class, () -> servis.zakazi(
                pacijent.getId(), lekar.getId(), SUTRA.atTime(14, 0), "Drugi isti dan", SADA));
        assertTrue(greska.getMessage().contains("vec ima aktivan termin"));
    }

    @Test
    @DisplayName("otkazan termin oslobadja vreme")
    void otkazivanjeOslobadjaVreme() {
        Termin prvi = servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);
        assertTrue(servis.otkazi(prvi.getId(), SADA));

        Termin drugi = servis.zakazi(drugiPacijent.getId(), lekar.getId(),
                SUTRA.atTime(9, 0), "Na oslobodjeno mesto", SADA);
        assertAll(
                () -> assertNotNull(drugi.getId()),
                () -> assertEquals(StatusTermina.OTKAZAN,
                        fabrika.termini().nadjiPoId(prvi.getId()).orElseThrow().getStatus()),
                () -> assertEquals(1, revizija.brojPoTipu(TipDogadjaja.TERMIN_OTKAZAN)));
    }

    @Test
    @DisplayName("PP-03: otkazivanje manje od 2 sata pre pocetka nije dozvoljeno")
    void kasnoOtkazivanje() {
        Termin t = servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);

        LocalDateTime tacnoDvaSataPre = SUTRA.atTime(7, 0);
        LocalDateTime sat = SUTRA.atTime(8, 0);
        assertAll(
                () -> assertThrows(ValidacijaException.class, () -> servis.otkazi(t.getId(), sat)),
                () -> assertTrue(servis.otkazi(t.getId(), tacnoDvaSataPre)));
    }

    @Test
    @DisplayName("vec otkazan termin ne moze da se otkaze ponovo")
    void dvostrukoOtkazivanje() {
        Termin t = servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);
        assertAll(
                () -> assertTrue(servis.otkazi(t.getId(), SADA)),
                () -> assertFalse(servis.otkazi(t.getId(), SADA)));
    }

    @Test
    void potvrdaPrelaziUPotvrdjen() {
        Termin t = servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Prvi", SADA);
        assertAll(
                () -> assertTrue(servis.potvrdi(t.getId())),
                () -> assertEquals(StatusTermina.POTVRDJEN,
                        fabrika.termini().nadjiPoId(t.getId()).orElseThrow().getStatus()),
                () -> assertFalse(servis.potvrdi(t.getId()), "dvostruka potvrda nema efekta"));
    }

    @Test
    @DisplayName("nepostojeci pacijent, lekar i termin daju jasnu gresku")
    void nepostojeciEntiteti() {
        assertAll(
                () -> assertThrows(EntitetNijeNadjenException.class, () -> servis.zakazi(
                        999, lekar.getId(), SUTRA.atTime(9, 0), "x", SADA)),
                () -> assertThrows(EntitetNijeNadjenException.class, () -> servis.zakazi(
                        pacijent.getId(), 999, SUTRA.atTime(9, 0), "x", SADA)),
                () -> assertThrows(EntitetNijeNadjenException.class, () -> servis.otkazi(999, SADA)),
                () -> assertThrows(EntitetNijeNadjenException.class, () -> servis.potvrdi(999)),
                () -> assertThrows(EntitetNijeNadjenException.class,
                        () -> servis.slobodniTermini(999, SUTRA, SADA)));
    }

    @Test
    @DisplayName("prazan razlog dolaska je odbijen")
    void prazanRazlog() {
        assertThrows(ValidacijaException.class, () -> servis.zakazi(
                pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "", SADA));
    }

    @Test
    @DisplayName("termin koji bi se zavrsio posle 20h je odbijen")
    void terminPrekoRadnogVremena() {
        Lekar dugiPregled = dodajLekara("LK-2", "Jelena", "Popovic", "pedijatrija");
        dugiPregled.setTrajanjePregledaMin(45);
        fabrika.lekari().azuriraj(dugiPregled);

        assertThrows(ValidacijaException.class, () -> servis.zakazi(
                pacijent.getId(), dugiPregled.getId(), SUTRA.atTime(19, 30), "Kasno", SADA));
    }

    @Test
    @DisplayName("slobodni termini: 39 mesta u danu, minus zauzeti")
    void slobodniTermini() {
        List<LocalDateTime> praznDan = servis.slobodniTermini(lekar.getId(), SUTRA, SADA);
        assertEquals(39, praznDan.size(), "od 07:00 do 20:00 stane 39 pregleda po 20 minuta");
        assertEquals(SUTRA.atTime(7, 0), praznDan.get(0));

        servis.zakazi(pacijent.getId(), lekar.getId(), SUTRA.atTime(9, 0), "Zauzeto", SADA);
        List<LocalDateTime> posle = servis.slobodniTermini(lekar.getId(), SUTRA, SADA);
        assertAll(
                () -> assertEquals(38, posle.size()),
                () -> assertFalse(posle.contains(SUTRA.atTime(9, 0))));
    }

    @Test
    @DisplayName("vikendom nema slobodnih termina")
    void vikendNemaTermina() {
        LocalDate subota = LocalDate.of(2026, 9, 12);
        assertTrue(servis.slobodniTermini(lekar.getId(), subota, SADA).isEmpty());
    }

    @Test
    @DisplayName("vec prosli sati danasnjeg dana se ne nude")
    void prosliSatiSeNeNude() {
        LocalDateTime podne = SUTRA.atTime(12, 0);
        List<LocalDateTime> slobodni = servis.slobodniTermini(lekar.getId(), SUTRA, podne);
        assertAll(
                () -> assertTrue(slobodni.stream().allMatch(t -> t.isAfter(podne))),
                () -> assertFalse(slobodni.isEmpty()));
    }

    @Test
    void servisOdbijaNullZavisnosti() {
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new ZakazivanjeServis(
                        null, fabrika.pacijenti(), fabrika.lekari(), dogadjaji)),
                () -> assertThrows(NullPointerException.class, () -> new ZakazivanjeServis(
                        fabrika.termini(), null, fabrika.lekari(), dogadjaji)),
                () -> assertThrows(NullPointerException.class, () -> new ZakazivanjeServis(
                        fabrika.termini(), fabrika.pacijenti(), null, dogadjaji)),
                () -> assertThrows(NullPointerException.class, () -> new ZakazivanjeServis(
                        fabrika.termini(), fabrika.pacijenti(), fabrika.lekari(), null)));
    }
}
