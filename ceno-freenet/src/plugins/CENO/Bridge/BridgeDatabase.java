package plugins.CENO.Bridge;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import plugins.CENO.CENOErrCode;
import plugins.CENO.CENOException;
import plugins.CENO.Bridge.Signaling.Channel;
import freenet.support.Logger;

public class BridgeDatabase
{

	//String databasePath;
	Connection _conn = null;
	/**
	 * Constructor 
	 * 
	 * It opens the sqlite bridge database, creates it if it doesn't exists.
	 * then create the table structure if doesn't exists
	 *
	 * @param databasePath is the path ot the file which stores sqlite
	 *        database
	 */
	public BridgeDatabase(String databasePath) throws CENOException
	//: databasePath(databasePath)
	{
		//Sanity check
		if (_conn != null) {
			Logger.error(this, "Already connected to the database.");
		} else {
			try {
				Class.forName("org.sqlite.JDBC");
				String jdbcString = "jdbc:sqlite:"+databasePath;
				_conn = DriverManager.getConnection(jdbcString);
				CreateDatabaseStructure();
			} catch ( Exception e ) {
				throw new CENOException(CENOErrCode.LCS_DATABASE_CONNECT_FAILED, "Could not connect to the bridge database");
			}
		}
	}

	/**
	 * Called by constructors Creates all the necessary tables in case 
	 * they do not exsit.
	 *
	 */
	public void CreateDatabaseStructure() throws CENOException {
		Statement sqlCommand = null;
		//Sanity check
		if (_conn == null) {
			Logger.error(this, "Not connected to the database yet.");
			throw new CENOException(CENOErrCode.RR_DATABASE_OPERATION_FAILED, "Failed to create channel table");
		}
		//Creating the channel table
		try {
			sqlCommand = _conn.createStatement();
			String sqlString = "CREATE TABLE IF NOT EXISTS channels " +
					"(privateSSK TEXT PRIMARY KEY," +
					"lastKnownVersion INT DEFAULT 0," +
					"lastSynced INT DEFAULT CURRENT_TIMESTAMP);";
			sqlCommand.executeUpdate(sqlString);
			sqlCommand.close();
		} catch ( Exception e ) {
			throw new CENOException(CENOErrCode.RR_DATABASE_OPERATION_FAILED, "Failed to create channel table");

		}

	}

	/**
	 *   reads the list of the registered channels from the channel table 
	 *
	 *   @return list of channels
	 */
	public List<Channel> retrieveChannels() throws CENOException
	{
		Statement sqlCommand;
		List<Channel> storedChannels = new ArrayList<Channel>();

		try {
			sqlCommand = _conn.createStatement();
			String sqlString = "SELECT * FROM channels; ";

			ResultSet result = sqlCommand.executeQuery(sqlString);

			while ( result.next() ) {
				try {
					storedChannels.add(new Channel(result.getString("privateSSK"),
							result.getLong("lastKnownVersion"), result.getLong("lastSynced")));
				} catch ( Exception e ) {
					Logger.error(this, "failed to add a channel for a stored SSK");
				}
			}
			result.close();
			sqlCommand.close();

		} catch ( Exception e ) {
			Logger.error(this, "failed to access the stored channel table");
			throw new CENOException(CENOErrCode.RR_DATABASE_OPERATION_FAILED, "Failed to read the stored channel");

		}

		return storedChannels;

	}

	/**
	 *   stores the given channel in the database for later retrieval
	 *
	 *   @param new channel ssk
	 *   @param provided edition
	 */
	public void storeChannel(String insertSSK, Long providedEdition, Long lastSynced) throws CENOException
	{
		Statement sqlCommand;
		try {
			sqlCommand = _conn.createStatement();

			String sqlString = "INSERT OR REPLACE INTO  channels (privateSSK,lastKnownVersion,lastSynced) " +
					"VALUES ('" + insertSSK+ "', " + providedEdition + ", " + lastSynced + ");"; 

			sqlCommand.executeUpdate(sqlString);
			sqlCommand.close();
			//_conn.commit(); auto commit is enabled by default

		} catch ( Exception e ) {
			Logger.error(this,"Failed to store channel");
			//throw new CENOException(CENOErrCode.LCS_DATABASE_OPERATION_FAILED, "Failed to store channel");

		}

	}

	public void storeChannel(Channel channel) throws CENOException {
		storeChannel(channel.getInsertSSK(), channel.getLastKnownEdition(), channel.getLastSynced());
	}


}
