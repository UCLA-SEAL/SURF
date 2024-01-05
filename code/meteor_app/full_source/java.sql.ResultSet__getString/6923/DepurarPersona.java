package cidc.general.obj;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;


public class DepurarPersona{
	String motor,driver,host,puerto,bd,usuario,password;
	

	public Connection abrir() throws Exception{
		   Connection cn=null;
			try{
				ResourceBundle rb=java.util.ResourceBundle.getBundle("cidc.general.conect");
				motor="postgresql";
				driver="org.postgresql.Driver";
				host="10.20.0.41";
				puerto="5432";
				bd="siciud";
				usuario="postgres";
				password="cpnatas40";
				Class.forName(driver);
				cn=DriverManager.getConnection(getUrl(), usuario, password);
			}catch(ClassNotFoundException cnfe){
				throw new Exception(cnfe);
			}
			catch(SQLException sqle){
				throw new Exception(sqle);
			}
			return cn;
		}
	
	public String getUrl(){
		if(motor.equals("postgresql") && host!=null && puerto!=null && bd!=null)
			return("jdbc:postgresql://"+host+":"+puerto+"/"+bd);
		return "";
	}
	
	public void depurar(){
		List paMatar=new ArrayList();
		int i=0;
		pipol p=null;
		try {
			ResultSet rs=null;
			Connection cn =abrir();
			PreparedStatement ps=cn.prepareStatement("select perid,pernombres,perapellidos,pernumdoc from personal where (pernumdoc is null or pernumdoc ='') and "+ 
														"perid <> all(select distinct perid from personal,inscrip_propuesta where inscinvprin=perid and (pernumdoc is null or pernumdoc ='')) and "+
														"perid <> all(select distinct perid from personal,cidc_grup_semill where cgsdirector=perid and (pernumdoc is null or pernumdoc ='')) and "+
														"perid <> all(select distinct perid from personal,pa_proy_data_gral where ppdginvesproy=perid and (pernumdoc is null or pernumdoc ='')) and "+
														"perid <> all(select distinct perid from personal,b_pares where beidpersona=perid and (pernumdoc is null or pernumdoc ='')) and "+
														"perid <> all(select perid from personal,usuario_sistema where (pernumdoc is null or pernumdoc ='') and usidpersona=perid) and "+
														"perid <> 0 and perid <> 7 and perid <> 11 and perid <> 8 and perid <> 9 and perid <> 10 and perid <> 12 and perid <> 13 and perid <> 18 and "+
														"perid <> 3455 and perid <> 4493 and perid <> 4494 and perid <> 4495 and perid <> 4496 and perid <> 4497"+
														"order by perid");
			rs=ps.executeQuery();
			while(rs.next()){
				i=1;
				p=new pipol();
				p.setId(rs.getInt(i++));
				p.setNombre(rs.getString(i++));
				p.setApellido(rs.getString(i++));
				p.setDoc(rs.getString(i++));
				paMatar.add(p);
			}
			cn.setAutoCommit(false);
			System.out.println("---Hay que eliminar a -->"+paMatar.size()+" Personas");
			ps=cn.prepareStatement("delete from b_investigadores where binvidper=?");
			for(int j =0;j<paMatar.size();j++){
				p=(pipol)paMatar.get(j);
				ps.setInt(1, p.getId());
				ps.addBatch();
			}
			System.out.println("---eliminados-->"+ps.executeBatch()+" investigadores");
			ps=cn.prepareStatement("delete from personal where perid=?");
			for(int j =0;j<paMatar.size();j++){
				p=(pipol)paMatar.get(j);
				ps.setInt(1, p.getId());
				ps.addBatch();
			}
			System.out.println("---eliminados-->"+ps.executeBatch()+" personajes");
			cn.commit();
			System.out.println("---proceso terminado-->");
		}catch (SQLException e) {
				// TODO Auto-generated catch block
			System.out.println("----->"+e.getNextException());
				e.printStackTrace();
				
			}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DepurarPersona dp=new DepurarPersona();
		dp.depurar();
	}

	
	public class pipol{
		private int id;
		private String nombre;
		private String apellido;
		private String doc;
		public int getId() {
			return id;
		}
		public void setId(int id) {
			this.id = id;
		}
		public String getNombre() {
			return nombre;
		}
		public void setNombre(String nombre) {
			this.nombre = nombre;
		}
		public String getApellido() {
			return apellido;
		}
		public void setApellido(String apellido) {
			this.apellido = apellido;
		}
		public String getDoc() {
			return doc;
		}
		public void setDoc(String doc) {
			this.doc = doc;
		}
		
	}
}
