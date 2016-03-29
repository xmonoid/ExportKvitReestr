/**
 * 
 */
package ru.nlmk_it.db2file.args;

import java.io.File;

import com.beust.jcommander.IStringConverter;

/**
 * @author kosyh_ev
 *
 */
public class SourceSQLScriptConverter implements IStringConverter<File> {

	@Override
	public File convert(String arg0) {
		return new File(arg0);
	}

}
