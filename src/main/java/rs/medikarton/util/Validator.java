package rs.medikarton.util;

import rs.medikarton.izuzeci.ValidacijaException;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Validator {

    private static final Pattern IME = Pattern.compile(
            "^[A-Za-zČčĆćĐđŠšŽž][A-Za-zČčĆćĐđŠšŽž'\\- ]{1,39}$");

    private static final Pattern UDVOJEN_RAZDVAJAC = Pattern.compile("(['\\- ])\\1");

    private static final Pattern EMAIL = Pattern.compile(
            "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private static final Pattern TELEFON = Pattern.compile(
            "^(\\+381|0)6[0-9]{7,8}$");

    private static final Pattern JMBG = Pattern.compile(
            "^(?<dan>\\d{2})(?<mesec>\\d{2})(?<godina>\\d{3})"
                    + "(?<region>\\d{2})(?<redni>\\d{3})(?<kontrola>\\d)$");

    private static final Pattern MKB10 = Pattern.compile("^[A-Z][0-9]{2}(\\.[0-9]{1,2})?$");

    private static final Pattern ATC = Pattern.compile("^[A-Z][0-9]{2}[A-Z]{2}[0-9]{2}$");

    private static final Pattern JAKA_LOZINKA = Pattern.compile(
            "^(?=.*[a-zčćđšž])(?=.*[A-ZČĆĐŠŽ])(?=.*\\d)(?=.*[^\\p{L}\\p{N}]).{10,64}$");

    private static final Pattern KRVNA_GRUPA = Pattern.compile("^(A|B|AB|0)[+\\-]$");

    private Validator() {
        throw new AssertionError("Validator se ne instancira.");
    }

    //Proverava da li je tekst uopste unet
    public static String zahtevajTekst(String vrednost, String polje) {
        if (vrednost == null || vrednost.isBlank()) {
            throw new ValidacijaException(polje, "Polje je obavezno.");
        }
        return vrednost.trim();
    }

    //Proverava duzinu
    public static String zahtevajDuzinu(String vrednost, String polje, int min, int max) {
        String ocisceno = zahtevajTekst(vrednost, polje);
        if (ocisceno.length() < min || ocisceno.length() > max) {
            throw new ValidacijaException(polje,
                    "Duzina mora biti izmedju " + min + " i " + max + " znakova, a uneto je "
                            + ocisceno.length() + ".");
        }
        return ocisceno;
    }

    //Proverava broj
    public static double zahtevajUOpsegu(double vrednost, String polje, double min, double max) {
        if (Double.isNaN(vrednost) || Double.isInfinite(vrednost)) {
            throw new ValidacijaException(polje, "Vrednost nije konacan broj.");
        }
        if (vrednost < min || vrednost > max) {
            throw new ValidacijaException(polje,
                    "Vrednost " + vrednost + " je van dozvoljenog opsega [" + min + ", " + max + "].");
        }
        return vrednost;
    }

    public static int zahtevajUOpsegu(int vrednost, String polje, int min, int max) {
        if (vrednost < min || vrednost > max) {
            throw new ValidacijaException(polje,
                    "Vrednost " + vrednost + " je van dozvoljenog opsega [" + min + ", " + max + "].");
        }
        return vrednost;
    }

    //Proverava ime i prezime
    public static String validirajIme(String vrednost, String polje) {
        String ocisceno = zahtevajTekst(vrednost, polje);
        if (!IME.matcher(ocisceno).matches()) {
            throw new ValidacijaException(polje,
                    "Dozvoljena su samo slova, crtica i apostrof; 2-40 znakova, pocinje slovom.");
        }
        if (UDVOJEN_RAZDVAJAC.matcher(ocisceno).find()) {
            throw new ValidacijaException(polje, "Znak za razdvajanje ne sme biti udvojen.");
        }
        return ocisceno;
    }

    //Proverava email
    public static String validirajEmail(String vrednost) {
        String ocisceno = zahtevajDuzinu(vrednost, "email", 5, 254).toLowerCase();
        if (!EMAIL.matcher(ocisceno).matches()) {
            throw new ValidacijaException("email", "Neispravan format e-adrese.");
        }
        return ocisceno;
    }

    public static String validirajTelefon(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "telefon").replaceAll("[\\s/\\-]", "");
        if (!TELEFON.matcher(ocisceno).matches()) {
            throw new ValidacijaException("telefon",
                    "Ocekivan je mobilni broj u formatu 06XXXXXXXX ili +3816XXXXXXXX.");
        }
        return ocisceno;
    }

    //Jedinstevni jmbg i proverava cifara
    public static String validirajJmbg(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "jmbg");
        Matcher m = JMBG.matcher(ocisceno);
        if (!m.matches()) {
            throw new ValidacijaException("jmbg", "JMBG mora imati tacno 13 cifara.");
        }
        if (datumIzJmbg(ocisceno) == null) {
            throw new ValidacijaException("jmbg", "Prvih sedam cifara ne cine ispravan datum rodjenja.");
        }
        if (!kontrolnaCifraJmbgValidna(ocisceno)) {
            throw new ValidacijaException("jmbg", "Kontrolna cifra JMBG-a nije ispravna.");
        }
        return ocisceno;
    }

    //izdvajanje datuma iz hmbg
    public static LocalDate datumIzJmbg(String jmbg) {
        Matcher m = JMBG.matcher(jmbg);
        if (!m.matches()) {
            return null;
        }
        int dan = Integer.parseInt(m.group("dan"));
        int mesec = Integer.parseInt(m.group("mesec"));
        int godinaTriCifre = Integer.parseInt(m.group("godina"));
        int godina = godinaTriCifre >= 900 ? 1000 + godinaTriCifre : 2000 + godinaTriCifre;
        try {
            return LocalDate.of(godina, mesec, dan);
        } catch (DateTimeException e) {
            return null;
        }
    }


    public static boolean kontrolnaCifraJmbgValidna(String jmbg) {
        if (jmbg == null || !jmbg.matches("\\d{13}")) {
            return false;
        }
        int[] c = new int[13];
        for (int i = 0; i < 13; i++) {
            c[i] = jmbg.charAt(i) - '0';
        }
        int zbir = 7 * (c[0] + c[6]) + 6 * (c[1] + c[7]) + 5 * (c[2] + c[8])
                + 4 * (c[3] + c[9]) + 3 * (c[4] + c[10]) + 2 * (c[5] + c[11]);
        int m = 11 - (zbir % 11);
        int ocekivana = (m >= 1 && m <= 9) ? m : 0;
        return ocekivana == c[12];
    }

    //Format MKB sifre
    public static String validirajMkb(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "dijagnozaMkb").toUpperCase();
        if (!MKB10.matcher(ocisceno).matches()) {
            throw new ValidacijaException("dijagnozaMkb",
                    "Ocekivana je MKB-10 sifra, npr. I10 ili J06.9.");
        }
        return ocisceno;
    }

    //Format atc sifre
    public static String validirajAtc(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "atcSifra").toUpperCase();
        if (!ATC.matcher(ocisceno).matches()) {
            throw new ValidacijaException("atcSifra", "Ocekivana je ATC sifra, npr. C09AA05.");
        }
        return ocisceno;
    }

    //Provera krvne grupe
    public static String validirajKrvnuGrupu(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "krvnaGrupa").toUpperCase();
        if (!KRVNA_GRUPA.matcher(ocisceno).matches()) {
            throw new ValidacijaException("krvnaGrupa", "Dozvoljene vrednosti: A+, A-, B+, B-, AB+, AB-, 0+, 0-.");
        }
        return ocisceno;
    }

    public static String validirajPol(String vrednost) {
        String ocisceno = zahtevajTekst(vrednost, "pol").toUpperCase();
        if (!ocisceno.equals("M") && !ocisceno.equals("Z")) {
            throw new ValidacijaException("pol", "Dozvoljene vrednosti su M i Z.");
        }
        return ocisceno;
    }

    public static String validirajLozinku(String vrednost) {
        if (vrednost == null || !JAKA_LOZINKA.matcher(vrednost).matches()) {
            throw new ValidacijaException("lozinka",
                    "Lozinka mora imati 10-64 znaka, i to bar jedno malo slovo, jedno veliko, "
                            + "jednu cifru i jedan specijalan znak.");
        }
        return vrednost;
    }

    //validno vreme i datum termina
    public static LocalDateTime validirajTerminPocetak(LocalDateTime pocetak, LocalDateTime sada) {
        if (pocetak == null) {
            throw new ValidacijaException("pocetak", "Vreme termina je obavezno.");
        }
        if (!pocetak.isAfter(sada)) {
            throw new ValidacijaException("pocetak", "Termin ne moze da se zakaze u proslosti.");
        }
        if (pocetak.isAfter(sada.plusYears(1))) {
            throw new ValidacijaException("pocetak", "Termin se zakazuje najvise godinu dana unapred.");
        }
        int sat = pocetak.getHour();
        if (sat < 7 || sat >= 20) {
            throw new ValidacijaException("pocetak", "Klinika radi od 07:00 do 20:00.");
        }
        var dan = pocetak.getDayOfWeek();
        if (dan == java.time.DayOfWeek.SATURDAY || dan == java.time.DayOfWeek.SUNDAY) {
            throw new ValidacijaException("pocetak", "Termini se zakazuju samo radnim danima.");
        }
        return pocetak;
    }
}
