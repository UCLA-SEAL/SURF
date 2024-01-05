package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import utility.ReportLogger;

public class AbstractDao {
	protected Connection connection;
	protected String currentTableName;
	protected PreparedStatement preparedStatementBatchUpdate;
	protected Logger logger = ReportLogger.getLog("general");

	protected void createPreparedStatementByUpdate(String sql)throws Exception{
		preparedStatementBatchUpdate = connection.prepareStatement(sql.trim());
	}

	public void closePreparedStatementByUpdate()throws Exception{
		if(preparedStatementBatchUpdate!=null){
			preparedStatementBatchUpdate.close();
		}
	}

	public void executeBatchByUpdate()throws Exception{
		if(preparedStatementBatchUpdate!=null){
			preparedStatementBatchUpdate.executeBatch();
		}
	}	

	public void setConnection(Connection con){connection = con;}
	
	public String getCurrentTableName(){return currentTableName;}
	
/*	protected synchronized String getId(Connection connection, String tableName) throws Exception{
		String id = "";
		if(connection.getAutoCommit()) throw new Exception("Settare autoCommit a FALSE");
		Statement s = null;
		ResultSet rs = null;
		String sql = "";
		try{
			sql = "INSERT INTO " + tableName + " (ID) VALUES (NULL)";
			s = connection.createStatement();
			s.executeUpdate(sql);
			
			sql = "SELECT MAX(ID) FROM " + tableName;
			s = connection.createStatement();
			rs = s.executeQuery(sql);
			if(rs.next()) id = rs.getString(1);
			
		}catch (Exception e) {
			throw e;
		}finally{
			if(rs!=null) rs.close();
			if(s!=null) s.close();
		}

		
		return id;
	}
*/
	protected String getId(Connection connection, String tableName) throws Exception{
		String id = "";
		Statement s = null;
		ResultSet rs = null;
		String sql = "";
		try{
			sql = "INSERT INTO " + tableName + " (ID) VALUES (NULL)";
			s = connection.createStatement();
			int i = s.executeUpdate(sql,Statement.RETURN_GENERATED_KEYS);
			
			rs = s.getGeneratedKeys();
			if(rs.next()) id = rs.getString(1);
			
		}catch (Exception e) {
			throw e;
		}finally{
			if(rs!=null) rs.close();
			if(s!=null) s.close();
		}

		
		return id;
	}

/*	protected synchronized String getIdInsert(Connection connection, String tableName) throws Exception{
		String id = "";
		if(connection.getAutoCommit()) throw new Exception("Settare autoCommit a FALSE");
		Statement s = null;
		ResultSet rs = null;
		String sql = "";
		try{
			
			sql = "SELECT MAX(ID) FROM " + tableName;
			s = connection.createStatement();
			rs = s.executeQuery(sql);
			if(rs.next()) id = rs.getString(1);
			
		}catch (Exception e) {
			throw e;
		}finally{
			if(rs!=null) rs.close();
			if(s!=null) s.close();
		}

		
		return id;
	}
*/}
