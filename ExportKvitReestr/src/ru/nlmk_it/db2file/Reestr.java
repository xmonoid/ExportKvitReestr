/**
 * 
 */
package ru.nlmk_it.db2file;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Properties;

import oracle.jdbc.OraclePreparedStatement;

import org.apache.log4j.Logger;

import ru.nlmk_it.db2file.args.Arguments;

/**
 * 
 * @author kosyh_ev
 *
 */
public final class Reestr {
	
	private static final Logger logger = Logger.getLogger(Reestr.class);
	
	// Чтение параметров из файла конфигурации.
	static {
		Properties properties = new Properties();
		InputStream in = null;
		try {
			in = new FileInputStream("./etc/parameters.property");
			properties.load(in);
			
			String url = properties.getProperty("url");
			String login = properties.getProperty("login");
			String password = properties.getProperty("password");
			connection = DriverManager.getConnection(url, login, password);
			logger.info("Database connection created.");
			logger.debug("Database connection created: \n" + "\turl = " + url + "\n\tlogin = " + login);
			in.close();
		}
		catch (IOException e) {
			logger.fatal(e);
			throw new RuntimeException(e);
		}
		catch (SQLException e) {
			logger.fatal(e);
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (in != null) {
					in.close();
				}
			}
			catch (IOException e) {
				logger.error(e);
			}
		}
	}
	
	// Подключение к БД.
	private static Connection connection;
	
	// Параметры, переданные программе.
	private Arguments arguments;

	/**
	 * 
	 * @param arguments
	 * @throws SQLException
	 * @throws IOException
	 */
	public Reestr(Arguments arguments) throws SQLException, IOException {
		logger.trace("Invoke the constructor Reestr(" + arguments + ")");
		assert arguments != null : "Arguments is null";
		arguments.validate();
		this.arguments = arguments;
		logger.trace("Create an object: " + this);
	}
	
	
	/**
	 * 
	 * @throws IOException
	 * @throws SQLException
	 */
	public void execute() throws IOException, SQLException {
		logger.trace("Invoke execute()");
		
		Queries queries = new Queries(arguments.getSourceSQLScript());
		
		// Извлекаем заголовок.
		String areaQuery = queries.getAreaQuery();
		
		// Извлекаем тело таблицы.
		String bodyQuery = queries.getBodyQuery();
		
		OraclePreparedStatement bodyStatement = (OraclePreparedStatement) connection.prepareStatement(bodyQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		
		boolean theFilterIsUsing = arguments.getPuse_filter().equals("1");
		
		if (bodyQuery.contains(":pdat")) {
			bodyStatement.setDateAtName("pdat", arguments.getPdat());
		}
		if (bodyQuery.contains(":bd_lesk")) {
			bodyStatement.setStringAtName("bd_lesk", arguments.getBdLesk());
		}
		if (bodyQuery.contains(":pleskgesk")) {
			bodyStatement.setStringAtName("pleskgesk", arguments.getPleskgesk());
		}
		if (bodyQuery.contains(":use_filter")) {
			bodyStatement.setFixedCHARAtName("use_filter", arguments.getPuse_filter());
		}
		if (bodyQuery.contains(":mkd_id")) {
			bodyStatement.setFixedCHARAtName("mkd_id", arguments.getMkdId());
		}
		
		ResultSet bodySet = bodyStatement.executeQuery();
		
		// Выполняем экспорт.
		XLSXExporter exporter = new XLSXExporter(new SimpleDateFormat("MM.yyyy").format(arguments.getPdat()), arguments.isMkd(), arguments.getExportDir());
		exporter.export(connection, areaQuery, bodySet, 1000, theFilterIsUsing);
		
		//if (!theFilterIsUsing) {
			exporter.export(connection, areaQuery, bodySet, 0, theFilterIsUsing);
		//}
		
		bodyStatement.close();
		logger.trace("execute() ended.");
	}
	
	
	/**
	 * 
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		logger.trace("Invoke close()");
		connection.close();
		logger.info("Database connection closed.");
	}
}
