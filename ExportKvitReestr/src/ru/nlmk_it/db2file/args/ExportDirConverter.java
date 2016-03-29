/**
 * 
 */
package ru.nlmk_it.db2file.args;

import java.io.File;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * @author Косых Евгений
 *
 */
public final class ExportDirConverter implements IStringConverter<File> {

	@Override
	public File convert(String value) {
		
		File result = new File(value);
		
		if (!result.isDirectory()) {
			throw new ParameterException(value + ": directory does not exist.");
		}
		
		return result;
	}

}
