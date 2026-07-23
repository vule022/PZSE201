package rs.medikarton.dao;

import rs.medikarton.izuzeci.PodaciException;
import rs.medikarton.model.Korisnik;
import rs.medikarton.model.Uloga;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class KorisnikDao extends OsnovniDao<Korisnik> {

    private static final String KOLONE = """
            id, korisnicko_ime, lozinka_hash, so, uloga, aktivan,
            broj_neuspelih_prijava, zakljucan_do, pacijent_id, lekar_id
            """;

    public KorisnikDao(Connection veza) {
        super(veza);
    }

    @Override
    protected String tabela() {
        return "korisnik";
    }

    @Override
    protected String kolone() {
        return KOLONE;
    }

    @Override
    protected String redosled() {
        return "korisnicko_ime";
    }

    @Override
    public Korisnik sacuvaj(Korisnik k) {
        String sql = """
                INSERT INTO korisnik
                    (korisnicko_ime, lozinka_hash, so, uloga, aktivan,
                     broj_neuspelih_prijava, zakljucan_do, pacijent_id, lekar_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = pripremiSaKljucem(sql)) {
            popuni(ps, k);
            k.setId(izvrsiInsert(ps));
            return k;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo cuvanje korisnika (" + k.getKorisnickoIme() + ").", e);
        }
    }

    @Override
    public boolean azuriraj(Korisnik k) {
        if (k.getId() == null) {
            throw new IllegalArgumentException("Korisnik bez identifikatora ne moze da se azurira.");
        }
        String sql = """
                UPDATE korisnik SET
                    korisnicko_ime = ?, lozinka_hash = ?, so = ?, uloga = ?, aktivan = ?,
                    broj_neuspelih_prijava = ?, zakljucan_do = ?, pacijent_id = ?, lekar_id = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = veza.prepareStatement(sql)) {
            popuni(ps, k);
            ps.setInt(10, k.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new PodaciException("Neuspelo azuriranje korisnika (id=" + k.getId() + ").", e);
        }
    }

    public Optional<Korisnik> nadjiPoKorisnickomImenu(String korisnickoIme) {
        return jedan("SELECT " + KOLONE + " FROM korisnik WHERE korisnicko_ime = ?",
                ps -> ps.setString(1, korisnickoIme),
                "Neuspelo citanje korisnika po korisnickom imenu.");
    }

    private static void popuni(PreparedStatement ps, Korisnik k) throws SQLException {
        ps.setString(1, k.getKorisnickoIme());
        ps.setString(2, k.getLozinkaHash());
        ps.setString(3, k.getSo());
        ps.setString(4, k.getUloga().name());
        ps.setInt(5, k.isAktivan() ? 1 : 0);
        ps.setInt(6, k.getBrojNeuspelihPrijava());
        ps.setString(7, tekst(k.getZakljucanDo()));
        postaviCeoBrojIliNull(ps, 8, k.getPacijentId());
        postaviCeoBrojIliNull(ps, 9, k.getLekarId());
    }

    @Override
    protected Korisnik procitaj(ResultSet rs) throws SQLException {
        Korisnik k = new Korisnik();
        k.setId(rs.getInt("id"));
        k.setKorisnickoIme(rs.getString("korisnicko_ime"));
        k.setLozinkaHash(rs.getString("lozinka_hash"));
        k.setSo(rs.getString("so"));
        k.setUloga(Uloga.izTeksta(rs.getString("uloga")));
        k.setAktivan(rs.getInt("aktivan") == 1);
        k.setBrojNeuspelihPrijava(rs.getInt("broj_neuspelih_prijava"));
        k.setZakljucanDo(vreme(rs.getString("zakljucan_do")));
        k.setPacijentId(celiBrojIliNull(rs, "pacijent_id"));
        k.setLekarId(celiBrojIliNull(rs, "lekar_id"));
        return k;
    }
}
