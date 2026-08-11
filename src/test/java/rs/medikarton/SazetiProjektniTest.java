package rs.medikarton;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

@DisplayName("MediKarton - 20 projektnih test scenarija")
class SazetiProjektniTest {

    @Test
    @DisplayName("01 - validacija imena i JMBG-a")
    void validacijaImenaIJmbga() {
        ValidatorTest test = new ValidatorTest();
        ValidatorTest.Imena imena = test.new Imena();
        for (String vrednost : new String[]{"Ana", "Nikola", "Petrovic-Jovic", "D'Angelo", "Ana Marija", "Cedomir"}) {
            imena.prihvataIspravnaImena(vrednost);
        }
        for (String vrednost : new String[]{"A", "-Ana", "Ana123", "Robert'); DROP TABLE pacijent;--", "Petrovic--Jovic", "D''Angelo"}) {
            imena.odbijaNeispravnaImena(vrednost);
        }
        for (String vrednost : new String[]{null, "", "   "}) {
            imena.odbijaPrazanUnos(vrednost);
        }
        imena.granicneDuzine();
        imena.skracujePraznine();

        ValidatorTest.Jmbg jmbg = test.new Jmbg();
        for (String vrednost : new String[]{"1203985710122", "2507990725157", "0411978710344", "1809002725085", "0102968710270"}) {
            jmbg.prihvataIspravanJmbg(vrednost);
        }
        for (String vrednost : new String[]{"12039857101", "12039857101222", "12039857101a2", "0000000000000"}) {
            jmbg.odbijaNeispravanFormat(vrednost);
        }
        jmbg.odbijaPogresnuKontrolnuCifru();
        jmbg.odbijaNepostojeciDatum();
        jmbg.izvlaciDatum();
        jmbg.kontrolnaCifraNaGranicama();
    }

    @Test
    @DisplayName("02 - validacija kontakta i medicinskih sifara")
    void validacijaKontaktaISifara() {
        ValidatorTest test = new ValidatorTest();
        ValidatorTest.Kontakt kontakt = test.new Kontakt();
        for (String vrednost : new String[]{"ana@example.rs", "ana.jovic+test@sub.example.co.uk", "A_B-1@x.io"}) {
            kontakt.prihvataIspravneAdrese(vrednost);
        }
        for (String vrednost : new String[]{"ana", "ana@", "@example.rs", "ana@example", "ana example@x.rs"}) {
            kontakt.odbijaNeispravneAdrese(vrednost);
        }
        for (String vrednost : new String[]{"0631234567", "+381631234567", "063 123 45 67", "064/123-4567"}) {
            kontakt.prihvataIMormalizujeTelefon(vrednost);
        }
        for (String vrednost : new String[]{"0121234567", "12345", "+3811234567890123"}) {
            kontakt.odbijaNeispravanTelefon(vrednost);
        }

        ValidatorTest.Sifre sifre = test.new Sifre();
        for (String vrednost : new String[]{"I10", "J06.9", "E11.9", "M54"}) sifre.prihvataMkbSifre(vrednost);
        for (String vrednost : new String[]{"110", "I1", "I100", "I10.", "I10.999"}) sifre.odbijaNeispravneMkbSifre(vrednost);
        for (String vrednost : new String[]{"C09AA05", "N02BE01", "A10BA02"}) sifre.prihvataAtcSifre(vrednost);
        for (String vrednost : new String[]{"C09AA0", "C9AA05", "C09A05", "0904AA05"}) sifre.odbijaNeispravneAtcSifre(vrednost);
        for (String vrednost : new String[]{"A+", "A-", "B+", "AB+", "0-"}) sifre.prihvataKrvneGrupe(vrednost);
        for (String vrednost : new String[]{"C+", "A", "0", "AB", "A++"}) sifre.odbijaNeispravneKrvneGrupe(vrednost);
        sifre.mkbSePrevodiUVelikaSlova();
        sifre.polPrihvataSamoMiZ();
    }

