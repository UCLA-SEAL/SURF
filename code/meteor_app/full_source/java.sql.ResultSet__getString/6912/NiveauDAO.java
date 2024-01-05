package fr.imie.formation.DAO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import fr.imie.formation.DAO.exceptions.DAOException;
import fr.imie.formation.DAO.interfaces.INiveauDAO;
import fr.imie.formation.DTO.CompetenceDTO;
import fr.imie.formation.DTO.NiveauDTO;
import fr.imie.formation.DTO.UtilisateurDTO;
import fr.imie.formation.transactionalFramework.ATransactional;
import fr.imie.formation.transactionalFramework.exception.TransactionalConnectionException;

public class NiveauDAO extends ATransactional implements INiveauDAO {

	public List<NiveauDTO> readCompetenceNiveauUtilisateur(
			UtilisateurDTO utilisateur)
					throws TransactionalConnectionException, DAOException {

		// initialisation de la liste qui servira au retour
		List<NiveauDTO> listcompNiv = null;

		// obtention des DTO avec une nouvelle connection
		listcompNiv = readCompetenceNiveauUtilisateur(utilisateur,
				getConnection());
		return listcompNiv;

	}

	public List<NiveauDTO> readNiveauUtilisateurCompetence(
			CompetenceDTO competence) throws TransactionalConnectionException,
			DAOException {



		// initialisation de la liste qui servira au retour
		List<NiveauDTO> listNivUtilisateur = null;

		// obtention des DTO avec une nouvelle connection
		listNivUtilisateur = readNiveauUtilisateurCompetence(competence,
				getConnection());
		return listNivUtilisateur;

	}

