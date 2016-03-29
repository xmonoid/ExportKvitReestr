/**
 * 
 */
package ru.nlmk_it.db2file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * @author kosyh_ev
 *
 */
public final class Queries {
	
	private static final Logger logger = Logger.getLogger(Queries.class);
	
	/**
	 * Запрос для заголовка.
	 */
	private String areaQuery;
	
	/**
	 * Запрос для таблицы.
	 */
	private String bodyQuery;

	public Queries(File fileWithQueries) throws IOException, SQLException {
		logger.trace("Invoke the constructor Queries(" + fileWithQueries.getCanonicalPath() + ")");
		assert fileWithQueries != null;
		StringBuilder fileData = new StringBuilder();
		
		// Чтение файла.
        BufferedReader reader = new BufferedReader(new FileReader(fileWithQueries));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        
        // Построение сценария.
		this.prepareScript(fileData);
		
		// Сценарий должен состоять из тре
		String[] queries = fileData.toString().split(";");
		if (queries.length < 2) {
			throw new SQLException("Невалидный сценарий!");
		}
		areaQuery = queries[0].trim();
		bodyQuery = queries[1].trim();
	}
	
	/**
	 * Подготовка сценария к выполнению. Удаление комментариев, TODO: извлечение переменных.
	 * @param script сценарий, считанный из файла.
	 * @throws SQLException Выскакивает, если сценарий невалидный.
	 */
	private void prepareScript(StringBuilder script) throws SQLException {
		logger.trace("Invoke setScript(" + script + ")");
		
		// Удаляем комментарии
		char[] symbol = script.toString().toCharArray();
		boolean isOneLineComment = false;
		boolean isMultLineComment = false;
		boolean isString = false;
		boolean isAlias = false;
		int beginThis = 0;
		int alreadyDeleted = 0;
		for (int i = 1; i < symbol.length; i++) {
			
			if (!isOneLineComment) {
				if (!isMultLineComment) {
					if (!isString) {
						if (!isAlias) {
							if (symbol[i-1] == '-' && symbol[i] == '-') {
								beginThis = i - 1;
								isOneLineComment = true;
							}
							else if (symbol[i-1] == '/' && symbol[i] == '*') {
								beginThis = i - 1;
								isMultLineComment = true;
							}
							else if (symbol[i] == '\'') {
								beginThis = i;
								isString = true;
							}
							else if (symbol[i] == '"') {
								beginThis = i;
								isAlias = true;
							}
							else if (symbol[i] == '&') {
								script.setCharAt(i - alreadyDeleted, ':');
							}
						}
						// Алиас
						else {
							if (symbol[i] == '"') {
								isAlias = false;
							}
						}
					}
					// Строка
					else {
						if (symbol[i] == '\'') {
							isString = false;
						}
					}
				}
				// Многострочный комментарий.
				else {
					
					if (symbol[i-1] == '*' && symbol[i] == '/') {
						int start = script.indexOf("/*", beginThis - alreadyDeleted);
						int end = script.indexOf("*/", start);
						script.delete(start, end + 2);
						alreadyDeleted += end - start + 2;
						isMultLineComment = false;
					}
				}
				
			}
			// Однострочный комментарий.
			else {
				
				if (symbol[i] == '\n') {
					int start = script.indexOf("--", beginThis - alreadyDeleted);
					int end = script.indexOf("\n", start);
					script.delete(start, end + 1);
					alreadyDeleted += end - start + 1;
					isOneLineComment = false;
				}
			}
		}
		
		
	}
	
	
	public String getAreaQuery() {
		return areaQuery;
	}
	
	public String getBodyQuery() {
		return bodyQuery;
	}
}
