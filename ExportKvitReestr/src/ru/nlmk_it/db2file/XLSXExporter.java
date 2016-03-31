/**
 * 
 */
package ru.nlmk_it.db2file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import oracle.jdbc.OraclePreparedStatement;

/**
 * @author kosyh_ev
 *
 */
public final class XLSXExporter {
	
	private static final Logger logger = Logger.getLogger(XLSXExporter.class);
	
	private final String date;
	
	private final boolean isMkd;
	
	private final File exportDir;

	/**
	 * 
	 * @param exportFile
	 * @param template
	 * @throws IOException
	 */
	public XLSXExporter(String date, boolean isMkd, File exportDir) throws IOException {
		logger.trace("Invoke the constructor XLSXExporter(" + date + ")");
		assert date != null : "Date is null";
		this.date = date;
		this.isMkd = isMkd;
		this.exportDir = exportDir;
	}
	

	
	/**
	 * This method creates the export file.
	 * @param date Selection date in format MM.yyyy.
	 * @param bd_lesk Database code (01 - 37).
	 * @param toSeparate 
	 * @return The created file.
	 * @throws IOException Exception when working with the file system.
	 */
	private File createNextExportFile(boolean toSeparate, String bd_lesk) throws IOException {
		logger.trace("Invoke createExportFile()");
		
		if (toSeparate) {
			// Export filename is 'reesrt_MM.yyyy_XX.xlsx', where MM is month, yyyy is year, and XX is the database code (01 - 37).
			File exportFile = new File( exportDir.getAbsolutePath() + "/reestr_" + date + "_" + bd_lesk + ".xlsx");
			
			int i = 1;
			while (exportFile.exists()) {
				logger.debug("File " + exportFile.getName() +" is already exists.");
				exportFile = new File(exportDir.getAbsolutePath() + "/reestr_" + date + "_" + bd_lesk + "_" + i++ + ".xlsx");
				logger.debug(" Create an other file: " + exportFile.getName());
			}
			// Copy template file to export folder.
			FileUtils.copyFile(new File("./etc/template.xlsx"), exportFile);
			
			logger.trace("createExportFile() returned" + exportFile.getCanonicalPath());
			return exportFile;
		}
		else {
			// Export filename is 'reesrt_MM.yyyy_XX.xlsx', where MM is month, yyyy is year, and XX is the database code (01 - 37).
			File exportFile = new File(exportDir.getAbsolutePath() + "/reestr_" + date + "_full.xlsx");
			// Copy template file to export folder.
			FileUtils.copyFile(new File("./etc/template.xlsx"), exportFile);
			logger.trace("createExportFile() returned" + exportFile.getCanonicalPath());
			return exportFile;
		}
	}

	
	/**
	 * Запуск процесса экспорта данных.
	 * @param areaQuery Запрос на получение заголовка таблицы.
	 * @param bodySet Результат запроса на получение тела таблицы.
	 * @param separateNumber Количество строк, по превышении которого файл реестра разделется (с доведением до конца
	 * текущей улицы). Если параметр установлен в 0 или в отрицательное значение, то разделения не происходит.
	 * @throws SQLException Выбрасывается при возникновении ошибки работы с БД.
	 * @throws IOException Выбрасывается при возникновении ошибки работы с файлом.
	 */
	public void export(Connection connection, String areaQuery, ResultSet bodySet,
					   int separateNumber, boolean theFilterIsUsing) throws SQLException, IOException {
		logger.trace("Invoke export(ResultSet areaSet, ResultSet bodySet)");
		assert areaQuery != null : "The query for area is null";
		assert bodySet != null : "The ResultSet for body is null";
		
		boolean resultHasLines = bodySet.first();
		
		String company = getCompany(connection, areaQuery,
						(theFilterIsUsing && separateNumber == 0 || !resultHasLines) ?
								"00" : bodySet.getString("BD_LESK"));
		
		BigDecimal countAll = BigDecimal.ZERO;
		
		boolean stillHaveKvitances = true;
		while(stillHaveKvitances) {
			
			File currentExportFile = createNextExportFile(separateNumber > 0, resultHasLines ? bodySet.getString("BD_LESK") : "00");
			InputStream in = new FileInputStream(currentExportFile);
			OutputStream out = null;
			Workbook workbook = new XSSFWorkbook(in);
			try {
				
				Sheet sheet = workbook.getSheetAt(0);
				int mergedRowNum = 0;
				
				// Перебор строк
				Iterator<Row> rows = sheet.iterator();
				while (rows.hasNext()) {
					
					// Перебор ячеек
					Row row = rows.next();
					int rowNum = row.getRowNum();
					Iterator<Cell> cells = row.cellIterator();
					cell_iterator:
					while (cells.hasNext()) {
						Cell cell = cells.next();
						
						switch (cell.getCellType()) {
						case Cell.CELL_TYPE_STRING:
							String value = cell.getStringCellValue();
							
							// Заголовок таблицы.
							if (value.equalsIgnoreCase("<COMPANY>")) {
								logger.trace("Set cell value COMPANY: " + company);
								cell.setCellValue(company);
							}
							if (value.equalsIgnoreCase("<IS_MKD>")) {
								String text = isMkd ? "Многоквартирные дома" : "Индивидуальные дома";
								logger.trace("Set cell value IS_MKD: " + text);
								cell.setCellValue(text);
							}
							// Тело таблицы.
							if (value.equalsIgnoreCase("<BODY>")) {
								CellStyle cellStyle = cell.getCellStyle();
								double countKvit = 0;
								if (!resultHasLines) {
									setRow(workbook,
											sheet,
											cellStyle,
											rowNum++, 
											null,
											0,
											0,
											0);
								} else {
									
									if (separateNumber > 0) {
										String bd_lesk = bodySet.getString("BD_LESK");
										String postal = bodySet.getString("POSTAL");
										
										// Реестр разделяется на файлы по separateNumber + ε строк.
										next_reestr:
										do {
											if (!bd_lesk.equalsIgnoreCase(bodySet.getString("BD_LESK")) 
													|| !postal.equalsIgnoreCase(bodySet.getString("POSTAL"))) {
												company = getCompany(connection, areaQuery, bodySet.getString("BD_LESK"));
												break next_reestr;
											}
											
											countKvit += setRow(workbook,
																sheet,
																cellStyle,
																rowNum++,
																bodySet.getString("ADDRESS"),
																bodySet.getBigDecimal("MIN").doubleValue(),
																bodySet.getBigDecimal("MAX").doubleValue(),
																bodySet.getBigDecimal("COUNT").doubleValue());
										} while (countKvit < separateNumber & bodySet.next());
									}
									else {
										// Реестр пишется в один файл.
										do {
											
											countKvit += setRow(workbook, 
																sheet, 
																cellStyle, 
																rowNum++, 
																bodySet.getString("ADDRESS"),
																bodySet.getBigDecimal("MIN").doubleValue(),
																bodySet.getBigDecimal("MAX").doubleValue(),
																bodySet.getBigDecimal("COUNT").doubleValue());
										} while (bodySet.next());
									}
								}
								
								countAll = new BigDecimal(countKvit);
								
								// Exit loop if no more lines in ResultSet.
								stillHaveKvitances = resultHasLines && !bodySet.isAfterLast();
								
								
								sheet.removeRow(sheet.getRow(rowNum + 1));
								rows = sheet.iterator();

								break cell_iterator;
							}
							// Подвал таблицы.
							else if (value.equalsIgnoreCase("<COUNT_ALL>")) {
								cell.setCellValue(countAll.doubleValue());
								logger.trace("Set cell value COUNT_ALL: " + countAll);
							}
							else if (value.equalsIgnoreCase("<MERGE>")) {
								mergedRowNum = rowNum;
							}
						}
					}
				}
				// Склеиваем ячейки в подвале.
				sheet.addMergedRegion(new CellRangeAddress(mergedRowNum, mergedRowNum, 1, 3));
			}
			finally {
				in.close();
				out = new FileOutputStream(currentExportFile);
				workbook.write(out);
				if (out != null) {
					out.close();
				}
			}
		}
	}
	
	
	/**
	 * Запись строки в реестр
	 * @param workbook Рабочая книга
	 * @param worksheet Рабочая страница
	 * @param cellStyle Стиль ячеек
	 * @param rowNum Номер строки
	 * @param address Адрес
	 * @param min Минимальный порядковый номер квитанции
	 * @param max Максимальный порядковый номер квитанции
	 * @param count Количество квитанций в данной улице
	 * @return Количество квитанций в данной улице
	 */
	private double setRow(Workbook workbook,
									Sheet worksheet,
									CellStyle cellStyle,
									int rowNum, 
									String address,
									double min,
									double max,
									double count) {
		copyRow(workbook, worksheet, rowNum + 1, rowNum + 2);
		
		Row newRow = worksheet.createRow(rowNum);
		
		Cell newCell = newRow.createCell(1);
		newCell.setCellStyle(cellStyle);
		newCell.setCellValue(address);
		logger.trace("Set cell value ADDRESS: " + address);
		
		newCell = newRow.createCell(2);
		newCell.setCellStyle(cellStyle);
		newCell.setCellValue(min);
		logger.trace("Set cell value MIN: " + min);
		
		newCell = newRow.createCell(3);
		newCell.setCellStyle(cellStyle);
		newCell.setCellValue(max);
		logger.trace("Set cell value MAX: " + max);
		
		newCell = newRow.createCell(4);
		newCell.setCellStyle(cellStyle);
		newCell.setCellValue(count);
		logger.trace("Set cell value COUNT: " + count);
		return count;
	}
	
	
	/**
	 * Код метода спизжен со StackOverflow. Копирует содержимое одной строки в другую.
	 * @param workbook Рабочая книга.
	 * @param worksheet Рабочий лист.
	 * @param sourceRowNum Номер копируемой строки.
	 * @param destinationRowNum Номер строки назначения.
	 */
	private static void copyRow(Workbook workbook, Sheet worksheet, int sourceRowNum, int destinationRowNum) {
        // Get the source / new row
        Row newRow = worksheet.getRow(destinationRowNum);
        Row sourceRow = worksheet.getRow(sourceRowNum);

        // If the row exist in destination, push down all rows by 1 else create a new row
        if (newRow != null) {
            worksheet.shiftRows(destinationRowNum, worksheet.getLastRowNum(), 1);
        } else {
            newRow = worksheet.createRow(destinationRowNum);
        }

        // Loop through source columns to add to new row
        for (int i = 0; i < sourceRow.getLastCellNum(); i++) {
            // Grab a copy of the old/new cell
            Cell oldCell = sourceRow.getCell(i);
            Cell newCell = newRow.createCell(i);

            // If the old cell is null jump to next cell
            if (oldCell == null) {
                newCell = null;
                continue;
            }

            // Copy style from old cell and apply to new cell
            CellStyle newCellStyle = workbook.createCellStyle();
            newCellStyle.cloneStyleFrom(oldCell.getCellStyle());
            newCell.setCellStyle(newCellStyle);

            // If there is a cell comment, copy
            if (oldCell.getCellComment() != null) {
                newCell.setCellComment(oldCell.getCellComment());
            }

            // If there is a cell hyperlink, copy
            if (oldCell.getHyperlink() != null) {
                newCell.setHyperlink(oldCell.getHyperlink());
            }

            // Set the cell data type
            newCell.setCellType(oldCell.getCellType());

            // Set the cell data value
            switch (oldCell.getCellType()) {
                case Cell.CELL_TYPE_BLANK:
                    newCell.setCellValue(oldCell.getStringCellValue());
                    break;
                case Cell.CELL_TYPE_BOOLEAN:
                    newCell.setCellValue(oldCell.getBooleanCellValue());
                    break;
                case Cell.CELL_TYPE_ERROR:
                    newCell.setCellErrorValue(oldCell.getErrorCellValue());
                    break;
                case Cell.CELL_TYPE_FORMULA:
                    newCell.setCellFormula(oldCell.getCellFormula());
                    break;
                case Cell.CELL_TYPE_NUMERIC:
                    newCell.setCellValue(oldCell.getNumericCellValue());
                    break;
                case Cell.CELL_TYPE_STRING:
                    newCell.setCellValue(oldCell.getRichStringCellValue());
                    break;
            }
        }

        // If there are are any merged regions in the source row, copy to new row
        for (int i = 0; i < worksheet.getNumMergedRegions(); i++) {
            CellRangeAddress cellRangeAddress = worksheet.getMergedRegion(i);
            if (cellRangeAddress.getFirstRow() == sourceRow.getRowNum()) {
                CellRangeAddress newCellRangeAddress = new CellRangeAddress(newRow.getRowNum(),
                        (newRow.getRowNum() +
                                (cellRangeAddress.getLastRow() - cellRangeAddress.getFirstRow()
                                        )),
                        cellRangeAddress.getFirstColumn(),
                        cellRangeAddress.getLastColumn());
                worksheet.addMergedRegion(newCellRangeAddress);
            }
        }
    }
	
