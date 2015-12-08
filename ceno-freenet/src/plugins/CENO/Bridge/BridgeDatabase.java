package plugins.CENO.Bridge;

import java.sql.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;

import java.util.List;
import java.util.ArrayList;

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
        try {
            Class.forName("org.sqlite.JDBC");
            String jdbcString = "jdbc:sqlite:"+databasePath;
            _conn = DriverManager.getConnection(jdbcString);
            CreateDatabaseStructure();
        } catch ( Exception e ) {
            throw new CENOException(CENOErrCode.LCS_DATABASE_CONNECT_FAILED, "Could not connect to the bridge database");
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
        if (_conn != null) {
            Logger.error(this, "Already connected to the database.");
        }
        //Creating the channel table
        try {
            sqlCommand = _conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS channels " +
                "(id INT PRIMARY KEY AUTOINCREMENT," +
                "privateSSK TEXT  NOT NULL," +
                "lastKnownVersion INT," +
                "lastSynced TEXT);";
            sqlCommand.executeUpdate(sql);
            sqlCommand.close();
        } catch ( Exception e ) {
            throw new CENOException(CENOErrCode.LCS_DATABASE_OPERATION_FAILED, "Failed to create channel table");
            
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
            String sql = "SELECT * FROM channels; ";

            ResultSet result = sqlCommand.executeQuery(sql);

            while ( result.next() )
                storedChannels.add(new Channel(result.getString("privateSSK"),
                                               result.getLong("lastKnownVersion")));
            
            result.close();
            sqlCommand.close();

        } catch ( Exception e ) {
            throw new CENOException(CENOErrCode.LCS_DATABASE_OPERATION_FAILED, "Failed to read the stored channel");
            
        }

        return storedChannels;
        
    }

    /**
     *   stores the given channel in the database for later retrieval
     *
     *   @param new channel ssk
     *   @param provided edition
     */
    public void storeChannel(String insertSSK, Long providedEdition) throws CENOException
    {
        Statement sqlCommand;
        try {
            sqlCommand = _conn.createStatement();
            
            String sql = "INSERT INTO  channels (privateSSK,lastKnownVersion) " +
                "VALUES (" + insertSSK+ ", " + String.valueOf(providedEdition) + ");"; 

            sqlCommand.executeUpdate(sql);
            sqlCommand.close();
            _conn.commit();

        } catch ( Exception e ) {
            throw new CENOException(CENOErrCode.LCS_DATABASE_OPERATION_FAILED, "Failed to store channel");
            
        }

    }


}