    @Test
    @DisplayName("03 - validacija lozinki, opsega i termina")
    void validacijaLozinkiOpsegaIVremena() {
        ValidatorTest test = new ValidatorTest();
        ValidatorTest.Lozinke lozinke = test.new Lozinke();
        for (String vrednost : new String[]{"Klinika-2026!", "Aa1!aaaaaa", "Zdravlje#2026"}) lozinke.prihvataJakeLozinke(vrednost);
        for (String vrednost : new String[]{"Aa1!aaaaa", "klinika2026!", "KLINIKA2026!", "KlinikaKlinika!", "Klinika20261", "Klinika2026Č"}) lozinke.odbijaSlabeLozinke(vrednost);
        lozinke.odbijaNullLozinku();

        ValidatorTest.Opsezi opsezi = test.new Opsezi();
        opsezi.prihvataVrednostiUOpsegu();
        opsezi.odbijaVrednostiVanOpsega();
        opsezi.odbijaNekonacneBrojeve();
        opsezi.zahtevajDuzinuProveraGranice();

        ValidatorTest.VremeTermina vreme = test.new VremeTermina();
        vreme.prihvataRadniDanUToku();
        vreme.odbijaTerminUProslosti();
        vreme.odbijaTerminDaljeOdGodinuDana();
        vreme.proveraRadnogVremena();
        vreme.odbijaVikend();
        vreme.odbijaNullVreme();
    }

    @Test
    @DisplayName("04 - domenski model pacijenta i lekara")
    void domenskiPacijentILekar() {
        ModelTest test = new ModelTest();
        ModelTest.PacijentTest pacijent = test.new PacijentTest();
        pacijent.racunaStarost();
        pacijent.starostGranicniSlucajevi();
        pacijent.prepoznajeAlergije();
        pacijent.jednakostPoJmbg();
        pacijent.punoImeSpajaImeIPrezime();
        ModelTest.LekarTest lekar = test.new LekarTest();
        lekar.prikazujeSpecijalizaciju();
        lekar.jednakostPoBrojuLicence();
    }

    @Test
    @DisplayName("05 - domenski model naloga i termina")
    void domenskiKorisnikITermin() {
        ModelTest test = new ModelTest();
        ModelTest.KorisnikTest korisnik = test.new KorisnikTest();
        korisnik.novNalog();
        korisnik.zakljucavanjeNaPetomPokusaju();
        korisnik.zakljucavanjeIstice();
        korisnik.resetPonistavaBrojacIZakljucavanje();
        korisnik.toStringNeCuriPodatke();
        korisnik.ulogaIzTeksta();
        ModelTest.TerminTest termin = test.new TerminTest();
        termin.racunaKraj();
        termin.krajBezPocetkaJeGreska();
        termin.preklapanje();
        termin.preklapanjeSaNullBacaGresku();
        termin.statusiDozvoljavajuIspravnePrelaze();
        termin.jednakostPoIdentifikatoru();
    }

    @Test
    @DisplayName("06 - domenski model pregleda, recepta i nalaza")
    void domenskiZapisiKartona() {
        ModelTest test = new ModelTest();
        ModelTest.ZapisiKartona zapisi = test.new ZapisiKartona();
        zapisi.pregledIzvlaciKategorijuDijagnoze();
        zapisi.pregledJednakostPoIdentifikatoru();
        zapisi.vazenjeRecepta();
        zapisi.receptBezDatumaBacaGresku();
        zapisi.oznakaNalaza();
    }

    @Test
    @DisplayName("07 - DAO pacijenata i lekara")
    void daoPacijentiILekari() throws Exception {
        dao(t -> t.new Pacijenti().punCrudCiklus());
        dao(t -> t.new Pacijenti().nalaziPoJmbgIVracaPraznoZaNepostojeci());
        dao(t -> t.new Pacijenti().pretragaPoPrezimenuVracaSamoPogodke());
        dao(t -> t.new Pacijenti().grupniUpis());
        dao(t -> t.new Pacijenti().grupniUpisSeVracaUnazad());
        dao(t -> t.new Pacijenti().azuriranjeBezIdentifikatoraJeGreska());
        dao(t -> t.new Pacijenti().dupliJmbgJeOdbijen());
        dao(t -> t.new Lekari().crudIPretragaPoSpecijalizaciji());
        dao(t -> t.new Lekari().azuriranjeBezIdentifikatoraJeGreska());
    }

    @Test
    @DisplayName("08 - DAO korisnika i termina")
    void daoKorisniciITermini() throws Exception {
        dao(t -> t.new Korisnici().crudIPretragaPoKorisnickomImenu());
        dao(t -> t.new Korisnici().nullableVeze());
        dao(t -> t.new Korisnici().azuriranjeBezIdentifikatoraJeGreska());
        dao(t -> t.new Termini().crudIFilterPoLekaruIDanu());
        dao(t -> t.new Termini().azuriranjeBezIdentifikatoraJeGreska());
        dao(t -> t.new Termini().straniKljucSeProverava());
    }

