/**
 * 
 */
package ru.nlmk_it.db2file.args;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * @author kosyh_ev
 *
 */
public final class Arguments {
	
	private static final Logger logger = Logger.getLogger(Arguments.class);
	
	@Parameter(names="-help",
			description="Печать справки и завершение работы программы",
			help=true)
	private boolean help;

	@Parameter(names="-pdat",
			description="Дата, на которую формируется реестр квитанций",
			required=true,
			converter=PdatConverter.class)
	private Date pdat;
	
	@Parameter(names="-bd_lesk",
			description="Район, по которому формируется реестр квитанций",
			required=true)
	private String bd_lesk;
	
	@Parameter(names="-query",
			description="Файл с SQL сценарием",
			required=true,
			converter=SourceSQLScriptConverter.class)
	private File sourceSQLScript;
	
	@Parameter(names="-mkd",
			description="Если указан, выгружаются МКД, в противном случае частный сектор")
	private boolean isMkd;
	
	@Parameter(names="-pleskgesk",
			description="The name of company (e.g. LESK, GESK)",
 		    required=true)
	private String pleskgesk;
	
	@Parameter(names="-export_dir",
			description="Каталог, в который экспортируются файлы",
			required=false,
			converter=ExportDirConverter.class)
	private File exportDir = new File("./export/");
	
	@Parameter(names="-puse_filter",
    		description="Set this to 1 if you want to use filter table or 0 otherwise",
    		required=false)
    private String use_filter = "0";
	
	@Parameter(names="-pmkd_id",
			description="ID одтельного МКД",
			required=false)
	private String mkd_id = "-1";
	
	static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
	
	/**
	 * Проверка указания опции справки. Указание данной опции выводит в стандартный поток вывода
	 * справку по всему доступному набору опций и прекращает работу программы.
	 * @return true, если опция справки указана, false иначе.
	 */
	public boolean isHelp() {
		logger.trace("Invoke isHelp()");
		logger.trace("Return value -> " + help);
		return help;
	}
	
	/**
	 * Дата, на которую необходима выгрузка реестра сверки.
	 * @return Дата в формате {@link java.sql.Date}
	 */
	public Date getPdat() {
		logger.trace("Invoke getPdat()");
		logger.trace("Return value -> " + dateFormat.format(pdat));
		return pdat;
	}
	
	/**
	 * Район, по которому производится выгрузка реестра.
	 * @return Код района в формате 3200xx.
	 */
	public String getBdLesk() {
		logger.trace("Invoke getBdLesk()");
		logger.trace("Return value -> " + bd_lesk);
		return bd_lesk;
	}
	
	/**
	 * Таблица, хранящая идентификаторы абонентов, по которым нужно выгрузить реестры.
	 * Если значение установлено в {@code empty}, то фильтр не применяется.
	 * @return Наименование таблицы-фильтра
	 */
	public String getPuse_filter() {
		logger.trace("Invoke getTableFilter()");
		logger.trace("Return value -> " + use_filter);
		return use_filter;
	}
	
	/**
	 * Файл, в котором расположены запросы на получение необходимой выборки данных.
	 * При этом файла может и не существовать. 
	 * @return Ссылка на файл типа {@link java.io.File} с набором необходимых запросов.
	 */
	public File getSourceSQLScript() {
		logger.trace("Invoke getSourceSQLScript()");
		logger.trace("Return value -> " + sourceSQLScript.getAbsolutePath());
		return sourceSQLScript;
	}
	
	/**
	 * Определяет отношение выгружаемых квитанций к МКД.
	 * @return {@code true}, если выгружаются МКД, {@code false}, если частный сектор.
	 */
	public boolean isMkd() {
		logger.trace("Invoke isMkd()");
		logger.trace("Return value -> " + isMkd);
		return isMkd;
	}
	
	/**
	 * 
	 * @return
	 */
	public String getMkdId() {
		logger.trace("Invoke getMkdId()");
		logger.trace("Return value -> " + mkd_id);
		return mkd_id;
	}
	
	/**
	 * Организация (ЛЭСК или ГЭСК)
	 * @return
	 */
	public String getPleskgesk() {
		logger.trace("Invoke getPleskgesk()");
		logger.trace("Return value -> " + pleskgesk);
		return pleskgesk;
	}
	
	/**
	 * Каталог, в который должны сохраняться создаваемые файлы.
	 * @return
	 */
	public File getExportDir() {
		logger.trace("Invoke getExportDir()");
		logger.trace("Return value -> " + exportDir.getAbsolutePath());
		return exportDir;
	}
	
	/**
	 * Проверка коррестности указанных в списке параметров данных.
	 * @throws IOException Возникеат, если не существует файла со сценарием выборки, либо не существует
	 * директории, в которой будет расположен файл экспорта.
	 * @throws SecurityException Возникает, если нет доступа к файлу со сценарием или к экспортной директории.
	 */
	public void validate() throws IOException, SecurityException {
		
		if (!sourceSQLScript.isFile()) {
			throw new FileNotFoundException("File not found: " + sourceSQLScript.getCanonicalPath());
		}
		else if (!sourceSQLScript.canRead()) {
			throw new SecurityException("Access denied: " + sourceSQLScript.getAbsolutePath());
		}
		
		if (pdat == null) {
			throw new ParameterException("Parameter pdat does not a value");
		}
		
		if (bd_lesk == null) {
			throw new ParameterException("Parameter r_on does not a value");
		}
	}
	
	@Override
	public String toString() {
		return null;
	}
}
