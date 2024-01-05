package com.culturaPococi.data;

import com.culturaPococi.dominio.Publicacion;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import javax.swing.JOptionPane;

/**
 *
 * @author jonathan
 */

public class DataPublicacion extends DataBase {

    public LinkedList<Publicacion> getListaPublicacion() throws SQLException {

        LinkedList<Publicacion> listaPublicacion = new LinkedList<Publicacion>();

        Publicacion p;

        String nombrePerfil;
        int idPublicacion;
        String fechaPublicacion;
        String correo;
        String descripcion;
        int verificacion;
        String musica;
        String video;
        String texto;
        String imagen;
        String nombreCategoria;

        String sql = "call pListaPublicaciones();";
        ResultSet resultado;
        Connection conexion = super.getConexion();

        Statement statement = conexion.createStatement();
        resultado = statement.executeQuery(sql);


        while (resultado.next()) {
            p = new Publicacion();

            nombrePerfil = resultado.getString(1);
            idPublicacion = resultado.getInt(2);
            fechaPublicacion = resultado.getString(3);
            correo = resultado.getString(4);
            descripcion = resultado.getString(5);
            verificacion = resultado.getInt(6);
            musica = resultado.getString(7);
            video = resultado.getString(8);
            texto = resultado.getString(9);
            imagen = resultado.getString(10);
            nombreCategoria = resultado.getString(11);

            p.setNombrePerfil(nombrePerfil);
            p.setIdPublicacion(idPublicacion);
            p.setFechaPublicacion(fechaPublicacion);
            p.setCorreo(correo);
            p.setDescripcion(descripcion);
            p.setVerificacion(verificacion);
            p.setMusica(musica);
            p.setVideo(video);
            p.setTexto(texto);
            p.setImagen(imagen);
            p.setNombreCategoria(nombreCategoria);

            listaPublicacion.add(p);

        }
        resultado.close();
        System.out.println("exito");

        return listaPublicacion;
    }

    public LinkedList<Publicacion> getListaPublicacionPendiente(String tipo) throws SQLException {

        LinkedList<Publicacion> listaPublicacionMusica = new LinkedList<Publicacion>();

        Publicacion publicacion;

        String sql = "call pPublicacionPendiente('" + tipo + "');";
        ResultSet resultado;
        Connection conexion = super.getConexion();
        try {

            Statement statement = conexion.createStatement();
            resultado = statement.executeQuery(sql);

            while (resultado.next()) {
                publicacion = new Publicacion(resultado.getString("nombrePerfil"),
                        resultado.getInt("idPublicacion"),
                        resultado.getString("fechaPublicacion"),
                        "", "", 0, "", "", "", "", "", 0, 0);
                listaPublicacionMusica.add(publicacion);
            }//fin while
            statement.close();
        } catch (Exception e) {
            listaPublicacionMusica = null;
        } finally {
            conexion.close();
        }

        return listaPublicacionMusica;
    }

