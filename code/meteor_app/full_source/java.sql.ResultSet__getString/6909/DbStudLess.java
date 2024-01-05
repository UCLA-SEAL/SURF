package kg.cloud.uims.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import kg.cloud.uims.domain.StudLess;

public class DbStudLess extends BaseDb {
	public ArrayList<StudLess> q;

	public DbStudLess() throws Exception {
		super();
	}

	public void execSQL(String std_id, String semID, String yearID)
			throws SQLException {

		String sql = "select t0.id,t1.id,t1.name,t1.surname,t6.name,t2.name,t2.id,t2.code,t2.stdyear,t2.hours,t2.credit,t3.id,"
				+ "t3.name,t4.year,t5.semester,t0.status, t7.name, t1.dept_id from less_stud as t0 "
				+ "left join student as t1 on t0.student_id=t1.id "
				+ "left join department as t6 on t1.dept_id=t6.id "
				+ "left join subjects as t2 on t0.subject_id=t2.id "
				+ "left join sinif as t3 on t1.group_id=t3.id "
				+ "left join year as t4 on t0.year_id=t4.id "
				+ "left join semester as t5 on t0.sem_id=t5.id "
				+ "left join faculty as t7 on t6.faculty_id=t7.id "
				+ "where t1.id=? and t5.id =? and t4.id=? order by t1.name,t1.surname asc;";

		q = new ArrayList<StudLess>();

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, std_id);
		stat.setString(2, semID);
		stat.setString(3, yearID);
		ResultSet result = stat.executeQuery();
		while (result.next()) {
			q.add(new StudLess(result.getInt("t0.id"), result.getInt("t1.id"),
					result.getInt("t2.id"), result.getInt("t3.id"), result
							.getInt("t0.status"), result.getString("t1.name"),
					result.getString("t1.surname"),
					result.getString("t6.name"), result.getString("t2.name"),
					result.getString("t2.code"), result.getString("t3.name"),
					result.getString("t4.year"),
					result.getString("t2.stdyear"), result
							.getString("t5.semester"), result
							.getString("t2.hours"), result
							.getString("t2.credit"), result
							.getString("t7.name"), result.getInt("t1.dept_id")));
		}
	}

	public void execSQL_Subject(String subj_id, String yearID, String semID)
			throws SQLException {

		String sql = "select t0.id,t1.id,t1.name,t1.surname,t6.name,t2.name,t2.id,"
				+ "t2.code,t2.stdyear,t2.hours,t2.credit,t3.id,"
				+ "t3.name,t4.year,t5.semester,t0.status,t7.name, t1.dept_id from less_stud as t0 "
				+ "left join student as t1 on t0.student_id=t1.id "
				+ "left join department as t6 on t1.dept_id=t6.id "
				+ "left join subjects as t2 on t0.subject_id=t2.id "
				+ "left join sinif as t3 on t1.group_id=t3.id "
				+ "left join year as t4 on t0.year_id=t4.id "
				+ "left join semester as t5 on t0.sem_id=t5.id "
				+ "left join faculty as t7 on t6.faculty_id=t7.id "
				+ "where t2.id=? and t5.id =? and t4.id=? and t0.status > 0 and t0.status < 3 "
				+ "and t1.edu_status_id=1 order by t1.name,t1.surname asc;";

		q = new ArrayList<StudLess>();

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, subj_id);
		stat.setString(2, semID);
		stat.setString(3, yearID);
		ResultSet result = stat.executeQuery();
		while (result.next()) {
			q.add(new StudLess(result.getInt("t0.id"), result.getInt("t1.id"),
					result.getInt("t2.id"), result.getInt("t3.id"), result
							.getInt("t0.status"), result.getString("t1.name"),
					result.getString("t1.surname"),
					result.getString("t6.name"), result.getString("t2.name"),
					result.getString("t2.code"), result.getString("t3.name"),
					result.getString("t4.year"),
					result.getString("t2.stdyear"), result
							.getString("t5.semester"), result
							.getString("t2.hours"), result
							.getString("t2.credit"), result
							.getString("t7.name"), result.getInt("t1.dept_id")));
		}
	}

	public void execSQL_Exam(String subj_id, String yearID, String semID)
			throws SQLException {

		String sql = "select t0.id,t1.id,t1.name,t1.surname, t1.rollnum, "
				+ "t6.name,t2.name,t2.id,t2.code,t2.stdyear,t2.hours,t2.credit,t3.id,"
				+ "t3.name,t4.year,t5.semester,t0.status,t7.name, t1.dept_id "
				+ "from less_stud as t0 "
				+ "left join student as t1 on t0.student_id=t1.id "
				+ "left join department as t6 on t1.dept_id=t6.id "
				+ "left join subjects as t2 on t0.subject_id=t2.id "
				+ "left join sinif as t3 on t1.group_id=t3.id "
				+ "left join year as t4 on t0.year_id=t4.id "
				+ "left join semester as t5 on t0.sem_id=t5.id "
				+ "left join faculty as t7 on t6.faculty_id=t7.id "
				+ "where t2.id=? and t5.id =? and t4.id=? "
				+ "and t0.status > 0 and t0.status < 4 and t1.edu_status_id=1 order by t1.name,t1.surname asc;";

		q = new ArrayList<StudLess>();

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, subj_id);
		stat.setString(2, semID);
		stat.setString(3, yearID);
		ResultSet result = stat.executeQuery();
		while (result.next()) {
			q.add(new StudLess(result.getInt("t0.id"), result.getInt("t1.id"),
					result.getInt("t2.id"), result.getInt("t3.id"), result
							.getInt("t0.status"), result.getString("t1.name"),
					result.getString("t1.surname"),
					result.getString("t6.name"), result.getString("t2.name"),
					result.getString("t2.code"), result.getString("t3.name"),
					result.getString("t4.year"),
					result.getString("t2.stdyear"), result
							.getString("t5.semester"), result
							.getString("t2.hours"), result
							.getString("t2.credit"), result
							.getString("t7.name"), result.getInt("t1.dept_id"),
					result.getString("t1.rollnum")));
		}
	}

	public void execSQL_Status(String subj_id, String yearID, String semID)
			throws SQLException {

		String sql = "select t0.id,t1.id,t1.name,t1.surname,t6.name,t2.name,t2.id,"
				+ "t2.code,t2.stdyear,t2.hours,t2.credit,t3.id,"
				+ "t3.name,t4.year,t5.semester,t0.status,t7.name, t1.dept_id from less_stud as t0 "
				+ "left join student as t1 on t0.student_id=t1.id "
				+ "left join department as t6 on t1.dept_id=t6.id "
				+ "left join subjects as t2 on t0.subject_id=t2.id "
				+ "left join sinif as t3 on t1.group_id=t3.id "
				+ "left join year as t4 on t0.year_id=t4.id "
				+ "left join semester as t5 on t0.sem_id=t5.id "
				+ "left join faculty as t7 on t6.faculty_id=t7.id "
				+ "where t2.id=? and t5.id =? and t4.id=? and t1.edu_status_id=1 order by t1.name,t1.surname asc;";

		q = new ArrayList<StudLess>();

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, subj_id);
		stat.setString(2, semID);
		stat.setString(3, yearID);
		ResultSet result = stat.executeQuery();
		while (result.next()) {
			q.add(new StudLess(result.getInt("t0.id"), result.getInt("t1.id"),
					result.getInt("t2.id"), result.getInt("t3.id"), result
							.getInt("t0.status"), result.getString("t1.name"),
					result.getString("t1.surname"),
					result.getString("t6.name"), result.getString("t2.name"),
					result.getString("t2.code"), result.getString("t3.name"),
					result.getString("t4.year"),
					result.getString("t2.stdyear"), result
							.getString("t5.semester"), result
							.getString("t2.hours"), result
							.getString("t2.credit"), result
							.getString("t7.name"), result.getInt("t1.dept_id")));
		}
	}

	public void execRegistration(String studID, String subjID, String status,
			String year, String sem) throws Exception {
		String sql = "insert ignore into less_stud values('',?,?,?,?,?);";

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, studID);
		stat.setString(2, subjID);
		stat.setString(3, status);
		stat.setString(4, year);
		stat.setString(5, sem);
		stat.executeUpdate();

	}

	public void execSubjectsTranscript(String sid) throws SQLException {

		String sql = "select t1.id, t1.subject_id, t1.year_id, t1.sem_id, t2.code, t2.name, t2.credit,"
				+ " t5.year, t6.semester"
				+ " from less_stud as t1 left join subjects as t2 on t1.subject_id=t2.id"
				+ " left join year as t5 on t1.year_id=t5.id"
				+ " left join semester as t6 on t1.sem_id=t6.id"
				+ " where t1.student_id=? and t1.status<4 order by t1.year_id,t1.sem_id;";

		q = new ArrayList<StudLess>();

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, sid);
		ResultSet result = stat.executeQuery();
		while (result.next()) {

			q.add(new StudLess(result.getInt("t1.id"), result
					.getInt("t1.subject_id"), result.getString("t1.year_id"),
					result.getString("t1.sem_id"), result.getString("t2.code"),
					result.getString("t2.name"), result.getString("t2.credit"),
					result.getString("t5.year"), result
							.getString("t6.semester"), Long.toString(Math
							.round(calcAverage_Transcript(
									result.getString("t1.id"),
									result.getString("t1.subject_id"))))));
		}
	}

	public double calcAverage_Transcript(String s_less_id, String subj_id)
			throws SQLException {
		String sql = "select t3.exam_name as exam,(t4.mark * t3.percentage/100) as average from less_stud "
				+ " as t5 left join student as  t1 on t5.student_id=t1.id left join "
				+ " subjects as t2 on t5.subject_id=t2.id left join sinif as t6 on "
				+ " t1.group_id=t6.id  left join subj_exam as t4 on t5.id = t4.stud_less_id "
				+ " left join exam as t3 on t4.exam_id=t3.exam_id "
				+ " where t2.id=? and t4.stud_less_id =? ;";

		double average = 0;
		double midterm = 0, fin = 0, mup = 0.0;
		int colCount = 0;

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setString(1, subj_id);
		stat.setString(2, s_less_id);
		ResultSet result = stat.executeQuery();
		while (result.next()) {
			String exam = result.getString("exam");
			if (exam.equals("Midterm")) {
				midterm = result.getDouble("average");
			} else if (exam.equals("Final")) {
				fin = result.getDouble("average");
				if (fin < 29.7) {
					fin = 0.0;
				}
			} else if (exam.equals("MakeUp")) {
				mup = result.getDouble("average");
				if (mup < 29.7) {
					mup = 0.0;
				}
			}
			if ((midterm + fin) < 49.5) {
				average = midterm + mup;
			} else {
				average = midterm + fin;
			}
			colCount++;
		}
		if (colCount < 2) {
			average = 101;
		}
		return average;
	}

	public double calcAverage_Report(String s_id, String sem, String year,
			int subj_id) throws SQLException {

		String sql = "select t3.exam_name as exam,(t4.mark * t3.percentage/100) as average from less_stud "
				+ "as t5 left join student as  t1 on t5.student_id=t1.id left join "
				+ "subjects as t2 on t5.subject_id=t2.id left join sinif as t6 on "
				+ "t1.group_id=t6.id  left join subj_exam as t4 on t5.id = t4.stud_less_id "
				+ "left join exam as t3 on t4.exam_id=t3.exam_id "
				+ "where  t2.id=? and t5.year_id=? and t5.sem_id=? and t5.student_id =?;";

		double average = 0;

		PreparedStatement stat = dbCon.prepareStatement(sql);
		stat.setInt(1, subj_id);
		stat.setString(2, year);
		stat.setString(3, sem);
		stat.setString(4, s_id);
		ResultSet result = stat.executeQuery();

		double midterm = 0, fin = 0, mup = 0.0;
		while (result.next()) {
			String exam = result.getString("exam");
			if (result.wasNull()) {
				average = 0;
			} else if (exam.equals("Midterm")) {
				midterm = result.getDouble("average");
			} else if (exam.equals("Final")) {
				fin = result.getDouble("average");
				if (fin < 29.7) {
					fin = 0.0;
				}
			} else if (exam.equals("MakeUp")) {
				mup = result.getDouble("average");
				if (mup < 29.7) {
					mup = 0.0;
				}
			}

			if ((midterm + fin) < 49.5) {
				average = midterm + mup;
			} else {
				average = midterm + fin;
			}
		}
		return average;
	}

	public ArrayList<StudLess> getArray() {
		return q;
	}
}