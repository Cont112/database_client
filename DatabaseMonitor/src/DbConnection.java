import java.sql.*;

public class DbConnection {
    private Connection con;
    private Statement stm;
    private Boolean con_status = false; //true for successful

    public Connection getCon() {
        return con;
    }

    public Boolean getCon_status() {
        return con_status;
    }

    public Statement getStm() {
        return stm;
    }
    public void closeConnection() {
        try {
            con.close();
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            System.out.println("Couldn't close connection");
        }
    }

    public void setCon(String con_type,String user, String password, String database) {
        String mysql_url = "jdbc:mysql://localhost/" + database;
        String pg_url = "jdbc:postgresql://localhost/" + database;

        try {
            String url;
            switch (con_type) {
                case "MySQL" -> url = mysql_url;
                case "PostgreSQL" -> url = pg_url;
                default -> url = "";
            }
            con = DriverManager.getConnection(url, user, password);
            stm = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            con_status = true;
        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
            con_status = false;

        }

    }
}
