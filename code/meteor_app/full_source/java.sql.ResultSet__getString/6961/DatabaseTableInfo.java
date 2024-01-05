package com.xone.webtools.web;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DatabaseTableInfo {
	
	protected String driver;
	protected String url;
	protected String username;
	protected String password;
	protected Map<String, Map<String, String>> tableInfo = null;
	
	protected Connection connection = null;
	
	public DatabaseTableInfo(String driver, String url, String username, String password) {
		this.driver = driver;
		this.url = url;
		this.username = username;
		this.password = password;
		try {
			Class.forName(this.driver);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	protected Connection getConnection() throws Exception {
		if (null == connection || connection.isClosed()) {
			connection = DriverManager.getConnection(url, username, password); // MySQL
		}
		return connection;
	}
	
	public Map<String, Map<String, String>> getTableInfos() throws Exception {
		if (null == tableInfo || tableInfo.isEmpty()) {
			tableInfo = new HashMap<String, Map<String, String>>();
			PreparedStatement preparedStatement = getConnection()
					.prepareStatement("SELECT TABLE_NAME, COLUMN_NAME, COLUMN_COMMENT, COLUMN_KEY FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'sample'");
			ResultSet resultSet = preparedStatement.executeQuery();
			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				tableName = tableName.toUpperCase();
				String columnName = resultSet.getString("COLUMN_NAME");
				columnName = columnName.toUpperCase();
				String columnComment = resultSet.getString("COLUMN_COMMENT");
//				String columnKey = resultSet.getString("COLUMN_KEY");
				Map<String, String> info = tableInfo.get(tableName);
				if (null == info) {
					info = new HashMap<String, String>();
				}
				info.put(columnName, columnComment);
				tableInfo.put(tableName, info);
			}
		}
		return tableInfo;
	}
	
	public List<String> getTables(String [] t) throws Exception {
		List<String> tableList = getTables();
		if (null == t || t.length == 0) {
			return tableList;
		}
		List<String> mixTables = new ArrayList<String>();
		for (String s : t) {
			if (tableList.contains(s)) {
				mixTables.add(s);
			}
		}
		if (null == mixTables || mixTables.isEmpty()) {
			return tableList;
		}
		return mixTables;
	}
	
	public List<String> getTables() throws Exception {
		List<String> tables = new ArrayList<String>();
		DatabaseMetaData metaData = getConnection().getMetaData();
		ResultSet resultSet = metaData.getTables(null, null, null, null);
		int i = 1;
		while (resultSet.next()) {
			if (i == 1) {
				System.out.println("|库名：" + resultSet.getString(1));
				System.out.println("+----------------+");
			}
			String tableName = resultSet.getString("TABLE_NAME");
			System.out.println("|表" + (i++) + ":" + tableName);
			tables.add(tableName);
		}
		System.out.println("+----------------+");
		resultSet.close();
		return tables;
	}
	
	public List<Map<String, String>> getTableProperties(String tableName) throws Exception {
		return getTableProperties(tableName, new DefaultColumnClassNameConvertor());
	}
	
	public List<Map<String, String>> getTableProperties(String tableName, ColumnClassNameConvertor columnClassNameConvertor) throws Exception {
		PreparedStatement preparedStatement = getConnection()
				.prepareStatement("select * from " + tableName + " where 1=2");
		ResultSetMetaData resultSetMetaData = preparedStatement.executeQuery()
				.getMetaData();
		Map<String, String> columnInfo = getTableInfos().get(tableName.toUpperCase());
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		for (int i = 0; i < resultSetMetaData.getColumnCount(); i++) {
			Map<String, String> map = new HashMap<String, String>();
			String columnName = resultSetMetaData.getColumnName(i + 1);
			String columnTypeName = resultSetMetaData.getColumnTypeName(i + 1);
			int columnDisplaySize = resultSetMetaData.getColumnDisplaySize(i + 1);
			String columnClassName = resultSetMetaData.getColumnClassName(i + 1);
			if (null != columnClassNameConvertor) {
				columnClassName = columnClassNameConvertor.convertor(columnClassName);
			}
			map.put("columnClassName", columnClassName);
			map.put("columnTypeName", columnTypeName);
			map.put("columnName", columnName);
			map.put("columnDisplaySize", String.valueOf(columnDisplaySize));
			// fix bug comments too long
			String comments = columnInfo.get(columnName.toUpperCase());
			if(!StringUtils.isBlank(comments)){
			    int kuohaoIndex = comments.indexOf("(");
			    if(kuohaoIndex > -1){
			        comments = comments.substring(0, kuohaoIndex);
			    }
			}else{
			    comments = "";
			}
			map.put("columnComments", comments);
			if (resultSetMetaData.isAutoIncrement(i + 1)) {
				map.put("autoIncrement", "1");
			} else {
				map.put("autoIncrement", "0");
			}
			result.add(map);
		}
		preparedStatement.close();
		return result;
	}
	
	public void close() {
		try {
			if (null != this.connection) {
				this.connection.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public interface ColumnClassNameConvertor {
		public String convertor(String columnClassName);
	}
	
	public class DefaultColumnClassNameConvertor implements ColumnClassNameConvertor {

		@Override
		public String convertor(String columnClassName) {
			if ("java.sql.Timestamp".equals(columnClassName)) {
				columnClassName = "java.util.Date";
			}
			return columnClassName;
		}
		
	}
	
}
