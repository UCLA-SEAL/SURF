package com.epam.lab.intouch.dao.project;

import static com.epam.lab.intouch.util.db.metadata.FieldName.COMPLETED;
import static com.epam.lab.intouch.util.db.metadata.FieldName.CREATED;
import static com.epam.lab.intouch.util.db.metadata.FieldName.CUSTOMER;
import static com.epam.lab.intouch.util.db.metadata.FieldName.DESCRIPTION;
import static com.epam.lab.intouch.util.db.metadata.FieldName.ESTIMATED_COMPLETION;
import static com.epam.lab.intouch.util.db.metadata.FieldName.ID;
import static com.epam.lab.intouch.util.db.metadata.FieldName.NAME;
import static com.epam.lab.intouch.util.db.metadata.FieldName.STATUS;
import static com.epam.lab.intouch.util.db.metadata.TableName.PROJECT;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.epam.lab.intouch.dao.AbstractBaseDAO;
import com.epam.lab.intouch.dao.exception.DAOCreateException;
import com.epam.lab.intouch.dao.exception.DAODeleteException;
import com.epam.lab.intouch.dao.exception.DAOReadException;
import com.epam.lab.intouch.dao.exception.DAOUpdateException;
import com.epam.lab.intouch.dao.exception.DBConnectionException;
import com.epam.lab.intouch.model.project.Project;
import com.epam.lab.intouch.model.project.enums.ProjectStatus;

/**
 * Class for manipulating project data in DB 
 * 
 * @author Molodec
 *
 */
public class DefaultProjectDAO extends AbstractBaseDAO<Project, Long> implements ProjectDAO {

	private final static Logger LOG = LogManager.getLogger(DefaultProjectDAO.class);
	
	/** 
	 * This method is create project in DB
	 * @see com.epam.lab.intouch.dao.BaseDAO#create(java.lang.Object)
	 * @param project
	 * @throws DAOCreateException 
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 * @return id project in DB
	 */
	@Override
	public Long create(Project project) throws DAOCreateException {

		StringBuilder queryInsert = new StringBuilder();
		queryInsert.append("INSERT INTO ").append(PROJECT).append(" (");
		queryInsert.append(NAME).append(", ");
		queryInsert.append(CREATED).append(", ").append(ESTIMATED_COMPLETION).append(", ");
		queryInsert.append(COMPLETED).append(", ").append(DESCRIPTION).append(", ");
		queryInsert.append(CUSTOMER).append(", ").append(STATUS).append(") ");
		queryInsert.append("VALUES (?,?,?,?,?,?,?)");

		try (Connection connection = getConnection(); 
				PreparedStatement statement = connection.prepareStatement(queryInsert.toString(), Statement.RETURN_GENERATED_KEYS);
				) {
			
			statement.setString(1, project.getProjectName());
			statement.setDate(2, getCreationDate(project));
			statement.setDate(3, getEstimatedDate(project));
			statement.setNull(4, Types.DATE);
			
			statement.setString(5, project.getDescription());
			statement.setString(6, project.getCustomer());
			if (project.getStatus() != null){
				statement.setString(7, project.getStatus().toString());
				
			} else {
				statement.setNull(7, Types.NVARCHAR);
			}

			statement.executeUpdate();
			project.setId(getId(statement));

		} catch (SQLException e) {
			LOG.error("Problem with create project", e);
			throw new DAOCreateException("Problew with create project" + e.getMessage());
		} catch (DBConnectionException e) {
			LOG.error("Connection exception", e);
			throw new DAOCreateException("Connection exception" + e.getMessage());

		}

		return project.getId();

	}
	
	/**
	 * @param statement
	 * @return last generated id project from DB
	 * @throws SQLException
	 */
	private Long getId(PreparedStatement statement) throws SQLException{
		
		ResultSet autoIncKey = statement.getGeneratedKeys();
		
		Long projectID = null;
		while (autoIncKey.next()) {
			projectID = autoIncKey.getLong(1);
		}
		return projectID;
	}
	
	/**
	 * Method for check creation project date on NULL
	 * @param project
	 * @return creationDate
	 */
	private Date getCreationDate(Project project){
		Date creationDate = null;
		if (project.getCreationDate() != null){
			return creationDate = new Date(project.getCreationDate().getTime());
		}
		
		return creationDate;
	}
	
