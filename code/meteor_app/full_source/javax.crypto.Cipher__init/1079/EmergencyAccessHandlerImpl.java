package ehospital.server.remote.impl;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import remote.obj.EmergencyAccessHandler;
import ehospital.server.RMIConsole;
import ehospital.server.Session;
import ehospital.server.Utility;
import ehospital.server.db.DBManager;
import ehospital.server.db.Logger;

/**
 * Emergency Access implementation
 * @author Gilbert
 *
 */
public class EmergencyAccessHandlerImpl extends UnicastRemoteObject implements EmergencyAccessHandler {
	
	/**
	 * Default constructor. Does Nothing.
	 * @throws RemoteException
	 */
	public EmergencyAccessHandlerImpl() throws RemoteException {
		
	}

	private static final long serialVersionUID = -1197149669700355840L;
	
	/* (non-Javadoc)
	 * @see remote.obj.EmergencyAccessHandler#emergencyAccess(byte[], byte[], byte[], int)
	 */
	public int emergencyAccess(byte[] username, byte[] emergencyUser,
			byte[] emergencyPwd, int tmpCardNum) throws RemoteException {
		String clientIP = Utility.getClientHost();
		
		
		try {
			Cipher c = null;
			c = Cipher.getInstance("aes");
			//decrypt in program key
			c.init(Cipher.DECRYPT_MODE, RMIConsole.ProgramKey);
			username = c.doFinal(username);
			String strUsername = new String(username);
			
			Session s = ehospital.server.SessionList.findClient(strUsername); 
			if (s == null) {
				return 1;
			} else {
				DBManager dbm = new DBManager();
				if (dbm.connect()) {
					//decrypt in program key
					c.init(Cipher.DECRYPT_MODE, RMIConsole.ProgramKey);
					emergencyUser = c.doFinal(emergencyUser);
					emergencyPwd = c.doFinal(emergencyPwd);
					//decrypt in session key
					c.init(Cipher.DECRYPT_MODE, s.getSessionKey());
					emergencyUser = c.doFinal(emergencyUser);
					emergencyPwd = c.doFinal(emergencyPwd);
					
					String strEmergencyUser = new String(emergencyUser);
					ResultSet emergencyUserData = dbm.isUserExist(strEmergencyUser);
					if (dbm.isUserExist(strUsername) != null  && emergencyUserData != null ){
						try {
							String strEmergencyPwd = Utility.byteArrayToString(emergencyPwd);
							if (!emergencyUserData.getString("pwd").equals(strEmergencyPwd)) {
								Logger.log(clientIP, "failed to authorize a temp card due to incorrect password from the temp card user.");
								return 1;
							}
							ResultSet tmpUser = dbm.query("select * from `tmp_user` where `isTaken` = 0 AND `id` = " + tmpCardNum, null);
							if (tmpUser.first()) {
								dbm.update("update `tmp_user` set `isTaken` = 1 where `id` = " + tmpUser.getString(1));
							} else {
								Logger.log(clientIP, "failed to authorize a temp card due to the temp card is authorized to other users.");
								return 4;
							}
							//record swapping in DB
							String [] n = {emergencyUserData.getString("uid"),emergencyUserData.getString("pub_key"),emergencyUserData.getString("mod"),Integer.toString(tmpCardNum)};
							dbm.update("insert into `swapped_user` (`user_id`, `ori_pub_key`, `ori_mod`, `tmp_card_num`) VALUE (?,?,?,?)",n);
							//swap key pair
							String [] o = {tmpUser.getString("pub_key"),tmpUser.getString("mod"),emergencyUserData.getString("uid")};
							dbm.update("update `user` set `pub_key` = ?, `mod` = ? where `uid` = ?", o);
							System.out.println(strUsername+" is authorizing " + strEmergencyUser + " in " + clientIP);
							System.out.print("~>");
							Logger.log(clientIP, " successfully authorized temp card no."+tmpCardNum+".");
							return 0;
						} catch (SQLException e) {
							e.printStackTrace();
							Logger.log(clientIP, " failed to authorize temp card due to: \n"+e.getMessage());
							return 2;
						}
					} else {
						Logger.log(clientIP, " failed to authorize temp card due to no such user.");
						return 1;
					}
				}
			}
			return 0;
		} catch (NoSuchAlgorithmException e) {
			
			return 1024;
		} catch (NoSuchPaddingException e) {
			Logger.log(clientIP, " failed to authorize temp card due to: \n"+e.getMessage());
			return 8;
		} catch (InvalidKeyException e) {
			Logger.log(clientIP, " failed to authorize temp card due to: \n"+e.getMessage());
			return 8;
		} catch (IllegalBlockSizeException e) {
			Logger.log(clientIP, " failed to authorize temp card due to: \n"+e.getMessage());
			return 4;
		} catch (BadPaddingException e) {
			Logger.log(clientIP, " failed to authorize temp card due to: \n"+e.getMessage());
			return 4;
		}
	}

}
