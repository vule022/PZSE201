package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.Recept;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ReceptDao extends OsnovniDao<Recept> {

    private static final String KOLONE =
            "id, pregled_id, naziv_leka, atc_sifra, doziranje, broj_pakovanja, datum_izdavanja, vazi_do";

    public ReceptDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "recept";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "datum_izdavanja DESC";
    }

    @Override
    public Recept sacuvaj(Recept r) {
        String sql = """
                INSERT INTO recept
                    (pregled_id, naziv_leka, atc_sifra, doziranje, broj_pakovanja, datum_izdavanja, vazi_do)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, r);
            r.setId(izvrsiInsert(ps));
            return r;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje recepta (" + r.getNazivLeka() + ").", e);
        }
    }

    @Override
    public boolean azuriraj(Recept r) {
        if (r.getId() == null) {
            throw new IllegalArgumentException("Recept bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE recept SET pregled_id = ?, naziv_leka = ?, atc_sifra = ?, doziranje = ?,
                                  broj_pakovanja = ?, datum_izdavanja = ?, vazi_do = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, r);
            ps.setInt(8, r.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje recepta (id=" + r.getId() + ").", e);
        }
    }

    public List<Recept> zaPregled(int pregledId) {
        String sql = "SELECT " + KOLONE + " FROM recept WHERE pregled_id = ? ORDER BY naziv_leka";
        return lista(sql, ps -> ps.setInt(1, pregledId),
                "Neuspelo citanje recepata pregleda (id=" + pregledId + ").");
    }

    public List<Recept> zaPacijenta(int pacijentId) {
        String sql = """
                SELECT r.id, r.pregled_id, r.naziv_leka, r.atc_sifra, r.doziranje,
                       r.broj_pakovanja, r.datum_izdavanja, r.vazi_do
                FROM recept r
                JOIN pregled p ON p.id = r.pregled_id
                WHERE p.pacijent_id = ?
                ORDER BY r.datum_izdavanja DESC, r.id DESC
                """;
        return lista(sql, ps -> ps.setInt(1, pacijentId),
                "Neuspelo citanje recepata pacijenta (id=" + pacijentId + ").");
    }

    public List<Recept> vazeciZaPacijenta(int pacijentId, LocalDate naDan) {
        String sql = """
                SELECT r.id, r.pregled_id, r.naziv_leka, r.atc_sifra, r.doziranje,
                       r.broj_pakovanja, r.datum_izdavanja, r.vazi_do
                FROM recept r
                JOIN pregled p ON p.id = r.pregled_id
                WHERE p.pacijent_id = ?
                  AND r.datum_izdavanja <= ?
                  AND r.vazi_do >= ?
                ORDER BY r.vazi_do
                """;
        return lista(sql, ps -> {
            ps.setInt(1, pacijentId);
            ps.setString(2, naDan.toString());
            ps.setString(3, naDan.toString());
        }, "Neuspelo citanje vazecih recepata pacijenta.");
    }

    private static void popuni(PreparedStatement ps, Recept r) throws SQLException {
        ps.setInt(1, r.getPregledId());
        ps.setString(2, r.getNazivLeka());
        ps.setString(3, r.getAtcSifra());
        ps.setString(4, r.getDoziranje());
        ps.setInt(5, r.getBrojPakovanja());
        ps.setString(6, tekst(r.getDatumIzdavanja()));
        ps.setString(7, tekst(r.getVaziDo()));
    }

    @Override
    protected Recept procitaj(ResultSet rs) throws SQLException {
        Recept r = new Recept();
        r.setId(rs.getInt("id"));
        r.setPregledId(rs.getInt("pregled_id"));
        r.setNazivLeka(rs.getString("naziv_leka"));
        r.setAtcSifra(rs.getString("atc_sifra"));
        r.setDoziranje(rs.getString("doziranje"));
        r.setBrojPakovanja(rs.getInt("broj_pakovanja"));
        r.setDatumIzdavanja(datum(rs.getString("datum_izdavanja")));
        r.setVaziDo(datum(rs.getString("vazi_do")));
        return r;
    }
}
