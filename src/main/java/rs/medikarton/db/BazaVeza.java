package rs.medikarton.db;

import rs.medikarton.izuzeci.PodaciException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class BazaVeza {

    public static final String PODRAZUMEVANI_URL = "jdbc:sqlite:podaci/medikarton.db";
    public static final String MEMORIJSKI_URL = "jdbc:sqlite::memory:";

    private static BazaVeza instanca;

    private final String jdbcUrl;
    private Connection veza;

    private BazaVeza(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    //vraca instancu baze, ponistava
    public static synchronized BazaVeza instanca() {
        return instanca(PODRAZUMEVANI_URL);
    }

    public static synchronized BazaVeza instanca(String jdbcUrl) {
        if (instanca == null) {
            instanca = new BazaVeza(jdbcUrl);
        } else if (!instanca.jdbcUrl.equals(jdbcUrl)) {
            throw new IllegalStateException(
                    "BazaVeza je vec inicijalizovana sa URL-om " + instanca.jdbcUrl
                            + "; pozovi ponistiInstancu() pre prelaska na " + jdbcUrl + ".");
        }
        return instanca;
    }

    public static synchronized void ponistiInstancu() {
        if (instanca != null) {
            instanca.zatvori();
            instanca = null;
        }
    }

    //conn
    public Connection veza() {
        try {
            if (veza == null || veza.isClosed()) {
                if (jdbcUrl.startsWith("jdbc:sqlite:") && !jdbcUrl.contains(":memory:")) {
                    napraviDirektorijumAkoTreba(jdbcUrl);
                }
                veza = DriverManager.getConnection(jdbcUrl);
                podesiPragme(veza);
            }
            return veza;
        } catch (SQLException e) {
            throw new PodaciException("Nije moguce otvoriti vezu ka bazi: " + jdbcUrl, e);
        }
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void zatvori() {
        try {
            if (veza != null && !veza.isClosed()) {
                veza.close();
            }
        } catch (SQLException e) {
            throw new PodaciException("Greska pri zatvaranju veze ka bazi.", e);
        } finally {
            veza = null;
        }
    }

    public static void podesiPragme(Connection veza) throws SQLException {
        try (Statement s = veza.createStatement()) {
            s.execute("PRAGMA foreign_keys = ON");
            s.execute("PRAGMA journal_mode = WAL");
        }
    }

    private static void napraviDirektorijumAkoTreba(String jdbcUrl) {
        String putanja = jdbcUrl.substring("jdbc:sqlite:".length());
        int poslednjiSeparator = putanja.lastIndexOf('/');
        if (poslednjiSeparator > 0) {
            new java.io.File(putanja.substring(0, poslednjiSeparator)).mkdirs();
        }
    }
}
