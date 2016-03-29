/**
 * 
 */
package ru.nlmk_it.db2file.args;

import java.sql.Date;
import java.text.ParseException;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

/**
 * 
 * @author kosyh_ev
 *
 */
public class PdatConverter implements IStringConverter<Date> {

	@Override
	public Date convert(String arg0) {
		
		try {
			long time = Arguments.dateFormat.parse(arg0).getTime();
			return new Date(time);
		}
		catch (ParseException e) {
			throw new ParameterException(e);
		}
	}

}
