package rs.medikarton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rs.medikarton.dao.DaoFabrika;
import rs.medikarton.db.BazaVeza;
import rs.medikarton.db.DemoPodaci;
import rs.medikarton.db.Migracije;
import rs.medikarton.izuzeci.PodaciException;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sloj podataka (DAO)")
class DaoTest extends OsnovniTest {

    @Nested
    @DisplayName("PacijentDao")
    class Pacijenti {

        @Test
        @DisplayName("pun CRUD ciklus: upis, citanje, izmena, brisanje")
        void punCrudCiklus() {
            Pacijent sacuvan = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            assertNotNull(sacuvan.getId());

            Pacijent procitan = fabrika.pacijenti().nadjiPoId(sacuvan.getId()).orElseThrow();
            assertAll(
                    () -> assertEquals("Nikola", procitan.getIme()),
                    () -> assertEquals(JMBG_NIKOLA, procitan.getJmbg()),
                    () -> assertEquals(LocalDate.of(1985, 3, 12), procitan.getDatumRodjenja()),
                    () -> assertEquals("A+", procitan.getKrvnaGrupa()));

            procitan.setAlergije("penicilin");
            procitan.setAdresa("Vojvode Misica 5");
            assertTrue(fabrika.pacijenti().azuriraj(procitan));
            assertEquals("penicilin",
                    fabrika.pacijenti().nadjiPoId(sacuvan.getId()).orElseThrow().getAlergije());

            assertTrue(fabrika.pacijenti().obrisi(sacuvan.getId()));
            assertTrue(fabrika.pacijenti().nadjiPoId(sacuvan.getId()).isEmpty());
            assertFalse(fabrika.pacijenti().obrisi(sacuvan.getId()));
        }

        @Test
        void nalaziPoJmbgIVracaPraznoZaNepostojeci() {
            dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            assertAll(
                    () -> assertTrue(fabrika.pacijenti().nadjiPoJmbg(JMBG_NIKOLA).isPresent()),
                    () -> assertTrue(fabrika.pacijenti().nadjiPoJmbg(JMBG_MILICA).isEmpty()),
                    () -> assertTrue(fabrika.pacijenti().nadjiPoId(999).isEmpty()));
        }

        @Test
        void pretragaPoPrezimenuVracaSamoPogodke() {
            dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            dodajPacijenta(JMBG_MILICA, "Milica", "Petrovic");
            dodajPacijenta(JMBG_DRAGAN, "Dragan", "Nikolic");

            assertAll(
                    () -> assertEquals(2, fabrika.pacijenti().pretraziPoPrezimenu("Petrovic").size()),
                    () -> assertEquals(2, fabrika.pacijenti().pretraziPoPrezimenu("Petr").size()),
                    () -> assertEquals(1, fabrika.pacijenti().pretraziPoPrezimenu("Nikolic").size()),
                    () -> assertEquals(0, fabrika.pacijenti().pretraziPoPrezimenu("Xyz").size()),
                    () -> assertEquals(3, fabrika.pacijenti().svi().size()),
                    () -> assertEquals(3, fabrika.pacijenti().prebroj()));
        }

        @Test
        @DisplayName("grupni upis prolazi kroz jednu transakciju")
        void grupniUpis() {
            List<Pacijent> grupa = List.of(
                    new Pacijent(JMBG_NIKOLA, "Nikola", "Petrovic", LocalDate.of(1985, 3, 12), "M", null, null),
                    new Pacijent(JMBG_MILICA, "Milica", "Stankovic", LocalDate.of(1990, 7, 25), "Z", null, null),
                    new Pacijent(JMBG_DRAGAN, "Dragan", "Nikolic", LocalDate.of(1978, 11, 4), "M", null, null));

            assertEquals(3, fabrika.pacijenti().sacuvajSve(grupa));
            assertEquals(3, fabrika.pacijenti().prebroj());
        }

        @Test
        @DisplayName("grupni upis sa duplim JMBG-om ne ostavlja polovicno stanje")
        void grupniUpisSeVracaUnazad() {
            dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            List<Pacijent> grupa = List.of(
                    new Pacijent(JMBG_MILICA, "Milica", "Stankovic", LocalDate.of(1990, 7, 25), "Z", null, null),
                    new Pacijent(JMBG_NIKOLA, "Duplikat", "Duplikat", LocalDate.of(1985, 3, 12), "M", null, null));

            assertThrows(PodaciException.class, () -> fabrika.pacijenti().sacuvajSve(grupa));
            assertEquals(1, fabrika.pacijenti().prebroj(), "transakcija je ponistena u celosti");
        }

