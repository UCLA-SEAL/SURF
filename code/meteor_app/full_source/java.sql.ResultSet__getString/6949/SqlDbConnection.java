package ms.utils.server.database;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SqlDbConnection {

	private Connection connection;
	private String database = "jdbc:sqlite:db/db.sqlite";

	public void setDatabase(String jdbcName) {
		database = jdbcName;
	}

	private Connection getConnection() throws SQLException {
		try {
			if (connection == null || connection.isClosed()) {
				Class.forName("org.sqlite.JDBC");
				connection = DriverManager.getConnection(database);
			}
		} catch (ClassNotFoundException e) {
			System.err.println("JDBC Driver nof found");
			e.printStackTrace();
		}
		return connection;
	}

	/**
	 * execute an insert statement.
	 */
	public int executeInsert(String sql) throws SQLException {
		int newId = -1;
		Statement stat = getConnection().createStatement();
		stat.execute(sql);
		newId = getIdOfInsertedElement(stat);
		stat.close();
		return newId;
	}

	public int executeInsert(String prepStmt, String... arguments)
			throws SQLException {
		int newId = -1;
		Connection conn = getConnection();
		PreparedStatement prep = createStatementWithArguments(prepStmt, conn,
				arguments);
		prep.execute();
		Statement stat = conn.createStatement();
		newId = getIdOfInsertedElement(stat);
		stat.close();
		return newId;
	}

	public void execute(String sql) throws SQLException {
		Connection conn = getConnection();
		Statement stat = conn.createStatement();
		stat.execute(sql);
		stat.close();
	}

	public void execute(String prepStmt, String... arguments)
			throws SQLException {
		Connection conn = getConnection();
		PreparedStatement prep = createStatementWithArguments(prepStmt, conn,
				arguments);
		prep.execute();
		prep.close();
	}

	/**
	 * Execute the query sql and return the, resulting as a
	 * {@link List} of type returnedClass.<br>
	 * 
	 * @param returnedClass
	 *            : <b>MUST</b> implement a constructor with a {@link ResultSet}
	 *            as the only parameter.
	 */
	public <T> ArrayList<T> getObjectList(String sql, Class<T> returnedClass) {
		ArrayList<T> list = new ArrayList<T>();
		try {
			Connection conn = getConnection();
			Statement stat = conn.createStatement();
			ResultSet res = stat.executeQuery(sql);
			while (res.next()) {
				Constructor<T> ctor = returnedClass
						.getConstructor(ResultSet.class);
				T object = ctor.newInstance(res);
				list.add(object);
			}
			stat.close();
		} catch (NoSuchMethodException me) {
			System.err.println("The class :" + returnedClass.getName()
					+ " has no Constructor for ResultSet");
			me.printStackTrace();
		} catch (Exception e) {
			System.out.println("Error while creating class out of a RowSet\n"
					+ e);
			e.printStackTrace();
		}
		return list;
	}

	public ArrayList<String> getStringList(String sql) {
		ArrayList<String> resultlist = new ArrayList<String>();
		Connection conn;
		try {
			conn = getConnection();
			Statement stat = conn.createStatement();
			ResultSet res = stat.executeQuery(sql);
			while (res.next()) {
				resultlist.add(res.getString(1));
			}
			stat.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return resultlist;
	}

	private int getIdOfInsertedElement(Statement stat)
			throws SQLException {
		int newId = -1;
		ResultSet res = stat.executeQuery("SELECT last_insert_rowid();");
		if (res.next())
			newId = res.getInt("last_insert_rowid()");
		return newId;
	}

	private PreparedStatement createStatementWithArguments(
			String prepStmt, Connection conn, String... arguments)
			throws SQLException {
		PreparedStatement prep = conn.prepareStatement(prepStmt);

		for (int i = 0; i < arguments.length; ++i) {
			prep.setString(i + 1, arguments[i]);
		}
		return prep;
	}
}
