package shelfy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// The data in PostgreSQL through JDBC (Module 14.3 and 14.4). Same interface as the other storages.
// Every method opens a connection, uses it and closes it again (try-with-resources, Module 06.6):
// a connection that stays open is one of the planted mistakes of shelfy-broken.
// SQL always goes through PreparedStatement with ? placeholders, never through string concatenation.
public class PostgresStorage implements Storage {

    private final String url;
    private final String user;
    private final String password;

    public PostgresStorage(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public String name() {
        return "postgres";
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    // The same statements as day4/sql/schema.sql; IF NOT EXISTS makes a second start harmless.
    @Override
    public void init() {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS media (
                        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        kind text NOT NULL CHECK (kind IN ('BOOK', 'AUDIOBOOK')),
                        title text NOT NULL CHECK (length(title) BETWEEN 1 AND 200),
                        author text NOT NULL,
                        isbn text NOT NULL UNIQUE,
                        duration_minutes integer NOT NULL DEFAULT 0
                    )""");
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS members (
                        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        name text NOT NULL
                    )""");
            s.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS loans (
                        id integer GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                        medium_id integer NOT NULL REFERENCES media(id),
                        member_id integer NOT NULL REFERENCES members(id),
                        borrowed_on date NOT NULL,
                        due date NOT NULL,
                        returned_on date
                    )""");
        } catch (SQLException e) {
            throw new StorageException("cannot create the tables in " + url + ": " + e.getMessage(), e);
        }
    }

    @Override
    public List<Medium> media() {
        String sql = "SELECT id, kind, title, author, isbn, duration_minutes FROM media ORDER BY id";
        List<Medium> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(readMedium(rs));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot read media: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<Member> members() {
        String sql = "SELECT id, name FROM members ORDER BY id";
        List<Member> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Member(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot read members: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public List<Loan> loans() {
        String sql = "SELECT id, medium_id, member_id, borrowed_on, due, returned_on FROM loans ORDER BY id";
        List<Loan> result = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Loan(rs.getInt("id"), rs.getInt("medium_id"), rs.getInt("member_id"),
                        rs.getObject("borrowed_on", LocalDate.class), rs.getObject("due", LocalDate.class),
                        rs.getObject("returned_on", LocalDate.class)));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot read loans: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public Medium addMedium(Medium medium) {
        String sql = "INSERT INTO media (kind, title, author, isbn, duration_minutes) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, medium.kind().name());
            ps.setString(2, medium.title());
            ps.setString(3, medium.author());
            ps.setString(4, medium.isbn());
            ps.setInt(5, medium.durationMinutes());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return medium.withId(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot insert medium: " + e.getMessage(), e);
        }
    }

    @Override
    public Member addMember(Member member) {
        String sql = "INSERT INTO members (name) VALUES (?) RETURNING id";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, member.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return member.withId(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot insert member: " + e.getMessage(), e);
        }
    }

    @Override
    public Loan addLoan(Loan loan) {
        String sql = "INSERT INTO loans (medium_id, member_id, borrowed_on, due, returned_on) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, loan.mediumId());
            ps.setInt(2, loan.memberId());
            ps.setObject(3, loan.borrowedOn());
            ps.setObject(4, loan.due());
            ps.setObject(5, loan.returnedOn());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return loan.withId(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new StorageException("cannot insert loan: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateLoan(Loan loan) {
        String sql = "UPDATE loans SET returned_on = ? WHERE id = ?";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, loan.returnedOn());
            ps.setInt(2, loan.id());
            if (ps.executeUpdate() == 0) {
                throw new NoSuchLoanException(loan.id());
            }
        } catch (SQLException e) {
            throw new StorageException("cannot update loan " + loan.id() + ": " + e.getMessage(), e);
        }
    }

    private static Medium readMedium(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        MediaKind kind = MediaKind.valueOf(rs.getString("kind"));
        String title = rs.getString("title");
        String author = rs.getString("author");
        String isbn = rs.getString("isbn");
        return switch (kind) {
            case BOOK -> new Book(id, title, author, isbn);
            case AUDIOBOOK -> new Audiobook(id, title, author, isbn, rs.getInt("duration_minutes"));
        };
    }
}