        @Test
        void azuriranjeBezIdentifikatoraJeGreska() {
            Pacijent bezId = new Pacijent(JMBG_NIKOLA, "Nikola", "Petrovic",
                    LocalDate.of(1985, 3, 12), "M", null, null);
            assertThrows(IllegalArgumentException.class, () -> fabrika.pacijenti().azuriraj(bezId));
        }

        @Test
        @DisplayName("dupli JMBG krsi jedinstveno ogranicenje baze")
        void dupliJmbgJeOdbijen() {
            dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            assertThrows(PodaciException.class, () -> dodajPacijenta(JMBG_NIKOLA, "Drugi", "Covek"));
        }
    }

    @Nested
    @DisplayName("LekarDao")
    class Lekari {

        @Test
        void crudIPretragaPoSpecijalizaciji() {
            Lekar ana = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            dodajLekara("LK-2", "Marko", "Ilic", "opsta medicina");
            dodajLekara("LK-3", "Jelena", "Popovic", "kardiologija");

            assertAll(
                    () -> assertEquals(3, fabrika.lekari().prebroj()),
                    () -> assertEquals(2, fabrika.lekari().poSpecijalizaciji("kardiologija").size()),
                    () -> assertEquals(0, fabrika.lekari().poSpecijalizaciji("nema takve").size()),
                    () -> assertEquals(3, fabrika.lekari().svi().size()));

            ana.setSpecijalizacija("interna medicina");
            ana.setTrajanjePregledaMin(30);
            assertTrue(fabrika.lekari().azuriraj(ana));

            Lekar procitan = fabrika.lekari().nadjiPoId(ana.getId()).orElseThrow();
            assertAll(
                    () -> assertEquals("interna medicina", procitan.getSpecijalizacija()),
                    () -> assertEquals(30, procitan.getTrajanjePregledaMin()));

            assertTrue(fabrika.lekari().obrisi(ana.getId()));
            assertTrue(fabrika.lekari().nadjiPoId(ana.getId()).isEmpty());
        }

