package com.github.boly38.mongodump;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.security.InvalidParameterException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.github.boly38.mongodump.domain.BackupConfiguration;
import com.github.boly38.mongodump.domain.RestoreConfiguration;
import com.github.boly38.mongodump.domain.hostconf.IMongoServerHostConfiguration;
import com.github.boly38.mongodump.domain.hostconf.MongoServerDefaultHostConfiguration;
import com.github.boly38.mongodump.services.impl.MongodumpServiceImpl;

import lombok.extern.slf4j.Slf4j;

/**
 * Specifications
 * 
 * console arg
 * -d [backup directory]
 * -n [database name]
 * -a [BACKUP|RESTORE]
 *
 */
@Slf4j
public class Main {
    private static final Options OPTIONS = new Options();
    
    public static void logOut(String msg) {
        log.info(msg);
    }

    static CommandLine initCommandLineParser(String[] args)
            throws ParseException {
        CommandLineParser parser = new PosixParser();
        initArgOption(false, "Help", "h");
        initArgOption(false, "Backup directory : local backup firectory. (default C:\\TMP\\mongoBackup or /tmp/mongoBackup)", "d");
        initArgOption(false, "Database name (required)", "n");
        initArgOption(false, "Collection name (optional)", "c");
        initArgOption(false, "action 'BACKUP' or 'RESTORE' (default: BACKUP)", "a");
        CommandLine rez = parser.parse(OPTIONS, args);
        return rez;
    }
	private static void initArgOption(boolean required, String optionName, String optionArg) {
		OptionBuilder.withArgName(optionName);
		OptionBuilder.isRequired(required);
		OptionBuilder.hasArg();
		Option opt = OptionBuilder.create(optionArg);
		OPTIONS.addOption(opt);
	}

	/**
	 * main class (console entry point)
	 * @param args
	 */
    public static void main(String[] args) {
        CommandLine cmd;
        try {
            cmd = initCommandLineParser(args);
        } catch (ParseException e) {
            logOut("command line error : " + e.getMessage());
            printUsage();
            return;
        }
        if (cmd.hasOption("h")) {
            printUsage();
            return;
        }
        IMongoServerHostConfiguration hostConf = new MongoServerDefaultHostConfiguration();
        String dbName = null;

        if (cmd.hasOption("n")) { // db name (required)
            String argVal = cmd.getOptionValue("n");
            dbName = argVal;
        } else {
        	printUsage();
        	return;
        }

        String cOption = null;
        if (cmd.hasOption("c")) { // collection name (optional)
            String argVal = cmd.getOptionValue("c");
            cOption = argVal;
        }
        String dOption = null;
        if (cmd.hasOption("d")) { // backup directory (optional) of file to restore (required)
            String argVal = cmd.getOptionValue("d");
            dOption = argVal;
        }

        String action = "BACKUP";
        if (cmd.hasOption("a")) {
            String argVal = cmd.getOptionValue("a");
            action = argVal;
        }
        try {
			MongodumpServiceImpl dumpSvc = new MongodumpServiceImpl(hostConf);
	        if ("BACKUP".equals(action)) {
	            BackupConfiguration backupConf = BackupConfiguration.getInstance(dbName, cOption, dOption);
	            dumpSvc.backup(backupConf);
	        } else if ("RESTORE".equals(action)) {
	            RestoreConfiguration restoreConf = RestoreConfiguration.getInstance(dbName, cOption, dOption);
	            dumpSvc.restore(restoreConf);
	        } else {
	        	log.info("invalid action {} : expected BACKUP|RESTORE", action);
	        	printUsage();
	        }
        } catch (InvalidParameterException ipe) {
        	log.error(ipe.getMessage());
        	printUsage();
        } catch (Throwable t) {
        	log.error(t.getMessage());
        }
        logOut("bye");
    }


    public static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);
        formatter.printUsage(writer, 1000, "(application)", OPTIONS);
        writer.close();
        logOut(baos.toString());
    }

}