	/**
	 * Вычленение наименования обслуживающей организации (первый запрос)
	 * @param resultSet Результат запроса
	 * @return Строковое наименование организации.
	 * @throws SQLException
	 */
	private String getCompany(Connection connection, String areaQuery, String bd_lesk) throws SQLException {
		logger.trace("Invoke getCompany(String, String)");
		
		OraclePreparedStatement areaStatement = (OraclePreparedStatement) connection.prepareStatement(areaQuery, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		if (areaQuery.contains(":pdat")) {
			try {
				areaStatement.setDateAtName("pdat", new java.sql.Date(new SimpleDateFormat("").parse(date).getTime()));
			}
			catch (ParseException e) {/*NOP*/}
		}
		if (areaQuery.contains(":bd_lesk")) {
			areaStatement.setStringAtName("bd_lesk", bd_lesk);
		}
		ResultSet areaSet = areaStatement.executeQuery();
		
		String result = null;
		if (areaSet.first()) {
			if ((result = areaSet.getString("COMPANY")) == null) {
				throw new SQLException("Отсутствует район с введённым кодом БД.");
			}
		} else {
			throw new SQLException("Отсутствует район с введённым кодом БД.");
		}
		areaSet.close();
		
		logger.trace("Company is " + result);
		return result;
	}
}
