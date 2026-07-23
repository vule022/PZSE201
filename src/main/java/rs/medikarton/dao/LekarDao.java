package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.Lekar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class LekarDao extends OsnovniDao<Lekar> {

    private static final String KOLONE =
            "id, broj_licence, ime, prezime, specijalizacija, email, trajanje_pregleda_min";

    public LekarDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "lekar";
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
    public Lekar sacuvaj(Lekar l) {
        String sql = """
                INSERT INTO lekar (broj_licence, ime, prezime, specijalizacija, email, trajanje_pregleda_min)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, l);
            l.setId(izvrsiInsert(ps));
            return l;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje lekara (licenca=" + l.getBrojLicence() + ").", e);
        }
    }

    @Override
    public boolean azuriraj(Lekar l) {
        if (l.getId() == null) {
            throw new IllegalArgumentException("Lekar bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE lekar SET broj_licence = ?, ime = ?, prezime = ?, specijalizacija = ?,
                                 email = ?, trajanje_pregleda_min = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, l);
            ps.setInt(7, l.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje lekara (id=" + l.getId() + ").", e);
        }
    }

    public List<Lekar> poSpecijalizaciji(String specijalizacija) {
        String sql = "SELECT " + KOLONE + " FROM lekar WHERE specijalizacija = ? ORDER BY prezime";
        return lista(sql, ps -> ps.setString(1, specijalizacija),
                "Neuspelo citanje lekara po specijalizaciji.");
    }

    private static void popuni(PreparedStatement ps, Lekar l) throws SQLException {
        ps.setString(1, l.getBrojLicence());
        ps.setString(2, l.getIme());
        ps.setString(3, l.getPrezime());
        ps.setString(4, l.getSpecijalizacija());
        ps.setString(5, l.getEmail());
        ps.setInt(6, l.getTrajanjePregledaMin());
    }

    @Override
    protected Lekar procitaj(ResultSet rs) throws SQLException {
        Lekar l = new Lekar();
        l.setId(rs.getInt("id"));
        l.setBrojLicence(rs.getString("broj_licence"));
        l.setIme(rs.getString("ime"));
        l.setPrezime(rs.getString("prezime"));
        l.setSpecijalizacija(rs.getString("specijalizacija"));
        l.setEmail(rs.getString("email"));
        l.setTrajanjePregledaMin(rs.getInt("trajanje_pregleda_min"));
        return l;
    }
}
