package cloud.dpgmedia;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeopleStorePostgres {

    // Database connection details from environment variables
    // Sensitive credentials (user/password) must be provided via environment variables
    private static final String DB_HOST = getEnvOrDefault("DB_HOST", "localhost");
    private static final String DB_PORT = getEnvOrDefault("DB_PORT", "5432");
    private static final String DB_NAME = getEnvOrDefault("DB_NAME", "webapp_db");
    private static final String USER = getRequiredEnv("DB_USER");
    private static final String PASSWORD = getRequiredEnv("DB_PASSWORD");
    private static final String URL = String.format("jdbc:postgresql://%s:%s/%s", DB_HOST, DB_PORT, DB_NAME);

    /**
     * Get environment variable with default fallback
     */
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
    }

    /**
     * Get required environment variable, throw exception if not set
     */
    private static String getRequiredEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(
                String.format("Required environment variable '%s' is not set. " +
                             "Please set database credentials via environment variables.", key)
            );
        }
        return value;
    }


    // I do not like the signature yet
    public Person registerPerson(Person person){
        String sql = "INSERT INTO people (firstName, lastName, dateOfBirth, timeOfRegistration, latitude, longitude, behavior, version) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id, firstName, lastName, dateOfBirth, timeOfRegistration, latitude, longitude, behavior, version";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind values to the placeholders
            preparedStatement.setString(1, person.firstName);
            preparedStatement.setString(2, person.lastName);
            preparedStatement.setDate(3, Date.valueOf(person.dateOfBirth));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(person.timeOfRegistration));
            if (person.addressLocation != null) {
                preparedStatement.setDouble(5, person.addressLocation.getLatitude());
                preparedStatement.setDouble(6, person.addressLocation.getLongitude());
            } else {
                preparedStatement.setNull(5, Types.DOUBLE);
                preparedStatement.setNull(6, Types.DOUBLE);
            }
            preparedStatement.setObject(7, person.behavior.name().toLowerCase(), java.sql.Types.OTHER);
            preparedStatement.setInt(8, person.version);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                person.id = Optional.of(resultSet.getInt("id"));
                person.firstName = resultSet.getString("firstName");
                person.lastName = resultSet.getString("lastName");
                person.dateOfBirth = resultSet.getDate("dateOfBirth").toLocalDate();
                person.timeOfRegistration = resultSet.getTimestamp("timeOfRegistration").toLocalDateTime();
                person.addressLocation = new Location(
                        resultSet.getDouble("latitude"),
                        resultSet.getDouble("longitude")
                );
                person.behavior = Behavior.valueOf(resultSet.getString("behavior").toUpperCase());
                person.version = resultSet.getInt("version");
            }
            return person;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void updatePerson(Person person) {
        String sql = """
       UPDATE people
       SET
          firstName = ?,
          lastName = ?,
          dateOfBirth = ?,
          latitude = ?,
          longitude = ?,
          behavior = ?,
          version = ?    
       WHERE id = ? AND version = ? - 1;
    """;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind values for the UPDATE query
            preparedStatement.setString(1, person.firstName);
            preparedStatement.setString(2, person.lastName);
            preparedStatement.setDate(3, Date.valueOf(person.dateOfBirth));

            // Handle location values (latitude and longitude)
            if (person.addressLocation != null) {
                preparedStatement.setDouble(4, person.addressLocation.getLatitude());
                preparedStatement.setDouble(5, person.addressLocation.getLongitude());
            } else {
                preparedStatement.setNull(4, Types.DOUBLE);
                preparedStatement.setNull(5, Types.DOUBLE);
            }
            preparedStatement.setObject(6, person.behavior.name().toLowerCase(), java.sql.Types.OTHER);
            preparedStatement.setInt(7, person.version);
            // this is tricky because in the create part this should not be defined hence optional
            // here however id will always be defined
            if (person.id.isEmpty()) {
                throw new IllegalArgumentException("Person ID must be provided for update.");
            }
            preparedStatement.setInt(8, person.id.get()); // Bind `id` for WHERE clause,

            preparedStatement.setInt(9, person.version); // Decrease the version in WHERE clause

            // Execute the UPDATE statement
            int rowsAffected = preparedStatement.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Person updated successfully.");
            } else {
                System.out.println("Update failed due to optimistic lock (version mismatch).");
                throw new RuntimeException("Update failed due to optimistic lock (version mismatch).");
            }
        } catch (SQLException e) {
            System.err.println("SQL error occurred: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Person getPerson(int id) {
        String sql = "SELECT id, firstName, lastName, dateOfBirth, timeOfRegistration, latitude, longitude, behavior, version FROM people WHERE id = ?";
        Person person = null;

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind the id value to the placeholder
            preparedStatement.setInt(1, id);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    String firstName = resultSet.getString("firstName");
                    String lastName = resultSet.getString("lastName");
                    LocalDate dateOfBirth = resultSet.getDate("dateOfBirth").toLocalDate();
                    LocalDateTime timeOfRegistration = resultSet.getTimestamp("timeOfRegistration").toLocalDateTime();

                    double latitude = resultSet.getDouble("latitude");
                    double longitude = resultSet.getDouble("longitude");
                    Location addressLocation = null;
                    if (!resultSet.wasNull()) {
                        addressLocation = new Location(latitude, longitude);
                    }
                    Behavior behavior = Behavior.valueOf(resultSet.getString("behavior").toUpperCase());
                    int version = resultSet.getInt("version");

                    person = new Person(Optional.of(id), firstName, lastName, dateOfBirth, timeOfRegistration, addressLocation, behavior, version);
                } else {
                    throw new RuntimeException("Person with ID " + id + " not found.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        return person;
    }
    public List<Person> getAllPeople() {
        String sql = "SELECT id, firstName, lastName, dateOfBirth, timeOfRegistration, latitude, longitude, behavior, version FROM people";
        List<Person> people = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String firstName = resultSet.getString("firstName");
                String lastName = resultSet.getString("lastName");
                LocalDate dateOfBirth = resultSet.getDate("dateOfBirth").toLocalDate();
                LocalDateTime timeOfRegistration = resultSet.getTimestamp("timeOfRegistration").toLocalDateTime();

                double latitude = resultSet.getDouble("latitude");
                double longitude = resultSet.getDouble("longitude");
                Location addressLocation = null;
                if (!resultSet.wasNull()) {
                    addressLocation = new Location(latitude, longitude);
                }
                Behavior behavior = Behavior.valueOf(resultSet.getString("behavior").toUpperCase());
                int version = resultSet.getInt("version");

                Person person = new Person(Optional.of(id), firstName, lastName, dateOfBirth, timeOfRegistration, addressLocation, behavior, version);

                // Add the person to the list
                people.add(person);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }

        return people;
    }

}