    @Test
    @DisplayName("09 - DAO zapisa zdravstvenog kartona")
    void daoZapisiKartona() throws Exception {
        dao(t -> t.new ZapisiKartona().pregledCrudIIstorijaPacijenta());
        dao(t -> t.new ZapisiKartona().izvestajPoDijagnozama());
        dao(t -> t.new ZapisiKartona().receptCrudIVazeciRecepti());
        dao(t -> t.new ZapisiKartona().kaskadnoBrisanje());
        dao(t -> t.new ZapisiKartona().labNalazCrudIFilterVanOpsega());
    }

    @Test
    @DisplayName("10 - baza, DAO fabrika i Singleton")
    void bazaFabrikaISingleton() throws Exception {
        dao(t -> t.new Infrastruktura().fabrikaKesiraDao());
        dao(t -> t.new Infrastruktura().fabrikaOdbijaNullVezu());
        dao(t -> t.new Infrastruktura().semaIPraznjenje());
        dao(t -> t.new Infrastruktura().demoPodaciSeUbacujuSamoJednom());
        dao(t -> t.new Infrastruktura().singletonVeza());
        dao(t -> t.new Infrastruktura().ponistavanjeSingletona());
    }

    @Test
    @DisplayName("11 - Observer obavestenja i revizioni trag")
    void dogadjajiIObavestenja() {
        DogadjajTest test = new DogadjajTest();
        test.dogadjajZahtevaTipIVreme();
        test.zapisZaDnevnikSadrziSveDelove();
        test.obavestavanjeSvih();
        test.dvostrukaRegistracija();
        test.odjavljenPosmatracViseNeDobijaDogadjaje();
        test.filterKanalaEposte();
        test.kvarPosmatracaNeRusiTok();
        test.izmenaListeTokomObavestavanja();
        test.revizijaBrojiPoTipuIVracaNepromenljivTrag();
        test.subjekatOdbijaNullVrednosti();
        test.podrazumevaniFilter();
    }

    @Test
    @DisplayName("12 - SQL injection napad i odbrana")
    void sqlInjectionNapadIOdbrana() throws Exception {
        sql(t -> t.tautologija());
        sql(t -> t.unionNapad());
        sql(t -> t.zaobilazenjePrijaveKomentarom());
        sql(t -> t.bezbedniUpitRadiZaObicanUnos());
        sql(t -> t.apostrofUPodacimaNijeProblem());
        sql(t -> t.punIzvestaj());
    }

    @Test
    @DisplayName("13 - zakazivanje, potvrda i slobodni termini")
    void zakazivanjeOsnovniTok() throws Exception {
        zakazivanje(t -> t.uspesnoZakazivanje());
        zakazivanje(t -> t.dodirivanjeKrajevaJeDozvoljeno());
        zakazivanje(t -> t.otkazivanjeOslobadjaVreme());
        zakazivanje(t -> t.potvrdaPrelaziUPotvrdjen());
        zakazivanje(t -> t.slobodniTermini());
        zakazivanje(t -> t.prosliSatiSeNeNude());
    }

    @Test
    @DisplayName("14 - poslovna pravila i greske zakazivanja")
    void zakazivanjePravilaIGreske() throws Exception {
        zakazivanje(t -> t.odbijaPreklapanje());
        zakazivanje(t -> t.odbijaDvaTerminaIstogDana());
        zakazivanje(t -> t.kasnoOtkazivanje());
        zakazivanje(t -> t.dvostrukoOtkazivanje());
        zakazivanje(t -> t.nepostojeciEntiteti());
        zakazivanje(t -> t.prazanRazlog());
        zakazivanje(t -> t.terminPrekoRadnogVremena());
        zakazivanje(t -> t.vikendNemaTermina());
        zakazivanje(t -> t.servisOdbijaNullZavisnosti());
    }

    @Test
    @DisplayName("15 - cuvanje lozinke, registracija i prijava")
    void autentifikacijaRegistracijaIPrijava() throws Exception {
        autentifikacija(t -> t.lozinkaSeNeCuvaOtvoreno());
        autentifikacija(t -> t.razliciteSoliDajuRazliciteHasheve());
        autentifikacija(t -> t.proveraLozinkeRadiIOdbijaPogresnu());
        autentifikacija(t -> t.hashOdbijaPrazneArgumente());
        autentifikacija(t -> t.registracijaOdbijaLosePodatke());
        autentifikacija(t -> t.prijavaSaTacnimPodacimaUspeva());
        autentifikacija(t -> t.neuspelePrijave());
        autentifikacija(t -> t.deaktiviranNalogNeMozeDaSePrijavi());
    }