    public Publicacion selectPublicacion(int idPublicacion) throws SQLException {
        JOptionPane.showMessageDialog(null, idPublicacion);
        Publicacion publicacion = new Publicacion();
        LinkedList<Publicacion> listaPublicaciones = new LinkedList<Publicacion>();
        String sql = " call pListarPublicacion(" + idPublicacion + ");";
        ResultSet resultado;
        Connection conexion = super.getConexion();

        try {

            Statement statement = conexion.createStatement();
            resultado = statement.executeQuery(sql);

            while (resultado.next()) {
                publicacion = new Publicacion(
                        resultado.getString("nombrePerfil"), resultado.getInt("idPublicacion"),
                        resultado.getString("fechaPublicacion"),
                        "", resultado.getString("descripcion"), 0,
                        resultado.getString("musica"),
                        resultado.getString("video"),
                        resultado.getString("texto"),
                        resultado.getString("imagen"),
                        resultado.getString("nombreCategoria"), 0, 0);


                listaPublicaciones.add(publicacion);
            }//fin while
            statement.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "catch");
            publicacion = null;
        } finally {
            conexion.close();
        }//fin try
        for (int i = 0; i < listaPublicaciones.size(); i++) {
            if (listaPublicaciones.get(i).getIdPublicacion() == idPublicacion) {
                publicacion = listaPublicaciones.get(i);
                i = listaPublicaciones.size();
            }//fin if
        }//fin for
        return publicacion;
    }//fin selectEventos

    public boolean aceptarPublicacion(int idPublicacion) throws SQLException {
        String sql = "call pAceptarPublicacion(?);";
        boolean accionRealizada = true;

        Connection conexion = super.getConexion();
        try {

            CallableStatement call = conexion.prepareCall(sql);
            call.setInt("pidPublicacion", idPublicacion);
            call.executeUpdate();

            call.close();
        } catch (Exception e) {
            accionRealizada = false;
        } finally {
            conexion.close();
        }
        return accionRealizada;
    }

    public boolean eliminarPublicacion(int idPublicacion) throws SQLException {
        String sql = "call pEliminarPublicacion(?);";
        boolean accionRealizada = true;

        Connection conexion = super.getConexion();
        try {
            CallableStatement call = conexion.prepareCall(sql);
            call.setInt("pidPublicacion", idPublicacion);
            call.executeUpdate();

            call.close();
        } catch (Exception e) {
            accionRealizada = false;
        } finally {
            conexion.close();
        }
        return accionRealizada;
    }

    public boolean crearPublicacion(Publicacion publicacion) throws SQLException {

        String sql = "call pCrearPublicacion(?,?,?,?,?,?,?,?,?,?);";
        boolean accionRealizada = true;
        Connection conexion = super.getConexion();

        try {
            CallableStatement call = conexion.prepareCall(sql);

            call.setString("pnombrePerfil", publicacion.getNombrePerfil());
            call.setString("pfechaPublicacion", publicacion.getFechaPublicacion());
            call.setString("pcorreo", publicacion.getCorreo());
            call.setString("pdescripcion", publicacion.getDescripcion());
            call.setInt("pverificacion", 0);
            call.setString("pmusica", publicacion.getMusica());
            call.setString("pvideo", publicacion.getVideo());
            call.setString("ptexto", publicacion.getTexto());
            call.setString("pimagen", publicacion.getImagen());
            call.setInt("pidCategoria", publicacion.getIdCategoria());
            call.executeUpdate();
            call.close();
            //JOptionPane.showMessageDialog(null, "bien!!!");
        } catch (Exception e) {
            accionRealizada = false;
            //JOptionPane.showMessageDialog(null, "mal!!!");
        } finally {
            conexion.close();
        }//fin try

        return accionRealizada;
    }

    public LinkedList<Publicacion> selectPublicacionPorPerfilYPorTipo(
            String tipoPublicacion,
            String nombrePerfil) throws SQLException {

        Publicacion publicacion;
        LinkedList<Publicacion> listaPublicaciones = new LinkedList<Publicacion>();
        String sql = " select * from publicacion where nombrePerfil = '" + nombrePerfil + "' ;";
        ResultSet resultado;
        Connection conexion = super.getConexion();
        try {

            Statement statement = conexion.createStatement();
            resultado = statement.executeQuery(sql);

            while (resultado.next()) {
                publicacion = new Publicacion(
                        resultado.getString("nombrePerfil"), 
                        resultado.getInt("idPublicacion"),
                        resultado.getString("fechaPublicacion"),
                        "", 
                        resultado.getString("descripcion"), 
                        0,
                        resultado.getString("musica"),
                        resultado.getString("video"),
                        resultado.getString("texto"),
                        resultado.getString("imagen"),
                        resultado.getString("nombreCategoria"),
                        resultado.getInt("idCategoria"), 
                        resultado.getInt("tipo"));
                listaPublicaciones.add(publicacion);
            }//fin while
            statement.close();
        } catch (Exception e) {
            listaPublicaciones = null;
            JOptionPane.showMessageDialog(null, e.getMessage());
        } finally {
            conexion.close();
        }//fin finally
        return listaPublicaciones;
    }//fin selectPublicaciones

    public LinkedList<Publicacion> selectPublicacionPorCorreo(
            String correo) throws SQLException {

        Publicacion publicacion;
        LinkedList<Publicacion> listaPublicaciones = new LinkedList<Publicacion>();
        String sql = "select * from publicacion where correo = '" + correo + "';";
        ResultSet resultado;
        Connection conexion = super.getConexion();
        try {

            Statement statement = conexion.createStatement();
            resultado = statement.executeQuery(sql);

            while (resultado.next()) {
                publicacion = new Publicacion(
                        resultado.getString("nombrePerfil"),
                        resultado.getInt("idPublicacion"),
                        resultado.getString("fechaPublicacion"),
                        "", resultado.getString("descripcion"), 0,
                        resultado.getString("musica"),
                        resultado.getString("video"),
                        resultado.getString("texto"),
                        resultado.getString("imagen"),
                        "",
                        resultado.getInt("idCategoria"),
                        resultado.getInt("tipo"));
                listaPublicaciones.add(publicacion);
            }//fin while
            statement.close();
        } catch (Exception e) {
            listaPublicaciones = null;
            JOptionPane.showMessageDialog(null, e.getMessage());
        } finally {
            conexion.close();
        }//fin finally

        return listaPublicaciones;
    }//fin selectPublicaciones
}
