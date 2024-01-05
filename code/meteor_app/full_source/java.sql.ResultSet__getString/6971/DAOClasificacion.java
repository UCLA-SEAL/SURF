package clasificaciones.dao;

import clasificaciones.dominio.Grupo;
import clasificaciones.to.TOSubGrupo;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import usuarios.dominio.UsuarioSesion;

/**
 *
 * @author Julio
 */
public class DAOClasificacion {

    private DataSource ds;
    private Connection cn;

    public DAOClasificacion() throws NamingException {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            ExternalContext externalContext = context.getExternalContext();
            HttpSession httpSession = (HttpSession) externalContext.getSession(false);
            UsuarioSesion usuarioSesion = (UsuarioSesion) httpSession.getAttribute("usuarioSesion");

            Context cI = new InitialContext();
            ds = (DataSource) cI.lookup("java:comp/env/"+usuarioSesion.getJndi());
        } catch (NamingException ex) {
            throw (ex);
        }
    }

    public int agregar(int codigoSubGrupo, String subGrupo, int idGrupo) throws SQLException {
        int idSubGrupo = 0;

        cn = ds.getConnection();
        Statement st = cn.createStatement();
        try {
            st.executeUpdate("begin Transaction");

            st.executeUpdate("INSERT INTO productosSubGrupos (codigoSubGrupo, subGrupo, idGrupo) VALUES (" + codigoSubGrupo + ", '" + subGrupo + "', " + idGrupo + ")");

            ResultSet rs = st.executeQuery("SELECT MAX(idSubGrupo) as idSubGrupo FROM productosSubGrupos");
            if (rs.next()) {
                idSubGrupo = rs.getInt("idSubGrupo");
            }

            st.executeUpdate("commit Transaction");
        } catch (SQLException ex) {
            st.executeUpdate("rollback Transaction");
            throw (ex);
        } finally {
            cn.close();
        }
        return idSubGrupo;
    }

    public void modificar(int idSubGrupo, int codigoSubGrupo, String subGrupo, int idGrupo) throws SQLException {
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            st.executeUpdate("UPDATE productosSubGrupos SET codigoSubGrupo=" + codigoSubGrupo + ", subGrupo='" + subGrupo + "', idGrupo=" + idGrupo + " "
                    + "WHERE idSubGrupo=" + idSubGrupo);
        } finally {
            cn.close();
        }
    }

    public TOSubGrupo obtenerSubGrupo(int idSubGrupo) throws SQLException {
        TOSubGrupo toSubGrupo = null;
        cn = ds.getConnection();
        Statement st = cn.createStatement();
        try {

            ResultSet rs = st.executeQuery("SELECT * FROM productosSubGrupos WHERE idSubGrupo=" + idSubGrupo);
            if (rs.next()) {
                toSubGrupo = new TOSubGrupo(rs.getInt("idSubGrupo"), rs.getInt("codigoSubGrupo"), rs.getString("SubGrupo"), rs.getInt("idGrupo"));
            }
        } finally {
            cn.close();
        }
        return toSubGrupo;
    }

    public ArrayList<TOSubGrupo> obtenerSubGrupos() throws SQLException {
        ArrayList<TOSubGrupo> lista = new ArrayList<TOSubGrupo>();
        ResultSet rs = null;

        cn = ds.getConnection();
        String strSQL = "SELECT * FROM productosSubGrupos ORDER BY idGrupo, subGrupo";
        try {
            Statement sentencia = cn.createStatement();
            rs = sentencia.executeQuery(strSQL);
            while (rs.next()) {
                lista.add(construir(rs));
            }
        } finally {
            cn.close();
        }
        return lista;
    }

    private TOSubGrupo construir(ResultSet rs) throws SQLException {
        return new TOSubGrupo(rs.getInt("idSubGrupo"), rs.getInt("codigoSubGrupo"), rs.getString("subGrupo"), rs.getInt("idGrupo"));
    }

    public int obtenerUltimoCodigoSubGrupo() throws SQLException {
        int xCodigoSubGrupo = 0;
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT MAX(codigoSubGrupo) AS ultimo FROM productosSubGrupos");
            if (rs.next()) {
                xCodigoSubGrupo = rs.getInt("ultimo");
            }
        } finally {
            cn.close();
        }
        return xCodigoSubGrupo;
    }

    public int obtenerIdGrupo(int codigoGrupo) throws SQLException {
        int xIdGrupo = 0;
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT idGrupo FROM productosGrupos WHERE codigoGrupo=" + codigoGrupo);
            if (rs.next()) {
                xIdGrupo = rs.getInt("idGrupo");
            }
        } finally {
            cn.close();
        }
        return xIdGrupo;
    }

    public Grupo obtenerGrupo(int idGrupo) throws SQLException {
        Grupo grupo = null;
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT * FROM productosGrupos WHERE idGrupo=" + idGrupo);
            if (rs.next()) {
                grupo = new Grupo(rs.getInt("idGrupo"), rs.getInt("codigoGrupo"), rs.getString("grupo"));
            }
        } finally {
            cn.close();
        }
        return grupo;
    }

    public List<SelectItem> obtenerGrupos() throws SQLException {
        List<SelectItem> grupos = new ArrayList<SelectItem>();
        ResultSet rs = null;

        cn = ds.getConnection();

        String strSQL = "SELECT * FROM productosGrupos ORDER BY grupo";
        try {
            Grupo grupo = new Grupo(0, 0, "SELECCIONE UN GRUPO");
            grupos.add(new SelectItem(grupo, grupo.getGrupo()));

            Statement sentencia = cn.createStatement();
            rs = sentencia.executeQuery(strSQL);
            while (rs.next()) {
                grupo = new Grupo(rs.getInt("idGrupo"), rs.getInt("codigoGrupo"), rs.getString("grupo"));
                grupos.add(new SelectItem(grupo, grupo.getGrupo()));
            }
        } finally {
            cn.close();
        }
        return grupos;
    }

    public int agregarGrupo(int codigoGrupo, String grupo) throws SQLException {
        int idGrupo = 0;
        cn = ds.getConnection();
        Statement st = cn.createStatement();
        try {
            st.executeUpdate("begin Transaction");


            st.executeUpdate("INSERT INTO productosGrupos (codigoGrupo, grupo) VALUES (" + codigoGrupo + ",'" + grupo + "')");

            ResultSet rs = st.executeQuery("SELECT MAX(idGrupo) AS idGrupo FROM productosGrupos");
            if (rs.next()) {
                idGrupo = rs.getInt("idGrupo");
            }

            st.executeUpdate("commit Transaction");

        } catch (SQLException ex) {
            st.executeUpdate("rollback Transaction");
            throw (ex);
        } finally {
            cn.close();
        }
        return idGrupo;
    }

    public int obtenerUltimoCodigoGrupo() throws SQLException {
        int ultimo = 0;
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            ResultSet rs = st.executeQuery("SELECT max(codigoGrupo) as ultimo FROM productosGrupos");
            if (rs.next()) {
                ultimo = rs.getInt("ultimo");
            }
        } finally {
            cn.close();
        }
        return ultimo;
    }

    public void modificarGrupo(int idGrupo, int codigoGrupo, String grupo) throws SQLException {
        cn = ds.getConnection();
        try {
            Statement st = cn.createStatement();
            st.executeUpdate("UPDATE productosGrupos SET codigoGrupo=" + codigoGrupo + ", grupo='" + grupo + "' WHERE idGrupo=" + idGrupo);
        } finally {
            cn.close();
        }
    }
}
