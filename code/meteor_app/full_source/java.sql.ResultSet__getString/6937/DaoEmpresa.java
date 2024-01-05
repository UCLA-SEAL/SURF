/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dao;

import accesoDatos.FachadaBD;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import logica.Postpago;
import logica.Empresa;


/**
 *
 * @author juandrd
 */
public class DaoEmpresa {

    FachadaBD fachada;

    public DaoEmpresa() {
        fachada = new FachadaBD();
    }//

    public int guardar(Empresa e) {
        String sql_guardar;
        sql_guardar = "INSERT INTO empresa VALUES ('"
                + e.getNombre() + "', '"
                + e.getTelefono() + "', '"
                + e.getDireccion() + "', '"
                + e.getCod_plan().getCod_plan().getCod_plan() + "')";
        try {
            Connection conn = fachada.conectar();
            Statement sentencia = conn.createStatement();
            int numFilas = sentencia.executeUpdate(sql_guardar);
            conn.close();
            return numFilas;
        } catch (SQLException ex) {
            System.out.println(ex);
        } catch (Exception ex) {
            System.out.println(ex);
        }
        return -1;
    }//fin guardar

    
    
    
    
    
    public LinkedList consultar(String nombre, String telefono,
            String direccion, String cod_plan) {
        LinkedList empresaConsulta = new LinkedList();
        String sql_select = "SELECT * FROM empresa      ";
        if (!nombre.equals("") 
            || !telefono.equals("")
            || !direccion.equals("")
            || !cod_plan.equals("")) {
            sql_select += "WHERE";
        }
      
        if(!nombre.equals("")){
            sql_select += " nombre LIKE '%"+nombre+"%'"+" AND ";
        }
        if(!telefono.equals("")){
            sql_select += " telefono LIKE '%"+telefono+"%'"+" AND ";
        }
        if(!direccion.equals("")){
            sql_select += " direccion LIKE '%"+direccion+"%'"+" AND ";
        }
        if(!cod_plan.equals(" ")){
            sql_select += " cod_plan LIKE '%"+cod_plan+"%'"+" AND ";
        }
                     
        sql_select = sql_select.substring(0, sql_select.length() - 5);
        System.out.println(sql_select);
        try {
            Connection conn = fachada.conectar();
            Statement sentencia = conn.createStatement();
            ResultSet tabla = sentencia.executeQuery(sql_select);
            while (tabla.next()) {
                Empresa empresa = new Empresa();
                empresa.setNombre(tabla.getString("nombre"));
                empresa.setTelefono(tabla.getString("telefono"));
                empresa.setDireccion(tabla.getString("direccion"));
                empresa.setCod_plan(new DaoPostpago().consultar(tabla.getString("cod_plan")));
                empresaConsulta.add(empresa);
            }
            conn.close();
            System.out.println("Conexion cerrada");
            return empresaConsulta;

        } catch (SQLException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }
    
    
    public Empresa consultar(String nombre) {
        Empresa em = new Empresa();
        String sql_select;
        sql_select = "SELECT * FROM empresa WHERE nombre='" + nombre + "'";
        try {
            Connection conn = fachada.conectar();
            Statement sentencia = conn.createStatement();
            ResultSet tabla = sentencia.executeQuery(sql_select);

            //
            if (tabla.next()) {

                em.setNombre(tabla.getString("nombre"));
                em.setTelefono(tabla.getString("telefono"));
                em.setDireccion(tabla.getString("direccion"));
                em.setCod_plan(new DaoPostpago().consultar(tabla.getString("cod_plan")));



            }

            conn.close();
            System.out.println("Conexion cerrada");
            return em;

        } catch (SQLException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }

    public int editar(Empresa em) {

        String sql_update;
        sql_update = "UPDATE empresa  SET "
                + "telefono='" + em.getTelefono() + "', "
                + "direccion='" + em.getDireccion() + "', "
                + "cod_plan='" + em.getCod_plan().getCod_plan().getCod_plan() + "' "
                + "WHERE nombre='" + em.getNombre() + "'";
        try {
            Connection conn = fachada.conectar();
            Statement sentencia = conn.createStatement();
            sentencia.executeUpdate(sql_update);


            conn.close();
            System.out.println("Conexion cerrada");
            return 0;

        } catch (SQLException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        }
        return -1;
    }
}
