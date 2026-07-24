package rs.medikarton.dao;

import rs.medikarton.db.BazaVeza;
import rs.medikarton.db.Migracije;

import java.sql.Connection;
import java.util.Objects;

public class DaoFabrika {

    private final Connection veza;
    private PacijentDao pacijentDao;
    private LekarDao lekarDao;
    private KorisnikDao korisnikDao;
    private TerminDao terminDao;
    private PregledDao pregledDao;
    private ReceptDao receptDao;
    private LabNalazDao labNalazDao;

    public DaoFabrika(Connection veza) {
        this.veza = Objects.requireNonNull(veza, "Veza ka bazi ne sme biti null.");
    }

    public static DaoFabrika podrazumevana() {
        Connection veza = BazaVeza.instanca().veza();
        Migracije.kreirajSemu(veza);
        return new DaoFabrika(veza);
    }

    public static DaoFabrika memorijska() {
        Connection veza = BazaVeza.instanca(BazaVeza.MEMORIJSKI_URL).veza();
        Migracije.kreirajSemu(veza);
        return new DaoFabrika(veza);
    }

    public Connection veza() {
        return veza;
    }

    public PacijentDao pacijenti() {
        return pacijentDao != null ? pacijentDao : (pacijentDao = new PacijentDao(veza));
    }

    public LekarDao lekari() {
        return lekarDao != null ? lekarDao : (lekarDao = new LekarDao(veza));
    }

    public KorisnikDao korisnici() {
        return korisnikDao != null ? korisnikDao : (korisnikDao = new KorisnikDao(veza));
    }

    public TerminDao termini() {
        return terminDao != null ? terminDao : (terminDao = new TerminDao(veza));
    }

    public PregledDao pregledi() {
        return pregledDao != null ? pregledDao : (pregledDao = new PregledDao(veza));
    }

    public ReceptDao recepti() {
        return receptDao != null ? receptDao : (receptDao = new ReceptDao(veza));
    }

    public LabNalazDao nalazi() {
        return labNalazDao != null ? labNalazDao : (labNalazDao = new LabNalazDao(veza));
    }
}
