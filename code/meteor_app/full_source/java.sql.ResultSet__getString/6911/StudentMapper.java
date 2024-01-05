package scheduelp.model.mapping;

import java.sql.ResultSet;
import java.sql.SQLException;


import org.springframework.jdbc.core.RowMapper;

import scheduelp.model.Student;

public class StudentMapper implements RowMapper<Student> {

	@Override
	public Student mapRow(ResultSet rs, int rowNum) throws SQLException {
		Student student = new Student();
		student.setStudentID(rs.getString("student_id"));
		student.setFirstName(rs.getString("first_name"));
		student.setMiddleName(rs.getString("middle_name"));
		student.setLastName(rs.getString("last_name"));		
		student.setEmail(rs.getString("email"));
		student.setDegree(rs.getString("degree"));
		student.setDegreeDesc(rs.getString("degree_name"));
		
		return student;
	}

}
