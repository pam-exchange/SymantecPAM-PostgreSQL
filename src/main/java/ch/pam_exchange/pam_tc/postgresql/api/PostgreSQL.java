package ch.pam_exchange.pam_tc.postgresql.api;

import com.ca.pam.extensions.core.model.LoggerWrapper;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ca.pam.extensions.core.api.exception.ExtensionException;
import com.ca.pam.extensions.core.TargetAccount;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgreSQL {

	private static final Logger LOGGER = Logger.getLogger(PostgreSQL.class.getName());
	private static final boolean EXTENDED_DEBUG = false;

	private static final String PROPERTY_FILE = "extensions.properties";
	private static final String PROPERTY_POSTGRESQL_LOGLEVEL = "postgresql.driver.loglevel";
	
	private static String POSTGRESQL_LOGLEVEL= "OFF";
	static {
		try {
			Properties props= new Properties();
			props.load(PostgreSQL.class.getClassLoader().getResourceAsStream(PROPERTY_FILE));
			if (props.getProperty(PostgreSQL.PROPERTY_POSTGRESQL_LOGLEVEL)!=null) {
				POSTGRESQL_LOGLEVEL= props.getProperty(PostgreSQL.PROPERTY_POSTGRESQL_LOGLEVEL);
			}
			Class.forName("org.postgresql.Driver");
		}
		catch (Exception e) {
			LOGGER.severe(LoggerWrapper.logMessage("Cannot load properties from '"+PROPERTY_FILE+"'"));
			LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Exception"), e);
		}
	}
	
	/**
	 * Constants
	 */
	private static final int DEFAULT_PORT = 5432;
	private static final long DEFAULT_CONNECT_TIMEOUT = 5000;
	private static final long DEFAULT_LOGIN_TIMEOUT = 5000;
	private static final String CHANGE_OTHER = "other";

	private static final String FIELD_PORT = "port";
	private static final String FIELD_CONNECTTIMEOUT = "connectionTimeout";
	private static final String FIELD_LOGINTIMEOUT = "loginTimeout";
	private static final String FIELD_DATABASE = "database";
	private static final String FIELD_USETLS = "useTLS";
	private static final String FIELD_CHANGEPROCESS = "changeProcess";
	private static final String FIELD_MASTERACCOUNT = "otherAccount";

	/**
	 * Instance variables used in the processCredentialsVerify and
	 * processCredentialsUpdate
	 */
	private String hostname = "";
	private int port = DEFAULT_PORT;
	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
	private long loginTimeout = DEFAULT_LOGIN_TIMEOUT;
	private String database = "";
	private boolean useTLS = false;

	private String username = "";
	private String oldPassword = "";
	private String newPassword = "";
	private boolean useMaster = true;
	private TargetAccount masterAccount = null;
	private String masterUsername = "";
	private String masterPassword = "";
	private String masterDatabase = "";

	/*
	 * Constructor
	 */
	public PostgreSQL(TargetAccount targetAccount) {

		LOGGER.fine(LoggerWrapper.logMessage("postgresqlLogLevel= "+PostgreSQL.POSTGRESQL_LOGLEVEL));
		
		/* 
		 * Server attributes
		 */
		this.hostname = targetAccount.getTargetApplication().getTargetServer().getHostName();
		LOGGER.fine(LoggerWrapper.logMessage("hostname= " + this.hostname));

		/*
		 *  Application attributes
		 */
		try {
			this.port = Integer.parseUnsignedInt(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_PORT));
		} 
		catch (Exception e) {
			LOGGER.warning(LoggerWrapper.logMessage("Using default port"));
			this.port = DEFAULT_PORT;
		}
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_PORT + "= " + Integer.toString(this.port)));

		try {
			this.connectTimeout = Long.parseUnsignedLong(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_CONNECTTIMEOUT));
		} 
		catch (Exception e) {
			LOGGER.warning(LoggerWrapper.logMessage("Using default connectTimeout"));
			this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
		}
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_CONNECTTIMEOUT + "= " +Long.toString(this.connectTimeout)));

		try {
			this.loginTimeout = Long.parseUnsignedLong(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_LOGINTIMEOUT));
		} 
		catch (Exception e) {
			LOGGER.warning(LoggerWrapper.logMessage("Using default loginTimeout"));
			this.loginTimeout = DEFAULT_LOGIN_TIMEOUT;
		}
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_LOGINTIMEOUT + "= " + Long.toString(this.loginTimeout)));

		this.database = targetAccount.getTargetApplication().getExtendedAttribute(FIELD_DATABASE);
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_DATABASE + "= " + this.database));

		this.useTLS = "true".equals(targetAccount.getTargetApplication().getExtendedAttribute(FIELD_USETLS));
		LOGGER.fine(LoggerWrapper.logMessage(FIELD_USETLS + "= " + this.useTLS));

		/* 
		 * Account attributes
		 */
		this.username = targetAccount.getUserName();
		LOGGER.fine(LoggerWrapper.logMessage("username= " + this.username));

		this.newPassword = targetAccount.getPassword();
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("newPassword= " + this.newPassword));

		this.oldPassword = targetAccount.getOldPassword();
		if (this.oldPassword == null || this.oldPassword.isEmpty()) {
			LOGGER.fine(LoggerWrapper.logMessage("oldPassword is empty, set oldPassword to newPassword"));
			this.oldPassword = this.newPassword;
		}
		if (EXTENDED_DEBUG)
			LOGGER.fine(LoggerWrapper.logMessage("oldPassword= " + this.oldPassword));

		this.useMaster = CHANGE_OTHER.equals(targetAccount.getExtendedAttribute(FIELD_CHANGEPROCESS));
		if (this.useMaster) {
			this.masterAccount = targetAccount.getMasterAccount(FIELD_MASTERACCOUNT).getAsTargetAccount();
			if (this.masterAccount == null) {
				LOGGER.fine(LoggerWrapper.logMessage("No master account"));
				this.useMaster = false;
			} 
			else {
				this.masterUsername = masterAccount.getUserName();
				if (this.masterUsername == null || this.masterUsername.isEmpty()) {
					LOGGER.severe(LoggerWrapper.logMessage("masterUsername is empty"));
					this.useMaster = false;
				} 
				else {
					LOGGER.fine(LoggerWrapper.logMessage("masterUsername= " + this.masterUsername));
				}
				this.masterPassword = this.masterAccount.getPassword();
				if (this.masterPassword == null || this.masterPassword.isEmpty()) {
					LOGGER.severe(LoggerWrapper.logMessage("masterPassword is empty"));
					this.useMaster = false;
				} 
				else {
					if (EXTENDED_DEBUG)
						LOGGER.fine(LoggerWrapper.logMessage("masterPassword= " + masterPassword));
				}
			}

			this.masterDatabase= this.masterAccount.getTargetApplication().getExtendedAttribute(FIELD_DATABASE);
			LOGGER.fine(LoggerWrapper.logMessage("masterDatabase= " + this.masterDatabase));
		}
		LOGGER.fine(LoggerWrapper.logMessage("useMaster= " + this.useMaster));
	}

	/**
	 * Verifies credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 *
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while verifying the
	 *                            credential
	 *
	 */
	public void credentialVerify() throws ExtensionException {

		Connection conn = null;
		try {
			final String url = "jdbc:postgresql://" + this.hostname + ":" + Integer.toString(this.port) + "/" + this.database;
			LOGGER.fine(LoggerWrapper.logMessage("url= " + url));

			/*
			 * build connection properties with username/oldPassword
			 */
			Properties props = this.buildConnectionProperties(this.username, this.oldPassword);
			
			/*
			 * Try to open a connection
			 */
			conn = DriverManager.getConnection(url, props);
		
			/*
			 * No exception, thus username/password is correct
			 */
			LOGGER.info(LoggerWrapper.logMessage("PostgreSQL DB user '" + username + "' password verified - OK"));
		} 
		catch (Exception e) {
			/*
			 * Ups, an exception. 
			 * Password is not verified. 
			 * Handle the exception
			 */
			LOGGER.info(LoggerWrapper.logMessage("PostgreSQL DB user '" + this.username + "' password verified - Not OK"));
			this.handleExceptions(e, this.username);
		}
		finally {
			try { conn.close(); } catch (Exception e) {}
		}
	}

	/**
	 * Updates credentials against target device. Stub method should be implemented
	 * by Target Connector Developer.
	 *
	 * @param targetAccount object that contains details for the account for
	 *                      verification Refer to TargetAccount java docs for more
	 *                      details.
	 * @throws ExtensionException if there is any problem while update the
	 *                            credential
	 */
	public void credentialUpdate() throws ExtensionException {
		Connection conn = null;
		Statement stmt= null;
		String loginUsername= "";
		String loginPassword= "";
		String url;

		try {
			if (this.useMaster) {
				url = "jdbc:postgresql://" + this.hostname + ":" + Integer.toString(this.port) + "/" + this.masterDatabase;
				loginUsername = this.masterUsername;
				loginPassword = this.masterPassword;
			} 
			else {
				url = "jdbc:postgresql://" + this.hostname + ":" + Integer.toString(this.port) + "/" + this.database;
				loginUsername = this.username;
				loginPassword = this.oldPassword;
			}
			LOGGER.fine(LoggerWrapper.logMessage("url= " + url));
			LOGGER.fine(LoggerWrapper.logMessage("loginUsername= " + loginUsername));
			if (EXTENDED_DEBUG)
				LOGGER.fine(LoggerWrapper.logMessage("loginPassword= " + loginPassword));

			/*
			 * build connection properties with loginUsername/loginPassword
			 */
			Properties props = this.buildConnectionProperties(loginUsername, loginPassword);
			
			/*
			 * Get a connection
			 */
			conn= DriverManager.getConnection(url, props);
			conn.setAutoCommit(false);

			/*
			 * build and run the ALTER USER command
			 */
			stmt= conn.createStatement();

			final String query= "ALTER USER \"" + this.username + "\" PASSWORD E'" + newPassword.replace("'", "''") + "'";
			if (EXTENDED_DEBUG)
				LOGGER.fine(LoggerWrapper.logMessage("query= " + query));
			else
				LOGGER.fine(LoggerWrapper.logMessage("query= ALTER USER \"" + this.username + "\" PASSWORD E'<hidden>'"));
			
			stmt.execute(query);
			conn.commit();
			
			/*
			 * Made  it this far without exceptions --> password is updated
			 */
			LOGGER.info(LoggerWrapper.logMessage("PostgreSQL DB user '" + this.username + "' password updated - OK"));
		} 
		catch (Exception e) {
			/*
			 * Ups, an exception. 
			 * Password is not updated. 
			 * Handle the exception
			 */
			LOGGER.info(LoggerWrapper.logMessage("PostgreSQL DB user '" + this.username + "' password updated - Not OK"));
			this.handleExceptions(e, loginUsername);
		}
		finally {
			try { stmt.close(); } catch (Exception e) {}
			try { conn.close(); } catch (Exception e) {}
		}
	}

	/*
	 * Create PostgrSQL connection properties
	 */
	private Properties buildConnectionProperties(String loginUsername, String loginPassword) throws ExtensionException {
		Properties props = new Properties();

		props.setProperty("user", loginUsername);
		props.setProperty("password", loginPassword);
		props.setProperty("ssl", Boolean.toString(this.useTLS));
		if (this.useTLS) {
			// props.setProperty("sslmode","allow");
			props.setProperty("sslfactory", "org.postgresql.ssl.NonValidatingFactory");
			props.setProperty("sslmode", "require");
		}
		props.setProperty("loginTimeout",Long.toString((this.loginTimeout / 1000) > 1 ? this.loginTimeout / 1000 : 1));
		props.setProperty("connectTimeout",Long.toString((this.connectTimeout / 1000) > 1 ? this.connectTimeout / 1000 : 1));
		props.setProperty("loggerLevel", PostgreSQL.POSTGRESQL_LOGLEVEL);
		
		return props;
	}
	
	/*
	 * Deal with exceptions
	 * 
	 * @param e The exception to handle
	 * @param loginUsername Username tried for login
	 * @return
	 * @throws ExtensionException
	 */
	private void handleExceptions(Exception e, String loginUsername) throws ExtensionException {

		if (e instanceof SQLException) {
			if (e.getMessage().contains("does not exist")) {
				LOGGER.severe(LoggerWrapper.logMessage("User '" + this.username + "' not found"));
				throw new ExtensionException(PostgreSQLMessageConstants.ERR_USER_NOT_FOUND, false, this.username);
			} 
			else if (e.getMessage().contains("password authentication failed")) {
				LOGGER.severe(LoggerWrapper.logMessage("Incorrect password"));
				throw new ExtensionException(PostgreSQLMessageConstants.ERR_PASSWORD, false);
			} 
			else if (e.getMessage().contains("Check that the hostname and port are correct")) {
				LOGGER.severe(LoggerWrapper.logMessage("Connection error -- " + e.getMessage()));
				throw new ExtensionException(PostgreSQLMessageConstants.ERR_CONNECTION, false, this.hostname + ":" + Integer.toString(this.port));
			} 
			else if (e.getMessage().contains("is not permitted to log in")) {
				LOGGER.severe(LoggerWrapper.logMessage("Login not permitted for user '" + loginUsername + "'"));
				throw new ExtensionException(PostgreSQLMessageConstants.ERR_LOGIN_NOT_PERMITTED, false, loginUsername);
			} 
			else if (e.getMessage().contains("The server does not support SSL")) {
				LOGGER.severe(LoggerWrapper.logMessage("SSL not enabled on server"));
				throw new ExtensionException(PostgreSQLMessageConstants.ERR_TLS_NOT_SUPPORTED, false);
			}
		}
		
		/*
		 * something other than SQLException
		 */
		LOGGER.log(Level.SEVERE, LoggerWrapper.logMessage("Extension Exception"), e);
		throw new ExtensionException(PostgreSQLMessageConstants.ERR_EXCEPTION, false);
	}
}
