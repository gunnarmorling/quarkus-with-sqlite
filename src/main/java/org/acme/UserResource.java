package org.acme;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ThreadLocalRandom;

import com.github.javafaker.Address;
import com.github.javafaker.Faker;

import io.agroal.api.AgroalDataSource;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/user")
public class UserResource {

    @Inject
    AgroalDataSource ds;

    long userCount = 0;

    public void initUserCount(@Observes StartupEvent se) throws Exception {
        userCount = getUserCount();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    @Path("/random")
    public User getRandom() {
        long id = ThreadLocalRandom.current().nextLong(1, userCount + 1);
        return User.findById(id);
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/admin/drop-and-create")
    public void dropAndCreateTable() throws Exception {
        try (Connection connection = ds.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DROP INDEX IF EXISTS addresses_user_id");
                statement.executeUpdate("DROP INDEX IF EXISTS phone_numbers_user_id");
                statement.executeUpdate("DROP TABLE IF EXISTS addresses");
                statement.executeUpdate("DROP TABLE IF EXISTS phone_numbers");
                statement.executeUpdate("DROP TABLE IF EXISTS users");
                statement.executeUpdate("""
                        CREATE TABLE users(
                          id INTEGER PRIMARY KEY,
                          first_name TEXT,
                          last_name TEXT,
                          email TEXT,
                          birthday TEXT
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE addresses(
                          id INTEGER PRIMARY KEY,
                          user_id INTEGER,
                          street TEXT,
                          zip_code TEXT,
                          city TEXT,
                          country TEXT,
                          FOREIGN KEY(user_id) REFERENCES users(id)
                        )
                        """);
                statement.executeUpdate("CREATE INDEX addresses_user_id ON addresses(user_id)");
                statement.executeUpdate("""
                        CREATE TABLE phone_numbers(
                          user_id INTEGER,
                          type TEXT,
                          number TEXT,
                          PRIMARY KEY(user_id, type)
                          FOREIGN KEY(user_id) REFERENCES users(id)
                        )
                        """);
                statement.executeUpdate("CREATE INDEX phone_numbers_user_id ON phone_numbers(user_id)");
                userCount = 0;
            }
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/admin/test-data")
    public String createTestData(@QueryParam("n") int numberOfRecords) throws Exception {
        long start = System.currentTimeMillis();
        Faker faker = new Faker();

        try (Connection connection = ds.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement insertUser = connection.prepareStatement("""
                    INSERT INTO users
                      (first_name, last_name, email, birthday)
                      VALUES(?, ?, ?, ?)
                      RETURNING id
                    """, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertAddress = connection.prepareStatement("""
                    INSERT INTO addresses
                      (user_id, street, zip_code, city, country)
                      VALUES(?, ?, ?, ?, ?)
                    """);
                PreparedStatement insertPhoneNo = connection.prepareStatement("""
                        INSERT INTO phone_numbers
                          (user_id, type, number)
                          VALUES(?, ?, ?)
                        """)) {
                for (int n = 1; n <= numberOfRecords; n++) {
                    insertUser.setString(1, faker.name().firstName());
                    insertUser.setString(2, faker.name().lastName());
                    insertUser.setString(3, faker.internet().emailAddress());
                    insertUser.setString(4,
                            LocalDate.ofInstant(faker.date().birthday().toInstant(), ZoneId.of("UTC")).toString());
                    insertUser.execute();
                    long userId = getGeneratedId(insertUser);

                    long addressCount = ThreadLocalRandom.current().nextInt(1, 5);
                    for (int a = 0; a < addressCount; a++) {
                        Address address = faker.address();
                        insertAddress.setLong(1, userId);
                        insertAddress.setString(2, address.streetAddress());
                        insertAddress.setString(3, address.zipCode());
                        insertAddress.setString(4, address.city());
                        insertAddress.setString(5, address.country());
                        insertAddress.execute();
                    }

                    insertPhoneNo.setLong(1, userId);
                    insertPhoneNo.setString(2, "cell");
                    insertPhoneNo.setString(3, faker.phoneNumber().cellPhone());
                    insertPhoneNo.execute();

                    insertPhoneNo.setLong(1, userId);
                    insertPhoneNo.setString(2, "home");
                    insertPhoneNo.setString(3, faker.phoneNumber().phoneNumber());
                    insertPhoneNo.execute();
                }
            }

            connection.commit();
            userCount = getUserCount();

            return "Inserted %s record(s) in %s ms. Total records: %s".formatted(numberOfRecords,
                    System.currentTimeMillis() - start, userCount);
        }
    }

    private long getGeneratedId(PreparedStatement insertUser) throws SQLException {
        try(ResultSet res = insertUser.getResultSet()) {
            res.next();
            return res.getLong(1);
        }
    }

    private int getUserCount() throws SQLException {
        try (Connection connection = ds.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")) {
                    if (!rs.next()) {
                        return 0;
                    }
                }

                try (ResultSet rs = statement.executeQuery("SELECT COUNT(1) FROM users")) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }
}
