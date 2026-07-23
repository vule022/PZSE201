package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.Pacijent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PacijentDao extends OsnovniDao<Pacijent> {

    private static final String KOLONE =
            "id, jmbg, ime, prezime, datum_rodjenja, pol, email, telefon, adresa, krvna_grupa, alergije";

    public PacijentDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "pacijent";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "prezime, ime";
    }

    @Override
    public Pacijent sacuvaj(Pacijent p) {
        String sql = """
                INSERT INTO pacijent
                    (jmbg, ime, prezime, datum_rodjenja, pol, email, telefon, adresa, krvna_grupa, alergije)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, p);
            p.setId(izvrsiInsert(ps));
            return p;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje pacijenta (jmbg=" + p.getJmbg() + ").", e);
        }
    }

    @Override
    public boolean azuriraj(Pacijent p) {
        if (p.getId() == null) {
            throw new IllegalArgumentException("Pacijent bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE pacijent SET
                    jmbg = ?, ime = ?, prezime = ?, datum_rodjenja = ?, pol = ?,
                    email = ?, telefon = ?, adresa = ?, krvna_grupa = ?, alergije = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, p);
            ps.setInt(11, p.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje pacijenta (id=" + p.getId() + ").", e);
        }
    }

    public Optional<Pacijent> nadjiPoJmbg(String jmbg) {
        return jedan("SELECT " + KOLONE + " FROM pacijent WHERE jmbg = ?",
                ps -> ps.setString(1, jmbg), "Neuspela pretraga pacijenta po JMBG-u.");
    }

    public List<Pacijent> pretraziPoPrezimenu(String deoPrezimena) {
        String sql = "SELECT " + KOLONE + " FROM pacijent WHERE prezime LIKE ? ORDER BY prezime, ime LIMIT 100";
        return lista(sql, ps -> ps.setString(1, deoPrezimena + "%"),
                "Neuspela pretraga pacijenata po prezimenu.");
    }

    public int sacuvajSve(List<Pacijent> pacijenti) {
        String sql = """
                INSERT INTO pacijent
                    (jmbg, ime, prezime, datum_rodjenja, pol, email, telefon, adresa, krvna_grupa, alergije)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        boolean prethodniAutoCommit = true;
        try {
            prethodniAutoCommit = veza.getAutoCommit();
            veza.setAutoCommit(false);
            int upisano;
            try (PreparedStatement ps = veza.prepareStatement(sql)) {
                for (Pacijent p : pacijenti) {
                    popuni(ps, p);
                    ps.addBatch();
                }
                upisano = java.util.Arrays.stream(ps.executeBatch()).sum();
            }
            veza.commit();
            return upisano;
        } catch (SQLException e) {
            vratiUnazad();
            throw new PodaciException("Neuspeo grupni upis pacijenata.", e);
        } finally {
            vratiAutoCommit(prethodniAutoCommit);
        }
    }

    private void vratiUnazad() {
        try {
            veza.rollback();
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo ponistavanje transakcije.", e);
        }
    }

    private void vratiAutoCommit(boolean vrednost) {
        try {
            veza.setAutoCommit(vrednost);
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo vracanje autoCommit rezima.", e);
        }
    }

    private static void popuni(PreparedStatement ps, Pacijent p) throws SQLException {
        ps.setString(1, p.getJmbg());
        ps.setString(2, p.getIme());
        ps.setString(3, p.getPrezime());
        ps.setString(4, tekst(p.getDatumRodjenja()));
        ps.setString(5, p.getPol());
        ps.setString(6, p.getEmail());
        ps.setString(7, p.getTelefon());
        ps.setString(8, p.getAdresa());
        ps.setString(9, p.getKrvnaGrupa());
        ps.setString(10, p.getAlergije() == null ? "" : p.getAlergije());
    }

    @Override
    protected Pacijent procitaj(ResultSet rs) throws SQLException {
        Pacijent p = new Pacijent();
        p.setId(rs.getInt("id"));
        p.setJmbg(rs.getString("jmbg"));
        p.setIme(rs.getString("ime"));
        p.setPrezime(rs.getString("prezime"));
        p.setDatumRodjenja(datum(rs.getString("datum_rodjenja")));
        p.setPol(rs.getString("pol"));
        p.setEmail(rs.getString("email"));
        p.setTelefon(rs.getString("telefon"));
        p.setAdresa(rs.getString("adresa"));
        p.setKrvnaGrupa(rs.getString("krvna_grupa"));
        p.setAlergije(rs.getString("alergije"));
        return p;
    }
}