	/**
	 * Method for check estimated project date on NULL
	 * @param project
	 * @return estimatedDate
	 */
	private Date getEstimatedDate(Project project){
		Date estimatedDate = null;
		if (project.getEstimatedCompletionDate() != null){
			return estimatedDate = new Date(project.getEstimatedCompletionDate().getTime());
		}
		
		return estimatedDate;
	}
	
	/**
	 * Method for check completed project date on NULL
	 * @param project
	 * @return completedDate
	 */
	private Date getCompletedDate(Project project){
		Date completedDate = null;
		if (project.getCompletionDate() != null){
			return completedDate = new Date(project.getCompletionDate().getTime());
		}
		return completedDate;
	}
	
	

	/** 
	 * This method is get project by id from DB
	 * @see com.epam.lab.intouch.dao.BaseDAO#getById(java.lang.Object)
	 * @param id
	 * @throws DAOReadException 
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 * @return Project
	 */
	@Override
	public Project getById(Long id) throws DAOReadException {
		
		StringBuilder queryById = new StringBuilder();
		queryById.append("SELECT * FROM ").append(PROJECT).append(" WHERE ").append(ID).append("=?");
		
		Project project = null;
		try (Connection connection = getConnection();
				PreparedStatement statement = connection.prepareStatement(queryById.toString())){
			
			statement.setLong(1, id);
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				project = new Project();
				project.setId(result.getLong(ID));
				project.setProjectName(result.getString(NAME));
				project.setCreationDate(result.getDate(CREATED));
				project.setEstimatedCompletionDate(result.getDate(ESTIMATED_COMPLETION));
				project.setCompletionDate(result.getDate(COMPLETED));
				project.setCustomer(result.getString(CUSTOMER));
				project.setDescription(result.getString(DESCRIPTION));
				project.setStatus(ProjectStatus.fromString(result.getString(STATUS)));

			}

		} catch (SQLException e) {
			LOG.error("Exception with read Project by ID", e);
			throw new DAOReadException("Exception with read Project by ID" + e);
		} catch (DBConnectionException e) {
			LOG.error("Connection exception" + e.getMessage());
			throw new DAOReadException("Connection exception" + e.getMessage());
		}

