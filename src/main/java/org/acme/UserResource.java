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
                statement.setQueryTimeout(30);
                statement.executeUpdate("DROP TABLE IF EXISTS users");
                statement.executeUpdate("""
                        CREATE TABLE users(
                          id INTEGER PRIMARY KEY,
                          first_name TEXT,
                          last_name TEXT,
                          street TEXT,
                          zip_code TEXT,
                          city TEXT,
                          country TEXT,
                          phone TEXT,
                          birthday TEXT
                        )
                        """);
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

            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO users
                      (first_name, last_name, street, zip_code, city, country, phone, birthday)
                      VALUES(?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                for (int n = 1; n <= numberOfRecords; n++) {
                    Address address = faker.address();

                    ps.setString(1, faker.name().firstName());
                    ps.setString(2, faker.name().lastName());
                    ps.setString(3, address.streetAddress());
                    ps.setString(4, address.zipCode());
                    ps.setString(5, address.city());
                    ps.setString(6, address.country());
                    ps.setString(7, faker.phoneNumber().cellPhone());
                    ps.setString(8,
                            LocalDate.ofInstant(faker.date().birthday().toInstant(), ZoneId.of("UTC")).toString());
                    ps.execute();
                }
            }
            connection.commit();
            userCount = getUserCount();

            return "Inserted %s record(s) in %s ms. Total records: %s".formatted(numberOfRecords,
                    System.currentTimeMillis() - start, userCount);
        }
    }

    private int getUserCount() throws SQLException {
        try (Connection connection = ds.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet rs = statement.executeQuery("SELECT COUNT(1) FROM users")) {
                    rs.next();
                    return rs.getInt(1);
                }
            }
        }
    }
}