    @Test
    @DisplayName("16 - zakljucavanje naloga i Brute Force zastita")
    void autentifikacijaZakljucavanjeIBruteForce() throws Exception {
        autentifikacija(t -> t.zakljucavanjePosle5Promasaja());
        autentifikacija(t -> t.zakljucanNalogOdbijaITacnuLozinku());
        autentifikacija(t -> t.kaznaIstice());
        autentifikacija(t -> t.uspesnaPrijavaResetujeBrojac());
        autentifikacija(t -> t.napadRecnikomJeZaustavljen());
        autentifikacija(t -> t.slabaLozinkaBiBilaPogodjena());
        autentifikacija(t -> t.kratakRecnikSeIscrpi());
        autentifikacija(t -> t.promenaLozinke());
        autentifikacija(t -> t.promenaLozinkeOdbijaIstuISlabuLozinku());
        autentifikacija(t -> t.administrativnoOtkljucavanje());
        autentifikacija(t -> t.servisOdbijaNullZavisnosti());
    }

    @Test
    @DisplayName("17 - evidentiranje pregleda")
    void zdravstveniKartonPregledi() throws Exception {
        karton(t -> t.evidentiranjePregleda());
        karton(t -> t.pregledNadPogresnimStatusom());
        karton(t -> t.pregledOdbijaNeispravanUnos());
    }

    @Test
    @DisplayName("18 - izdavanje recepta i provera alergija")
    void zdravstveniKartonRecepti() throws Exception {
        karton(t -> t.izdavanjeRecepta());
        karton(t -> t.tudjLekarNeMozeDaIzdaRecept());
        karton(t -> t.alergijaBlokiraRecept());
        karton(t -> t.nealergenLekProlazi());
        karton(t -> t.receptOdbijaNeispravanUnos());
    }

    @Test
    @DisplayName("19 - laboratorijski nalazi i kontrola pristupa kartonu")
    void zdravstveniKartonNalaziIPristup() throws Exception {
        karton(t -> t.nalazVanOpsegaObavestava());
        karton(t -> t.nalazOdbijaNeispravanUnos());
        karton(t -> t.nalazMoraPripadatiIstomPacijentu());
        karton(t -> t.dohvatanjeKartona());
        karton(t -> t.kartonAlergicnogPacijentaIsticeAlergije());
        karton(t -> t.kartonNepostojecegPacijentaJeGreska());
        karton(t -> t.kontrolaPristupaKartonu());
    }

    @Test
    @DisplayName("20 - integracija servisa sa obavestenjima")
    void integracijaServisaIObavestenja() throws Exception {
        karton(t -> t.obavestenjaStizuNaEposu());
        karton(t -> t.servisOdbijaNullZavisnosti());
    }

    private void dao(Consumer<DaoTest> scenario) throws Exception {
        DaoTest test = new DaoTest();
        saBazom(test, scenario, null);
    }

    private void sql(Consumer<SqlInjectionTest> scenario) throws Exception {
        SqlInjectionTest test = new SqlInjectionTest();
        saBazom(test, scenario, SqlInjectionTest::pripremiPodatke);
    }

    private void zakazivanje(Consumer<ZakazivanjeServisTest> scenario) throws Exception {
        ZakazivanjeServisTest test = new ZakazivanjeServisTest();
        saBazom(test, scenario, ZakazivanjeServisTest::pripremiServis);
    }

    private void autentifikacija(Consumer<AutentifikacijaServisTest> scenario) throws Exception {
        AutentifikacijaServisTest test = new AutentifikacijaServisTest();
        saBazom(test, scenario, AutentifikacijaServisTest::pripremiServis);
    }

    private void karton(Consumer<KartonServisTest> scenario) throws Exception {
        KartonServisTest test = new KartonServisTest();
        saBazom(test, scenario, KartonServisTest::pripremiServise);
    }

    private <T extends OsnovniTest> void saBazom(
            T test, Consumer<T> scenario, Consumer<T> dodatnaPriprema) throws Exception {
        test.pripremiBazu();
        try {
            if (dodatnaPriprema != null) dodatnaPriprema.accept(test);
            scenario.accept(test);
        } finally {
            test.zatvoriBazu();
        }
    }
}
