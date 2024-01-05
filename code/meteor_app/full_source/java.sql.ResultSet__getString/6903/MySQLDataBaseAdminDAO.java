package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import settings.SQLSettings;

import model.User;

public class MySQLDataBaseAdminDAO implements DataBaseAdminDAO{

	public static final String projectTablesCreationFile = "ptablesCreation.sql";
	
	
	
	// PAS FINI ? 
	@Override
	public boolean createUser(User user) throws SQLException {
		if(SQLSettings.getPDS() == null) 
			System.err.println("getPDS() not started.");
		ResultSet rset = null;
		int rset2;
		Statement stmt = null;
		Connection connection = null;
		
		try {
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();
			String encryptedPass = null;		
	
			rset = stmt.executeQuery("select PASSWORD('"+user.getPassword()+"') ;");
			if (rset != null) {
				while(rset.next()){
					encryptedPass=rset.getString(1);
				}
				rset2 = stmt.executeUpdate("create user '"+user.getLogin()+"'@'%' IDENTIFIED BY PASSWORD '"+encryptedPass+"' ;");
				
				String[] viewCommand = Scripts.getCreateUserViews(user);
				// On cree les vues utilisateur et on donne les acces
				for(String curcom:viewCommand)
					rset2 = stmt.executeUpdate(curcom);
				
				// on insere le tuple dans user_view
				UserViewDAO uvdao = new MySQLUserViewDAO();
				uvdao.addUserView(user.getLogin(), user.getId());
				
				return true;
			}
			
			return false;
		
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}finally {
			try { if(rset!=null) rset.close();  } catch (Exception e) {};
			try { if(stmt!=null) stmt.close();  } catch (Exception e) {};
			try { if(connection!=null) connection.close();  } catch (Exception e) {};
		}
	}
	
	// PAS FINI ? 
	@Override
	public boolean removeUser(User user) throws SQLException {
		if(SQLSettings.getPDS() == null) 
			System.err.println("getPDS() not started.");
		int rset2;
		Statement stmt = null;
		Connection connection = null;
		
		try {
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();
	
			String[] viewCommand = Scripts.getDeleteUserViews(user);
			// On supprime les vues utilisateur et  acces
			for(String curcom:viewCommand)
				rset2 = stmt.executeUpdate(curcom);
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();
			stmt.execute("DROP USER "+user.getLogin()+" ;");
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();
			stmt.execute("DROP USER "+user.getLogin()+"@'' ;");

			// on enleve le tuple de user_view
			UserViewDAO uvdao = new MySQLUserViewDAO();
			uvdao.removeUser(user);
			
			return true;

		
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}finally {
			try { if(stmt!=null) stmt.close();  } catch (Exception e) {};
			try { if(connection!=null) connection.close();  } catch (Exception e) {};
		}
	}

	@Override
	public boolean exists(String databaseName) throws SQLException {
		Connection connection = null;
		ResultSet resultSet = null;
		try {
			connection = SQLSettings.getPDS().getConnection();
			resultSet = connection.getMetaData().getCatalogs();
	
			//iterate each catalog in the ResultSet
			while (resultSet.next()) {
			  // Get the database name, which is at position 1
			  String dbName = resultSet.getString(1);
			  if(dbName.equals(databaseName)){
				  return true;
			  }
			}
			return false;
		}catch(SQLException e){
			e.printStackTrace();
			throw e;
		}finally{
			resultSet.close();
		}
	}

	@Override
	public boolean createProject(String databaseName) throws SQLException {
		Statement stmt = null;
		Connection connection = null;
		
		try {
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();	
	
			stmt.executeUpdate("create database "+databaseName+" ;");

			return true;
		
		}catch(SQLException e){
			e.printStackTrace();	
			throw e;
		}finally {
			try { if(stmt!=null) stmt.close();  } catch (Exception e) {};
			try { if(connection!=null) connection.close();  } catch (Exception e) {};
		}
	}

	@Override
	public boolean setPasswordForCurrentUser(String pass) throws SQLException {
		Statement stmt = null;
		Connection connection = null;
		
		try {
			connection = SQLSettings.getPDS().getConnection();
			stmt = connection.createStatement();	
	
			stmt.executeUpdate("set password=password('"+pass+"') ;");

			return true;
		
		}catch(SQLException e){
			e.printStackTrace();	
			throw e;
		}finally {
			try { if(stmt!=null) stmt.close();  } catch (Exception e) {};
			try { if(connection!=null) connection.close();  } catch (Exception e) {};
		}
	}
	
	
}