	public int addCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau)
			throws TransactionalConnectionException, DAOException{		
		int addNum=0;
		addNum= addCompUtil(utilisateur, comp, niveau, getConnection());
		return addNum;
	}


	public int updateCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau)
			throws TransactionalConnectionException, DAOException{		
		int updateNum=0;
		updateNum= updateCompUtil(utilisateur, comp, niveau, getConnection());
		return updateNum;
	}


	public int deleteCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau)
			throws TransactionalConnectionException, DAOException{		
		int deleteNum=0;
		deleteNum= deleteCompUtil(utilisateur, comp, niveau, getConnection());
		return deleteNum;
	}

	public List<NiveauDTO> readAllNomNiveau()
			throws TransactionalConnectionException, DAOException {

		List<NiveauDTO> listeNomNiveau = null;
		listeNomNiveau= readAllNomNiveau(getConnection());

		return listeNomNiveau;
	}
	
	public NiveauDTO readNiveau(NiveauDTO niveauDTO)
			throws TransactionalConnectionException, DAOException {
		NiveauDTO niveau = null;
		niveau = readNiveau(niveauDTO, getConnection());
		
		return niveau;
	}

	// Liste des Niveaux et compétences pour un utilisateur
	private List<NiveauDTO> readCompetenceNiveauUtilisateur(
			UtilisateurDTO utilisateur, Connection cn)
					throws TransactionalConnectionException, DAOException {

		PreparedStatement pstm = null;
		ResultSet rst = null;

		List<NiveauDTO> listcompNiv = new ArrayList<NiveauDTO>();

		try {
			String query = "SELECT niveau.num, niveau.valeur as niveau, competence.num, competence.nom FROM niveau INNER JOIN competence_util ON niveau.num=competence_util.num_niveau INNER JOIN utilisateur ON utilisateur.num=competence_util.num_util INNER JOIN competence ON competence.num=competence_util.num_competence where utilisateur.num=?;";

			pstm = cn.prepareStatement(query);
			pstm.setInt(1, utilisateur.getNum());
			rst = pstm.executeQuery();

			while (rst.next()) {
				NiveauDTO niveau = new NiveauDTO();
				CompetenceDTO comp = new CompetenceDTO();
				niveau.setNum(rst.getInt(1));
				niveau.setNom(rst.getString(2));
				comp.setNum(rst.getInt(3));
				comp.setNom(rst.getString(4));
				niveau.setCompetence(comp);
				listcompNiv.add(niveau);

			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rst != null) {
					rst.close();
				}
				if (pstm != null) {
					pstm.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return listcompNiv;
	}

	// Liste des Niveaux et utilisateurs pour une compétence
	private List<NiveauDTO> readNiveauUtilisateurCompetence(
			CompetenceDTO competence, Connection cn)
					throws TransactionalConnectionException, DAOException {

		PreparedStatement pstm = null;
		ResultSet rst = null;

		List<NiveauDTO> listNiveau = new ArrayList<NiveauDTO>();

		try {

			String query = "SELECT utilisateur.num, utilisateur.nom, utilisateur.prenom, niveau.num, niveau.valeur as niveau, competence.num, competence.nom FROM niveau INNER JOIN competence_util ON niveau.num=competence_util.num_niveau INNER JOIN utilisateur ON utilisateur.num=competence_util.num_util INNER JOIN competence ON competence.num=competence_util.num_competence where competence.num=?;";

			pstm = cn.prepareStatement(query);
			pstm.setInt(1, competence.getNum());
			rst = pstm.executeQuery();

			while (rst.next()) {
				UtilisateurDTO utilisateur = new UtilisateurDTO();
				NiveauDTO niveau = new NiveauDTO();
				CompetenceDTO comp = new CompetenceDTO();
				utilisateur.setNum(rst.getInt(1));
				utilisateur.setNom(rst.getString(2));
				utilisateur.setPrenom(rst.getString(3));
				niveau.setUtilisateur(utilisateur);
				niveau.setNum(rst.getInt(4));
				niveau.setNom(rst.getString(5));
				comp.setNum(rst.getInt(6));
				comp.setNom(rst.getString(7));
				niveau.setCompetence(comp);

				listNiveau.add(niveau);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rst != null) {
					rst.close();
				}
				if (pstm != null) {
					pstm.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return listNiveau;
	}
	
	private int addCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau,Connection cn)
			throws TransactionalConnectionException, DAOException {

		int addNum=0;
		PreparedStatement pstm=null;

		try {
			String query="INSERT INTO competence_util (num_util, num_competence, num_niveau)VALUES(?,?,?)";
			pstm= cn.prepareStatement(query);
			pstm.setInt(1,utilisateur.getNum());
			pstm.setInt(2, comp.getNum());
			pstm.setInt(3, niveau.getNum());

			addNum= pstm.executeUpdate();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				if (pstm != null) {
					pstm.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return addNum;
	}
	private int updateCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau,Connection cn)
			throws TransactionalConnectionException, DAOException {

		PreparedStatement pstm= null;
		int updateNum = 0;

		try {
			String query ="update competence_util set num_niveau=? where num_util=? and num_competence=?";
			pstm=cn.prepareStatement(query);
			pstm.setInt(2, utilisateur.getNum());
			pstm.setInt(3, comp.getNum());
			pstm.setInt(1, niveau.getNum());

			updateNum=pstm.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				if (pstm != null) {
					pstm.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return updateNum;
	}
	private int deleteCompUtil(UtilisateurDTO utilisateur,CompetenceDTO comp,NiveauDTO niveau,Connection cn)
			throws TransactionalConnectionException, DAOException {
		int deleteNum=0;
		PreparedStatement pstm=null;
		try {
			String query="delete from competence_util where num_util=?, and num_competence=?, and num_niveau=?";
			pstm=cn.prepareStatement(query);
			pstm.setInt(1, utilisateur.getNum());
			pstm.setInt(2, comp.getNum());
			pstm.setInt(3, niveau.getNum());

			deleteNum=pstm.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			try {
				if (pstm != null) {
					pstm.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return deleteNum;
	}

	private List<NiveauDTO> readAllNomNiveau(Connection cn)
			throws TransactionalConnectionException, DAOException{

		List<NiveauDTO> listeNomNiveau = new ArrayList<NiveauDTO>();

		Statement stmt= null;
		ResultSet rst= null;

		try {
			String query = "select num,valeur from niveau"	;

			stmt=cn.createStatement();
			rst=stmt.executeQuery(query);

			while(rst.next()){

				NiveauDTO nomNiveau = new NiveauDTO();
				nomNiveau.setNum(rst.getInt(1));
				nomNiveau.setNom(rst.getString(2));

				listeNomNiveau.add(nomNiveau);
			}

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}finally {
			try {
				if (rst != null) {
					rst.close();
				}
				if (stmt != null) {
					stmt.close();
				}

			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return listeNomNiveau;
	}
	
	private NiveauDTO readNiveau(NiveauDTO niveauDTO, Connection cn)
			throws TransactionalConnectionException, DAOException {

		PreparedStatement pstmt = null;
		ResultSet rst = null;

		NiveauDTO niveau = new NiveauDTO();

		String query = "select num, valeur from niveau where num=?";
		try {
			pstmt = cn.prepareStatement(query);
			pstmt.setInt(1, niveauDTO.getNum());
			rst = pstmt.executeQuery();

			while (rst.next()) {			
				niveau.setNum(rst.getInt(1));
				niveau.setNom(rst.getString(2));
						
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (rst != null) {
					rst.close();
				}
				if (pstmt != null) {
					pstmt.close();
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return niveau;
	}
}