        @Test
        void azuriranjeBezIdentifikatoraJeGreska() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabrika.lekari().azuriraj(new Lekar("LK-9", "X", "Y", "z", "e@x.rs")));
        }
    }

    @Nested
    @DisplayName("KorisnikDao")
    class Korisnici {

        @Test
        void crudIPretragaPoKorisnickomImenu() {
            Korisnik k = new Korisnik("ana.jovic", "hash", "so", Uloga.LEKAR);
            k.setLekarId(null);
            fabrika.korisnici().sacuvaj(k);

            assertAll(
                    () -> assertTrue(fabrika.korisnici().nadjiPoKorisnickomImenu("ana.jovic").isPresent()),
                    () -> assertTrue(fabrika.korisnici().nadjiPoKorisnickomImenu("nepostojeci").isEmpty()),
                    () -> assertTrue(fabrika.korisnici().nadjiPoId(k.getId()).isPresent()),
                    () -> assertEquals(1, fabrika.korisnici().svi().size()));

            k.setBrojNeuspelihPrijava(3);
            k.setZakljucanDo(LocalDateTime.of(2026, 9, 7, 8, 15));
            k.setAktivan(false);
            assertTrue(fabrika.korisnici().azuriraj(k));

            Korisnik procitan = fabrika.korisnici().nadjiPoKorisnickomImenu("ana.jovic").orElseThrow();
            assertAll(
                    () -> assertEquals(3, procitan.getBrojNeuspelihPrijava()),
                    () -> assertEquals(LocalDateTime.of(2026, 9, 7, 8, 15), procitan.getZakljucanDo()),
                    () -> assertFalse(procitan.isAktivan()),
                    () -> assertEquals(Uloga.LEKAR, procitan.getUloga()));

            assertTrue(fabrika.korisnici().obrisi(k.getId()));
        }

        @Test
        @DisplayName("veza sa pacijentom i lekarom se cuva i cita kao NULL kada je nema")
        void nullableVeze() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Korisnik saPacijentom = new Korisnik("nikola.p", "h", "s", Uloga.PACIJENT);
            saPacijentom.setPacijentId(p.getId());
            fabrika.korisnici().sacuvaj(saPacijentom);

            Korisnik bezVeze = new Korisnik("admin", "h", "s", Uloga.ADMIN);
            fabrika.korisnici().sacuvaj(bezVeze);

            assertAll(
                    () -> assertEquals(p.getId(),
                            fabrika.korisnici().nadjiPoKorisnickomImenu("nikola.p").orElseThrow().getPacijentId()),
                    () -> org.junit.jupiter.api.Assertions.assertNull(
                            fabrika.korisnici().nadjiPoKorisnickomImenu("admin").orElseThrow().getPacijentId()),
                    () -> org.junit.jupiter.api.Assertions.assertNull(
                            fabrika.korisnici().nadjiPoKorisnickomImenu("admin").orElseThrow().getLekarId()));
        }

        @Test
        void azuriranjeBezIdentifikatoraJeGreska() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabrika.korisnici().azuriraj(new Korisnik("x", "h", "s", Uloga.ADMIN)));
        }
    }

    @Nested
    @DisplayName("TerminDao")
    class Termini {

        @Test
        void crudIFilterPoLekaruIDanu() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            LocalDate dan = LocalDate.of(2026, 9, 8);

            Termin t1 = fabrika.termini().sacuvaj(
                    new Termin(p.getId(), l.getId(), dan.atTime(9, 0), 20, "kontrola"));
            fabrika.termini().sacuvaj(
                    new Termin(p.getId(), l.getId(), dan.atTime(11, 0), 20, "kontrola 2"));
            fabrika.termini().sacuvaj(
                    new Termin(p.getId(), l.getId(), dan.plusDays(1).atTime(9, 0), 20, "sutra"));

            assertAll(
                    () -> assertEquals(2, fabrika.termini().aktivniZaLekaraNaDan(l.getId(), dan).size()),
                    () -> assertEquals(1, fabrika.termini().aktivniZaLekaraNaDan(l.getId(), dan.plusDays(1)).size()),
                    () -> assertEquals(0, fabrika.termini().aktivniZaLekaraNaDan(999, dan).size()),
                    () -> assertEquals(3, fabrika.termini().zaPacijenta(p.getId()).size()),
                    () -> assertEquals(3, fabrika.termini().svi().size()));

            t1.setStatus(StatusTermina.OTKAZAN);
            assertTrue(fabrika.termini().azuriraj(t1));
            assertEquals(1, fabrika.termini().aktivniZaLekaraNaDan(l.getId(), dan).size(),
                    "otkazan termin vise ne zauzima vreme");

            assertEquals(2, fabrika.termini().predstojeciZaPacijenta(p.getId(), dan.atStartOfDay()).size());
            assertTrue(fabrika.termini().obrisi(t1.getId()));
        }

        @Test
        void azuriranjeBezIdentifikatoraJeGreska() {
            assertThrows(IllegalArgumentException.class,
                    () -> fabrika.termini().azuriraj(new Termin(1, 1, LocalDateTime.now(), 20, "x")));
        }

        @Test
        @DisplayName("strani kljuc sprecava termin nad nepostojecim pacijentom")
        void straniKljucSeProverava() {
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            assertThrows(PodaciException.class, () -> fabrika.termini().sacuvaj(
                    new Termin(999, l.getId(), LocalDateTime.of(2026, 9, 8, 9, 0), 20, "x")));
        }
    }

    @Nested
    @DisplayName("PregledDao, ReceptDao i LabNalazDao")
    class ZapisiKartona {

        @Test
        void pregledCrudIIstorijaPacijenta() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");

            Pregled stariji = fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                    LocalDateTime.of(2026, 3, 1, 9, 0), "prva poseta", "J06.9", "mirovanje"));
            Pregled noviji = fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                    LocalDateTime.of(2026, 9, 8, 9, 0), "kontrola", "I10", "terapija"));

            List<Pregled> istorija = fabrika.pregledi().zaPacijenta(p.getId());
            assertAll(
                    () -> assertEquals(2, istorija.size()),
                    () -> assertEquals(noviji.getId(), istorija.get(0).getId(), "najnoviji je prvi"),
                    () -> assertEquals(stariji.getId(), istorija.get(1).getId()),
                    () -> assertEquals(2, fabrika.pregledi().svi().size()),
                    () -> assertTrue(fabrika.pregledi().nadjiPoId(noviji.getId()).isPresent()));

            noviji.setTerapija("izmenjena terapija");
            assertTrue(fabrika.pregledi().azuriraj(noviji));
            assertEquals("izmenjena terapija",
                    fabrika.pregledi().nadjiPoId(noviji.getId()).orElseThrow().getTerapija());

            assertTrue(fabrika.pregledi().obrisi(stariji.getId()));
            assertThrows(IllegalArgumentException.class,
                    () -> fabrika.pregledi().azuriraj(new Pregled()));
        }

        @Test
        @DisplayName("izvestaj po dijagnozama grupise u bazi i postuje prag")
        void izvestajPoDijagnozama() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            for (int i = 0; i < 3; i++) {
                fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                        LocalDateTime.of(2026, 9, 8, 9 + i, 0), "a", "I10", "t"));
            }
            fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                    LocalDateTime.of(2026, 9, 9, 9, 0), "a", "J06.9", "t"));

            Map<String, Integer> svi = fabrika.pregledi().brojPregledaPoDijagnozi(1);
            Map<String, Integer> cesti = fabrika.pregledi().brojPregledaPoDijagnozi(2);
            assertAll(
                    () -> assertEquals(2, svi.size()),
                    () -> assertEquals(3, svi.get("I10")),
                    () -> assertEquals(1, cesti.size()),
                    () -> assertTrue(cesti.containsKey("I10")));
        }

        @Test
        void receptCrudIVazeciRecepti() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            Pregled pregled = fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                    LocalDateTime.of(2026, 9, 8, 9, 0), "a", "I10", "t"));

            LocalDate izdat = LocalDate.of(2026, 9, 8);
            Recept aktivan = fabrika.recepti().sacuvaj(new Recept(pregled.getId(), "Ramipril",
                    "C09AA05", "1x1", 2, izdat, izdat.plusDays(30)));
            fabrika.recepti().sacuvaj(new Recept(pregled.getId(), "Brufen",
                    "M01AE01", "3x1", 1, izdat.minusDays(90), izdat.minusDays(60)));

            assertAll(
                    () -> assertEquals(2, fabrika.recepti().zaPregled(pregled.getId()).size()),
                    () -> assertEquals(1, fabrika.recepti().vazeciZaPacijenta(p.getId(), izdat).size()),
                    () -> assertEquals(0, fabrika.recepti().vazeciZaPacijenta(p.getId(),
                            izdat.plusDays(60)).size()),
                    () -> assertEquals(2, fabrika.recepti().svi().size()),
                    () -> assertTrue(fabrika.recepti().nadjiPoId(aktivan.getId()).isPresent()));

            aktivan.setBrojPakovanja(3);
            assertTrue(fabrika.recepti().azuriraj(aktivan));
            assertEquals(3, fabrika.recepti().nadjiPoId(aktivan.getId()).orElseThrow().getBrojPakovanja());

            assertTrue(fabrika.recepti().obrisi(aktivan.getId()));
            assertThrows(IllegalArgumentException.class, () -> fabrika.recepti().azuriraj(new Recept()));
        }

        @Test
        @DisplayName("brisanje pregleda kaskadno brise njegove recepte")
        void kaskadnoBrisanje() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Lekar l = dodajLekara("LK-1", "Ana", "Jovic", "kardiologija");
            Pregled pregled = fabrika.pregledi().sacuvaj(new Pregled(null, p.getId(), l.getId(),
                    LocalDateTime.of(2026, 9, 8, 9, 0), "a", "I10", "t"));
            fabrika.recepti().sacuvaj(new Recept(pregled.getId(), "Ramipril", "C09AA05", "1x1", 1,
                    LocalDate.of(2026, 9, 8), LocalDate.of(2026, 10, 8)));

            assertEquals(1, fabrika.recepti().prebroj());
            fabrika.pregledi().obrisi(pregled.getId());
            assertEquals(0, fabrika.recepti().prebroj());
        }

        @Test
        void labNalazCrudIFilterVanOpsega() {
            Pacijent p = dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            LocalDate dan = LocalDate.of(2026, 9, 8);

            LabNalaz normalan = fabrika.nalazi().sacuvaj(new LabNalaz(p.getId(), null,
                    "Glukoza", 5.0, "mmol/L", 3.9, 6.1, dan));
            fabrika.nalazi().sacuvaj(new LabNalaz(p.getId(), null,
                    "Holesterol", 7.4, "mmol/L", 3.0, 5.2, dan));
            fabrika.nalazi().sacuvaj(new LabNalaz(p.getId(), null,
                    "Hemoglobin", 110, "g/L", 130, 175, dan));

            assertAll(
                    () -> assertEquals(3, fabrika.nalazi().zaPacijenta(p.getId()).size()),
                    () -> assertEquals(2, fabrika.nalazi().vanOpsegaZaPacijenta(p.getId()).size()),
                    () -> assertEquals(3, fabrika.nalazi().svi().size()),
                    () -> assertEquals(3, fabrika.nalazi().prebroj()));

            normalan.setVrednost(9.9);
            assertTrue(fabrika.nalazi().azuriraj(normalan));
            assertEquals(3, fabrika.nalazi().vanOpsegaZaPacijenta(p.getId()).size());

            assertTrue(fabrika.nalazi().obrisi(normalan.getId()));
            assertTrue(fabrika.nalazi().nadjiPoId(normalan.getId()).isEmpty());
            assertThrows(IllegalArgumentException.class, () -> fabrika.nalazi().azuriraj(new LabNalaz()));
        }
    }

    @Nested
    @DisplayName("Fabrika, sema i Singleton veza")
    class Infrastruktura {

        @Test
        @DisplayName("fabrika vraca istu instancu DAO objekta pri svakom pozivu")
        void fabrikaKesiraDao() {
            assertAll(
                    () -> assertSame(fabrika.pacijenti(), fabrika.pacijenti()),
                    () -> assertSame(fabrika.lekari(), fabrika.lekari()),
                    () -> assertSame(fabrika.korisnici(), fabrika.korisnici()),
                    () -> assertSame(fabrika.termini(), fabrika.termini()),
                    () -> assertSame(fabrika.pregledi(), fabrika.pregledi()),
                    () -> assertSame(fabrika.recepti(), fabrika.recepti()),
                    () -> assertSame(fabrika.nalazi(), fabrika.nalazi()),
                    () -> assertNotNull(fabrika.veza()));
        }

        @Test
        void fabrikaOdbijaNullVezu() {
            assertThrows(NullPointerException.class, () -> new DaoFabrika(null));
        }

        @Test
        @DisplayName("kreiranje seme je idempotentno, a praznjenje vraca cisto stanje")
        void semaIPraznjenje() {
            dodajPacijenta(JMBG_NIKOLA, "Nikola", "Petrovic");
            Migracije.kreirajSemu(veza);
            assertEquals(1, fabrika.pacijenti().prebroj());

            Migracije.isprazni(veza);
            assertAll(
                    () -> assertEquals(0, fabrika.pacijenti().prebroj()),
                    () -> assertEquals(0, fabrika.lekari().prebroj()),
                    () -> assertEquals(7, Migracije.brojTabela()));
        }

        @Test
        void demoPodaciSeUbacujuSamoJednom() {
            DemoPodaci.ubaci(fabrika);
            int posleUbacivanja = fabrika.pacijenti().prebroj();
            DemoPodaci.ubaci(fabrika);

            assertAll(
                    () -> assertEquals(5, posleUbacivanja),
                    () -> assertEquals(5, fabrika.pacijenti().prebroj()),
                    () -> assertEquals(3, fabrika.lekari().prebroj()));
        }

        @Test
        @DisplayName("Singleton vraca istu instancu i odbija promenu URL-a")
        void singletonVeza() {
            try {
                BazaVeza prva = BazaVeza.instanca(BazaVeza.MEMORIJSKI_URL);
                BazaVeza druga = BazaVeza.instanca(BazaVeza.MEMORIJSKI_URL);
                assertAll(
                        () -> assertSame(prva, druga),
                        () -> assertEquals(BazaVeza.MEMORIJSKI_URL, prva.getJdbcUrl()),
                        () -> assertNotNull(prva.veza()),
                        () -> assertThrows(IllegalStateException.class,
                                () -> BazaVeza.instanca("jdbc:sqlite:druga.db")));
            } finally {
                BazaVeza.ponistiInstancu();
            }
        }

        @Test
        @DisplayName("ponistavanje Singletona zatvara vezu i dozvoljava nov URL")
        void ponistavanjeSingletona() {
            BazaVeza.instanca(BazaVeza.MEMORIJSKI_URL).veza();
            BazaVeza.ponistiInstancu();
            BazaVeza.ponistiInstancu();

            DaoFabrika memorijska = DaoFabrika.memorijska();
            assertEquals(0, memorijska.pacijenti().prebroj());
            BazaVeza.ponistiInstancu();
        }
    }
}
