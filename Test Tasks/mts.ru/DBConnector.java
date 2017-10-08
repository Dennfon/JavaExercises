/**
 * Class DBConnector
 * The class provides work with the SQLite database:
 *  - table PRODUCT stores information about products
 *  - table LOG stores requests history
 *
 * @author Sergey Iryupin
 * @version 0.2.1 dated Oct 08, 2017
 */
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.*;

class DBConnector {
    static final String DRIVER_NAME = "org.sqlite.JDBC";
    static final String DB_FILE = "product.db";
    static Connection connect = null;

    public static int getPrice(String name) { // get price by name
        return getIntField(name, "price");
    }

    public static void sell(String name, int quantity) { // sell by name
        Integer price = getPrice(name);
        Integer balance = getAmount(name);
        Integer sales = getSales(name);
        Integer batch = getBatch(name);
        Integer change = getChange(name);
        if (balance != null && balance >= quantity) {
            try (Statement stmt = connect.createStatement()) {
                stmt.executeUpdate(
                    "UPDATE product SET amount=" + (balance - quantity) +
                    " WHERE name='" + name + "';");
                stmt.executeUpdate(
                    "UPDATE product SET sales=" + (sales + quantity) +
                    " WHERE name='" + name + "';");
            } catch (Exception e) {
                e.printStackTrace();
            }
            writeLog(
                name, "sell " + Integer.toString(quantity), "success");
            // change price if it need
            int rest = sales % batch; // get the rest of saled batches
            if ((rest + quantity) / batch > 0)
                setPrice(name, price + (rest + quantity) / batch * change);
        } else
            writeLog(
                name, "sell " + quantity, "failed");
    }

    public static void setPrice(String name, int price) { // set price by name
        openDB();
        try (Statement stmt = connect.createStatement()) {
            int i = stmt.executeUpdate(
                "UPDATE product SET price=" + price + " WHERE name='" + name + "';");
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeLog(
            name, "set price " + price, "success");
    }

    private static Integer getAmount(String name) { // get amount by name
        return getIntField(name, "amount");
    }

    private static Integer getSales(String name) { // get sales by name
        return getIntField(name, "sales");
    }

    private static Integer getBatch(String name) { // get batch volume by name
        return getIntField(name, "batch");
    }

    private static Integer getChange(String name) { // get change price by name
        return getIntField(name, "change");
    }

    public static void add(String name, int quantity) { // add quantity by name
        int amount = getAmount(name);
        openDB();
        try (Statement stmt = connect.createStatement()) {
            stmt.executeUpdate(
                "UPDATE product SET amount=" + (amount + quantity) +
                " WHERE name='" + name + "';");
        } catch (Exception e) {
            e.printStackTrace();
        }
        writeLog(
            name, "add " + quantity, "success");
    }

    private static void openDB() { // open and init DB if it's needed
        try {
            if (connect == null) {
                // open db
                Class.forName(DRIVER_NAME);
                connect = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
                // create tables
                Statement stmt = connect.createStatement();
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS product" +
                    "(name   CHAR(12) PRIMARY KEY NOT NULL," +
                    " price  INT NOT NULL," +
                    " amount INT," +
                    " sales  INT," +  // sold quantity
                    " batch  INT," +  // batch size for price change
                    " change INT);"); // price change
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS log" +
                    "(date   TEXT NOT NULL," +
                    " name   CHAR(12)," +
                    " action TEXT," +
                    " result CHAR(8));");
                // add two records if table is empty
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM product;");
                if (rs.getInt(1) == 0) {
                    stmt.executeUpdate("INSERT INTO product" +
                        "(name, price, amount, batch, change) " +
                        "VALUES ('apple', 15, 1000, 100, -1);");
                    stmt.executeUpdate("INSERT INTO product" +
                        "(name, price, amount, batch, change) " +
                        "VALUES ('wine', 250, 50, 20, 10);");
                }
                stmt.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Integer getIntField(String name, String field) { // get int field
        Integer result = null;
        openDB();
        try (Statement stmt = connect.createStatement()) {
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM product WHERE name='" + name + "';");
            while (rs.next())
                result = rs.getInt(field);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void writeLog(String name, String action, String result) {
        openDB();
        try (Statement stmt = connect.createStatement()) {
            stmt.executeUpdate("INSERT INTO log" +
                "(date, name, action, result) " +
                "VALUES ('" + (new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss:SSS").format(new Date())) + "', '" +
                name + "', '" + action + "', '" + result + "');");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}