		return project;
	}
	
	/**
	 * Method for update project with old value on new value in DB
	 * @see com.epam.lab.intouch.dao.BaseDAO#update(java.lang.Object, java.lang.Object)
	 * @param oldProject project with old value
	 * @param newProject project with new value
	 * @throws DAOUpdateException 
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 */
	@Override
	public void update(Project oldProject, Project newProject) throws DAOUpdateException {

		StringBuilder queryUpdate = new StringBuilder();
		queryUpdate.append("UPDATE ").append(PROJECT).append(" SET ");
		queryUpdate.append(NAME).append("= ?, ");
	
		queryUpdate.append(ESTIMATED_COMPLETION).append("= ?, ");
		queryUpdate.append(COMPLETED).append("= ?, ");
		queryUpdate.append(DESCRIPTION).append("= ?, ");
		queryUpdate.append(CUSTOMER).append("= ?, ");
		queryUpdate.append(STATUS).append("= ?");
		queryUpdate.append(" WHERE ").append(ID).append("=?");

		try (Connection connection = getConnection(); 
				PreparedStatement statement = connection.prepareStatement(queryUpdate.toString())) {
			statement.setString(1, newProject.getProjectName());
			if(getEstimatedDate(newProject) != null){
				statement.setDate(2, getEstimatedDate(newProject));
			}else {
				statement.setNull(2, Types.DATE);
			}
			
			if (getCompletedDate(newProject) != null){
				statement.setDate(3, getCompletedDate(newProject));
			}else {
				statement.setNull(3, Types.DATE);
			}
			statement.setString(4, newProject.getDescription());
			statement.setString(5, newProject.getCustomer());
			if (newProject.getStatus() != null){
				statement.setString(6, newProject.getStatus().toString());
			} else {
				statement.setNull(6, Types.NVARCHAR);
			}
			statement.setLong(7, oldProject.getId());
			statement.executeUpdate();

		} catch (SQLException e) {
			LOG.error("Problem with update project ", e);
			throw new DAOUpdateException("Problem with update project " + e.getMessage());
		} catch (DBConnectionException e) {
			LOG.error("Problem with conection ", e);
			throw new DAOUpdateException("Problem with conection " + e.getMessage());
		}

	}
	
	/**
	 * Method for delete project in DB
	 * @param project 
	 * @throws DAODeleteException
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 * @see com.epam.lab.intouch.dao.BaseDAO#delete(java.lang.Object)
	 */
	@Override
	public void delete(Project project) throws DAODeleteException {
		
		StringBuilder queryDelete = new StringBuilder();
		queryDelete.append("DELETE FROM ").append(PROJECT).append(" WHERE ").append(ID).append("=?");

		try (Connection connection = getConnection(); 
				PreparedStatement statement = connection.prepareStatement(queryDelete.toString())) {
			statement.setLong(1, project.getId());
			statement.executeUpdate();

		} catch (SQLException e) {
			LOG.error("Problem with delete project ", e);
			throw new DAODeleteException("Problem with delete project " + e.getMessage());
		} catch (DBConnectionException e) {
			LOG.error("Problem with conection ", e);
			throw new DAODeleteException("Problem with conection " + e.getMessage());
		}

	}
	
	/**
	 * Method for getting all project from DB 
	 * @see com.epam.lab.intouch.dao.BaseDAO#getAll()
	 * @return List<Project>  
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 * @throws DAOReadException
	 */
	@Override
	public List<Project> getAll() throws DAOReadException {

		StringBuilder queryAll = new StringBuilder();
		queryAll.append("SELECT * FROM ").append(PROJECT);
		
		List<Project> projects = new ArrayList<Project>();

		try (Connection connection = getConnection(); 
				Statement statement = connection.createStatement(); 
				ResultSet result = statement.executeQuery(queryAll.toString())) {

			while (result.next()) {
				Project project = new Project();
				project.setId(result.getLong(ID));
				project.setProjectName(result.getString(NAME));
				project.setCreationDate(result.getDate(CREATED));
				project.setEstimatedCompletionDate(result.getDate(ESTIMATED_COMPLETION));
				project.setCompletionDate(result.getDate(COMPLETED));
				project.setCustomer(result.getString(CUSTOMER));
				project.setDescription(result.getString(DESCRIPTION));
				project.setStatus(ProjectStatus.fromString(result.getString(STATUS)));

				projects.add(project);

			}

		} catch (SQLException e) {
			LOG.error("Problem with getting all Projects", e);
			throw new DAOReadException("Problem with getting all Projects" + e.getMessage());
		} catch (DBConnectionException e) {
			LOG.error("Connection exception" + e.getMessage());
			throw new DAOReadException("Connection exception" + e.getMessage());
		}

		return projects;
	}
	
	/**
	 * Method for getting all project from DB who match the query
	 * @see com.epam.lab.intouch.dao.BaseDAO#getAll()
	 * @param query This is query to DB
	 * @return List<Project> 
	 * @throws DAOReadException
	 * @exception SQLException if problem in SQL query or other
	 * @exception DBConnectionException if problem with connection
	 */
	@Override
	public List<Project> getAllFromSearch(String query) throws DAOReadException {
		
		List<Project> projects = new ArrayList<Project>();

		try (Connection connection = getConnection(); 
				Statement statement = connection.createStatement(); 
				ResultSet result = statement.executeQuery(query)) {

			while (result.next()) {
				Project project = new Project();
				project.setId(result.getLong(ID));
				project.setProjectName(result.getString(NAME));
				project.setCreationDate(result.getDate(CREATED));
				project.setEstimatedCompletionDate(result.getDate(ESTIMATED_COMPLETION));
				project.setCompletionDate(result.getDate(COMPLETED));
				project.setCustomer(result.getString(CUSTOMER));
				project.setDescription(result.getString(DESCRIPTION));
				project.setStatus(ProjectStatus.fromString(result.getString(STATUS)));

				projects.add(project);

			}

		} catch (SQLException e) {
			LOG.error("Problem with getting all Projects", e);
			throw new DAOReadException("Problem with getting all Projects" + e.getMessage());
		} catch (DBConnectionException e) {
			LOG.error("Connection exception" + e.getMessage());
			throw new DAOReadException("Connection exception" + e.getMessage());
		}

		return projects;
	}

}
