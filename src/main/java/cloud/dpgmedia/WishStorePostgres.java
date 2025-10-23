package cloud.dpgmedia;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WishStorePostgres {
    public void storeWish(Wish wish) {
        // Placeholder for storing the wish in a PostgreSQL database
        System.out.println("Storing wish in PostgreSQL: " + wish);
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";

        String sql = "INSERT INTO wishes (id, productName, quantity, beneficiaryId) VALUES (?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind values to the placeholders
            preparedStatement.setString(1, wish.id);
            preparedStatement.setString(2, wish.productName);
            preparedStatement.setInt(3, wish.quantity);
            preparedStatement.setInt(4, wish.beneficiaryId);

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Rows inserted: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Wish> getAllWishes() {
        System.out.println("Getting all wishes from Postgres");
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";

        // string to get all wishes from postgress db of wishes table

        String sql = "SELECT * FROM wishes";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             Statement statement = connection.createStatement()) {

            // Execute the raw SQL INSERT query
            ResultSet wishes = statement.executeQuery(sql);
            List<Wish> wishesList = new ArrayList<>();
            //transofrm Resultset wishes to Wishes from domain model
            while (wishes.next()) {
                String id = wishes.getString("id");
                String productName = wishes.getString("productName");
                int quantity = wishes.getInt("quantity");
                int beneficiaryId = wishes.getInt("beneficiaryId");
                Wish wish = new Wish(id, productName, quantity, beneficiaryId);
                wishesList.add(wish);
                System.out.println("Wish from db: " + wish.id + " " + wish.productName + " " + wish.quantity);
            }

            return wishesList;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


    }

    public List<Wish> getWishesForBeneficiary(int beneficiaryId) {
        System.out.println("Getting wishes for beneficiaryId " + beneficiaryId + " from Postgres");
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";

        // string to get all wishes from postgress db of wishes table

        String sql = "SELECT * FROM wishes WHERE beneficiaryId = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind values to the placeholders
            preparedStatement.setInt(1, beneficiaryId);

            // Execute the raw SQL INSERT query
            ResultSet wishes = preparedStatement.executeQuery();
            List<Wish> wishesForBeneficiary = new ArrayList<>();
            //transform Resultset wishes to Wishes from domain model
            while (wishes.next()) {
                String id = wishes.getString("id");
                String productName = wishes.getString("productName");
                int quantity = wishes.getInt("quantity");
                int benId = wishes.getInt("beneficiaryId");
                Wish wish = new Wish(id, productName, quantity, benId);
                wishesForBeneficiary.add(wish);
                System.out.println("Wish from db: " + wish.id + " " + wish.productName + " " + wish.quantity);
            }

            return wishesForBeneficiary;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void deleteWish(String id) {
        // Placeholder for deleting the wish from a PostgreSQL database
        System.out.println("Deleting wish with id " + id + " from PostgreSQL");
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";

        String sql = "DELETE FROM wishes WHERE id = ?";

        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind the id value to the placeholder
            preparedStatement.setString(1, id);

            int rowsAffected = preparedStatement.executeUpdate();
            System.out.println("Rows deleted: " + rowsAffected);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public Optional<Wish> getWish(String id) {
        // get wish from db for id id
        System.out.println("Getting wish with id " + id + " from PostgreSQL");
        // Database connection details
        String url = "jdbc:postgresql://localhost:5432/webapp_db";
        String user = "geert";
        String password = "gman";
        String sql = "SELECT * FROM wishes WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(url, user, password);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

            // Bind the id value to the placeholder
            preparedStatement.setString(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                String productName = resultSet.getString("productName");
                int quantity = resultSet.getInt("quantity");
                int beneficiaryId = resultSet.getInt("beneficiaryId");
                Wish wish = new Wish(id, productName, quantity, beneficiaryId);
                System.out.println("Wish found: " + wish.id + " " + wish.productName + " " + wish.quantity);
                return Optional.of(wish);
            } else {
                System.out.println("No wish found with id: " + id);
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
