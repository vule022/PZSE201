package rs.medikarton.bezbednost;

import rs.medikarton.dao.KorisnikDao;
import rs.medikarton.dao.PacijentDao;
import rs.medikarton.model.Pacijent;

import java.sql.Connection;
import java.util.List;

public class SqlInjectionDemo {

    public static final String NAPAD_TAUTOLOGIJA = "' OR 1=1 --";
    public static final String SUFIKS_KOMENTAR = "' --";
    public static final String NAPAD_UNION = "' UNION SELECT korisnicko_ime, uloga FROM korisnik --";

    private final RanjiviPacijentDao ranjivi;
    private final PacijentDao bezbedni;
    private final KorisnikDao bezbedniKorisnici;

    public SqlInjectionDemo(Connection veza) {
        this.ranjivi = new RanjiviPacijentDao(veza);
        this.bezbedni = new PacijentDao(veza);
        this.bezbedniKorisnici = new KorisnikDao(veza);
    }

    public record Ishod(String naziv, String unos, int redovaRanjivo, int redovaBezbedno, int ukupnoUBazi) {

        public boolean napadUspeo() {
            return redovaRanjivo > redovaBezbedno;
        }

        public String izvestaj() {
            return String.format("%-24s ranjivo=%2d  bezbedno=%2d  (u bazi %d)  -> %s%n"
                            + "%-24s unos: %s",
                    naziv, redovaRanjivo, redovaBezbedno, ukupnoUBazi,
                    napadUspeo() ? "NAPAD USPEO" : "napad odbijen",
                    "", unos);
        }
    }

    public Ishod tautologija() {
        List<Pacijent> krozRanjivu = ranjivi.pretraziPoPrezimenu(NAPAD_TAUTOLOGIJA);
        List<Pacijent> krozBezbednu = bezbedni.pretraziPoPrezimenu(NAPAD_TAUTOLOGIJA);
        return new Ishod("Tautologija (OR 1=1)", NAPAD_TAUTOLOGIJA,
                krozRanjivu.size(), krozBezbednu.size(), bezbedni.prebroj());
    }

    public Ishod union() {
        List<String> krozRanjivu = ranjivi.pretragaSaDveKolone(NAPAD_UNION);
        List<Pacijent> krozBezbednu = bezbedni.pretraziPoPrezimenu(NAPAD_UNION);
        return new Ishod("Spajanje (UNION SELECT)", NAPAD_UNION,
                krozRanjivu.size(), krozBezbednu.size(), bezbedni.prebroj());
    }

    public List<String> procitaniNaloziUnionNapadom() {
        return ranjivi.pretragaSaDveKolone(NAPAD_UNION);
    }

    public boolean prijavaBezLozinkeProlaziKrozRanjivu(String ciljniNalog) {
        return ranjivi.prijava(ciljniNalog + SUFIKS_KOMENTAR, "napadac-ne-zna-lozinku");
    }

    public boolean prijavaBezLozinkeProlaziKrozBezbednu(String ciljniNalog) {
        return bezbedniKorisnici.nadjiPoKorisnickomImenu(ciljniNalog + SUFIKS_KOMENTAR).isPresent();
    }

    public String izvestaj(String ciljniNalog) {
        return tautologija().izvestaj() + System.lineSeparator()
                + union().izvestaj() + System.lineSeparator()
                + String.format("%-24s ranjivo: %s   bezbedno: %s%n%-24s unos: %s",
                        "Komentar (--)",
                        prijavaBezLozinkeProlaziKrozRanjivu(ciljniNalog) ? "PRIJAVLJEN BEZ LOZINKE" : "odbijen",
                        prijavaBezLozinkeProlaziKrozBezbednu(ciljniNalog) ? "prijavljen" : "odbijen",
                        "", ciljniNalog + SUFIKS_KOMENTAR);
    }
}
