package plugins.CeNo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import freenet.support.Logger;

public class Configuration {
	/** The default location for the configuration file (~/.CeNo/ceno.properties) */
	private static final String defaultConfigLocation = System.getProperty("user.home") + "/.CeNo/config.properties";
	private String customConfigLocation;

	/**
	 * Properties instance to keep configuration properties during the execution of CeNo.
	 * When calling {{@link #storeProperties()}, the current values of this variable
	 * get stored in the configuration file in use.
	 */
	private Properties properties;

	/**
	 * Constructs a configuration file at the default location {@link #defaultConfigLocation}
	 * 
	 */
	public Configuration() {
		this(defaultConfigLocation);
	}

	/**
	 * Creates a configuration object corresponding to a file at a custom location
	 * 
	 * @param customConfigLocation the custom location
	 */
	public Configuration(String customConfigLocation) {
		this.customConfigLocation = customConfigLocation;
		properties = new Properties();
	}

	/**
	 * Tests whether the configuration file actually exists.
	 * 
	 * @return <code>true</code> if and only if the configuration file exists;
	 * <code>false</code> otherwise
	 */
	public boolean configExists() {
		try {
			return new File(customConfigLocation).exists();
		} catch(Exception e) {
			return false;
		}
	}

	private boolean createConfigFile(String configFilePath) {
		File configFile = new File(configFilePath);
		try {
			configFile.getParentFile().mkdirs();
			if (configFile.createNewFile()) {
				return true;
			}
		} catch (IOException e) {
			return false;
		} catch (SecurityException e) {
			return false;
		}
		return true;
	}

	/**
	 * Store into {@link #properties} the values of the configuration file.
	 * If the configuration file does not exist, create it.
	 * 
	 * @return <code>true</code> if configuration file was read, or if the configuration file
	 * didn't exist and got successfully created; <code>false</code> otherwise
	 */
	public boolean readProperties() {
		if (!configExists()) {
			if (createConfigFile(customConfigLocation)) {
				return true;
			} else {
				Logger.warning(this, "CeNo plugin could not create configuration file.");
				return false;
			}
		}
		FileInputStream configFileIn;
		try {
			configFileIn = new FileInputStream(customConfigLocation);
		} catch (FileNotFoundException e) {
			return false;
		}
		try {
			properties.load(configFileIn);
			configFileIn.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Sets/updates the value of a property
	 * Properties are not saved in the configuration file until {@link #storeProperties()}
	 * is called.
	 * 
	 * @param key the key of the property
	 * @param value the new value to be set
	 */
	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	/**
	 * Stores the values of {@link #properties} in the configuration file. If a property with
	 * the same key exists, updates its value. Properties in the configuration file that
	 * do not exist in the {@link #properties} variable do not get updated or removed.
	 * 
	 * @return <code>true</code> if storing properties to the configuration file was successful,
	 * <code>false</code> otherwise
	 */
	public boolean storeProperties() {
		try {
			FileOutputStream configFileOut = new FileOutputStream(customConfigLocation);
			properties.store(configFileOut, null);
			configFileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
