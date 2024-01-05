package com.sanxing.sesame.pwd;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.crypto.Cipher;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sanxing.sesame.util.Base64;

public class PasswordTool
{
    private static final Logger LOG = LoggerFactory.getLogger( PasswordTool.class );

    public static String decrypt( String passwd )
    {
        if ( passwd == null || passwd.isEmpty() )
        {
            return passwd;
        }
        try
        {
            String serverDir = System.getProperty( "SESAME_HOME" );
            Document doc = new SAXBuilder().build( serverDir + "/security/license.sesame" );
            Element serials = doc.getRootElement().getChild( "serials" );
            String storePasswd = serials.getChildText( "serial-1" );
            String keyPasswd = serials.getChildText( "serial-2" );
            FileInputStream fis1 = new FileInputStream( serverDir + "/security/sesame-server.jks" );
            KeyStore ks1 = KeyStore.getInstance( "jks" );
            PasswordCipher tool = PasswordCipher.getInstance();
            ks1.load( fis1, tool.decrypt( Base64.decode( storePasswd ) ).toCharArray() );
            Key privateKey = ks1.getKey( "sesame-server", tool.decrypt( Base64.decode( keyPasswd ) ).toCharArray() );
            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.DECRYPT_MODE, privateKey );
            return new String( cipher.doFinal( Base64.decode( passwd ) ) );
        }
        catch ( Exception e )
        {
            LOG.debug( e.getMessage(), e );
        }
        return passwd;
    }

    public static String encrypt( String passwd )
    {
        if ( passwd == null || passwd.isEmpty() )
        {
            return passwd;
        }
        try
        {
            String serverDir = System.getProperty( "SESAME_HOME" );
            Document doc = new SAXBuilder().build( serverDir + "/security/license.sesame" );

            Element serials = doc.getRootElement().getChild( "serials" );
            String storePasswd = serials.getChildText( "serial-1" );
            FileInputStream fis1 = new FileInputStream( serverDir + "/security/sesame-server.jks" );
            KeyStore ks1 = KeyStore.getInstance( "jks" );
            PasswordCipher tool = PasswordCipher.getInstance();
            ks1.load( fis1, tool.decrypt( Base64.decode( storePasswd ) ).toCharArray() );
            Certificate cert = ks1.getCertificate( "sesame-server" );

            Cipher cipher = Cipher.getInstance( "RSA" );
            cipher.init( Cipher.ENCRYPT_MODE, cert.getPublicKey() );
            return Base64.encode( cipher.doFinal( passwd.getBytes() ) );
        }
        catch ( Exception e )
        {
            LOG.debug( e.getMessage(), e );
        }
        return passwd;
    }
}