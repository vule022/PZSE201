package rs.medikarton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import rs.medikarton.dao.DaoFabrika;
import rs.medikarton.db.BazaVeza;
import rs.medikarton.db.Migracije;
import rs.medikarton.model.Lekar;
import rs.medikarton.model.Pacijent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;

abstract class OsnovniTest {

    protected Connection veza;
    protected DaoFabrika fabrika;

    @BeforeEach
    void pripremiBazu() throws SQLException {
        veza = DriverManager.getConnection(BazaVeza.MEMORIJSKI_URL);
        BazaVeza.podesiPragme(veza);
        Migracije.kreirajSemu(veza);
        fabrika = new DaoFabrika(veza);
    }

    @AfterEach
    void zatvoriBazu() throws SQLException {
        if (veza != null && !veza.isClosed()) {
            veza.close();
        }
    }

    protected static final String JMBG_NIKOLA = "1203985710122";
    protected static final String JMBG_MILICA = "2507990725157";
    protected static final String JMBG_DRAGAN = "0411978710344";

    protected Pacijent dodajPacijenta(String jmbg, String ime, String prezime) {
        Pacijent p = new Pacijent(jmbg, ime, prezime, LocalDate.of(1985, 3, 12), "M",
                ime.toLowerCase() + "@example.rs", "0631234567");
        p.setKrvnaGrupa("A+");
        return fabrika.pacijenti().sacuvaj(p);
    }

    protected Lekar dodajLekara(String licenca, String ime, String prezime, String specijalizacija) {
        return fabrika.lekari().sacuvaj(
                new Lekar(licenca, ime, prezime, specijalizacija, ime.toLowerCase() + "@medikarton.rs"));
    }
}
