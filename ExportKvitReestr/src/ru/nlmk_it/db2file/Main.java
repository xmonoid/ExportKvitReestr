/**
 * 
 */
package ru.nlmk_it.db2file;

import java.util.Arrays;

import org.apache.log4j.Logger;

import ru.nlmk_it.db2file.args.Arguments;

import com.beust.jcommander.JCommander;


/**
 * @author kosyh_ev
 *
 */
public final class Main {

	public static final Logger logger = Logger.getLogger(Main.class);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		logger.info("Program is started with arguments: " + Arrays.toString(args));
		try {
			
			Arguments arguments = new Arguments();
			
			JCommander commander = new JCommander(arguments);
			
			
			commander.parse(args);
			
			if (arguments.isHelp()) {
				StringBuilder stringBuilder = new StringBuilder();
				commander.usage(stringBuilder);
				logger.info(stringBuilder);
				return;
			}
			
			Reestr reestr = new Reestr(arguments);
			
			try {
				reestr.execute();
			}
			finally {
				reestr.close();
			}
		}
		catch (Throwable t) {
			logger.fatal("Fatal error: " + t.getMessage());
			t.printStackTrace();
		}
		finally {
			logger.info("Program is ended.");
		}
	}

}
