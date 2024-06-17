import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class Main {
    private static String user;
    private static String password;
    private static String type;
    private static String database = "";
    private static boolean isRunning = false;
    private static boolean queryMode = false;
    private static int max = 1000;

    private static StringBuilder queryBuffer = new StringBuilder();

    private static DbConnection connection;

    public static void main(String[] args) {
        connection = new DbConnection();
        boolean got_type = false;
        while(!got_type){
            System.out.println("Select the connection type:");
            System.out.println("- Type '1' for MySQL connection.");
            System.out.println("- Type '2' for PostgreSQL connection.");
            System.out.print("Connection: ");
            String type_id = System.console().readLine().toLowerCase().trim();
            switch (type_id) {
                case "1":
                    type = "MySQL";
                    got_type = true;
                    break;
                case "2":
                    type = "PostgreSQL";
                    got_type = true;
                    break;
                default:
                    System.out.println("Please type a valid number.");
            }
        }
        while (!connection.getCon_status()) {

            System.out.print("Enter user: ");
            user = System.console().readLine();

            System.out.print("Enter password: ");
            password = System.console().readLine();

            connection.setCon(type,user, password, database);

            isRunning = true;
        }
        System.out.println("Connected to "+ type +" Monitor.");
        System.out.println("Type 'help' to see a list of all available commands.");

        while (isRunning) {
            if(queryMode){
                System.out.print(">");

            }else {
                System.out.print(type.toLowerCase()+">");
            }
            String input = System.console().readLine();

            manageCommand(input);
        }
        connection.closeConnection();
    }

    static void exit() {
        System.out.println("See you soon!");
        isRunning = false;
    }

    static void manageCommand(String input) {
        if(queryMode){
            handleQueryMode(input);
        } else {
            String[] Inputs = input.split(" ");
            String command = Inputs[0].toLowerCase();
            Inputs[0] = "";
            String argument = String.join(" ", Inputs).trim();
            switch (command) {
                case "exit" -> exit();
                case "use" -> changeDatabase(argument);
                case "query" -> queryExecute(input);
                case "help" -> help();
                case "info" -> showTableInformation(argument);
                case "tables" -> showTables();
                case "clear" -> clear();
                case "tree" -> tree();
                case "max" -> setMax(argument);

                default -> System.out.println("Invalid command. Type 'help' to see a list of commands.");
            }
        }
    }

    static void changeDatabase(String db) {
        connection.setCon(type,user, password, db);
        if (connection.getCon_status()) {
            System.out.println("Database changed to " + db + ".");
            database = db;
        }
    }

    static void queryExecute(String query) {
        if(database.isEmpty()){
            System.out.println("Select a database first!");
        } else {
            if (query.trim().equalsIgnoreCase("query")) {
                queryMode = true;
                System.out.println("Entered query mode, queries are terminated with ';'. Type 'end;' to execute all queries and exit.");
            } else {
                String s = query.split(" ")[0].toLowerCase();
                switch (s) {
                    case "select", "explain" -> queryExecuteTable(query);
                    case "insert", "update", "delete" -> queryExecuteUpdate(query);
                    default -> queryExecuteAll(query);
                }
            }
        }
    }
    static void queryExecuteAll(String query){
        try {
            if(query.isEmpty()){
                return;
            }else {
                connection.getStm().execute(query);
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    static void handleQueryMode(String input) {
        if (input.trim().equalsIgnoreCase("end;")) { // Sai do modo query
            queryMode = false;
            executeBufferedQueries();
            queryBuffer.setLength(0);
        } else {
            queryBuffer.append(input).append(" ");
        }
    }

    static void executeBufferedQueries() {
        String[] queries = queryBuffer.toString().split(";");
        for (String query : queries) {
            queryExecute(query.trim());
        }
    }

    static List<String> createDataList(ResultSet rs) throws  SQLException{
        ResultSetMetaData md = rs.getMetaData();
        int col = md.getColumnCount();

        List<String> data = new ArrayList<String>();

        for(int i = 1; i <= col; i++){
            data.add(md.getColumnName(i));
        }

        while(rs.next()) {
            for (int i = 1; i <= col; i++) {
                switch (md.getColumnType(i)) {
                    case Types.INTEGER, Types.DECIMAL -> {
                        int element = rs.getInt(i);
                        if (rs.wasNull()){
                            data.add("NULL");
                        } else{
                            data.add(String.valueOf(element));
                        }
                    }
                    case Types.VARCHAR -> {
                        String element = rs.getString(i);
                        if (rs.wasNull()){
                            data.add("NULL");
                        } else{
                            data.add(element);
                        }
                    }
                }

            }
        }
        return data;
    }

    static void queryExecuteTable(String sql) {
        try {
            ResultSet rs = connection.getStm().executeQuery(sql + " LIMIT " + max);
            ResultSetMetaData md = rs.getMetaData();
            int col = md.getColumnCount();
            List<String> data = createDataList(rs);
            Table table = new Table(data, col);
            table.printTable();
            System.out.println(data.size()/col - 1 + " rows in set (" + max + " max). Type 'max [rows]' to change the maximum number of rows displayed.");
            System.out.println("Would you like to save this query as a CSV file? Type Y or N to continue...");
            String response = System.console().readLine();
            if(response.equalsIgnoreCase("y")){
                System.out.print("Type the desired filename: ");
                String filename = System.console().readLine();
                saveTableCsv(table, filename);
            }



        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static void queryExecuteUpdate(String sql){
        try{
            int rows = connection.getStm().executeUpdate(sql);
            System.out.println("Query OK, " + rows + "row(s) affected.");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static void showTables(){
        try{
            if(database.isEmpty()){
                System.out.println("Select a database first!");
            }
            else {
                DatabaseMetaData md = connection.getCon().getMetaData();
                ResultSet rs = md.getTables(database, null, "%", null);

                List<String> data = new ArrayList<String>();

                data.add("Tables_in_" + database);
                while (rs.next()) {
                    String name = rs.getString(3);
                    data.add(name);
                }
                Table t = new Table(data, 1);
                t.printTable();
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    static void showTableInformation(String table){
        try{
            if(database.isEmpty()){
                System.out.println("Select a database first!");
            } else {
                DatabaseMetaData md = connection.getCon().getMetaData();
                ResultSet rs = md.getColumns(database, null, table, null);

                List<String> data = new ArrayList<String>();

                data.add("Field");
                data.add("Type");
                data.add("PK");

                List<String> pk = new ArrayList<String>();
                ResultSet pkrs = md.getPrimaryKeys(database, null, table);
                while (pkrs.next()) {
                    pk.add(pkrs.getString("column_name"));
                }

                while (rs.next()) {
                    String name = rs.getString(4);
                    int size = rs.getInt(7);
                    boolean p = pk.contains(name);

                    String type = rs.getString(6);

                    data.add(name);
                    data.add(type + "(" + String.valueOf(size) + ")");
                    if (p) {
                        data.add("TRUE");
                    } else {
                        data.add("FALSE");
                    }
                }

                Table t = new Table(data, 3);
                t.printTable();
            }
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    static void help() {
        System.out.println("    List of available commands:");
        System.out.println("    help                               -- Display this help message.");
        System.out.println("    exit                               -- Exit the monitor.");
        System.out.println("    query                              -- Enters query mode.");
        System.out.println("    use                                -- Change current database.");
        System.out.println("    tables                             -- Display tables in the current database.");
        System.out.println("    tree                               -- Display the current database's tables and views in a tree.");
        System.out.println("    info [table]                       -- Display information on specified table.");
        System.out.println("    max [nrows]                        -- Changes the maximum number of rows displayed (1000 for default).");
    }

    static void setMax(String m){
        max = Integer.parseInt(m);
        System.out.println("Maximum number of displayed rows changed to " + m + ".");
    }

    static void clear(){
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    static void tree(){
        try {

            if(database.isEmpty()){
                System.out.println("Select a database first! Type 'use [database]' to select a database.");
            } else {
            DatabaseMetaData md = connection.getCon().getMetaData();
            ResultSet tables = md.getTables(database, null, "%", new String[]{"TABLE", "VIEW"});

            System.out.println("Database: " + database);

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                String tableType = tables.getString("TABLE_TYPE");
                System.out.println("  ├── " + tableName + " (" + tableType + ")");

                ResultSet columns = md.getColumns(database, null, tableName, null);
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    System.out.println("  │   └── " + columnName);
                }
            }
            }
        } catch (SQLException e) {
            System.out.println("Erro ao obter informações do banco de dados: " + e.getMessage());
        }
    }

    static void saveTableCsv(Table t, String filename) {
        try (FileWriter writer = new FileWriter("./" + database + "_" + filename + ".csv")) {

            List<String> data = t.getData(); // Assuming Table has a getRows() method
            int col = t.getnColumns();
            String[] row = new String[col];
            int nrows = data.size()/col ;
            for(int i = 0; i < nrows; i++){
                for (int j = 0; j < col; j++) {
                    row[j] = data.get(i*col+j);
                }
                writer.write(String.join(",", row) + "\n"); // Comma-separate values
            }
            System.out.println("Saved to ./" + database + "_" + filename + ".csv");
        } catch (IOException e) {
            System.out.println("Error saving CSV: " + e.getMessage());
        }
    }
}