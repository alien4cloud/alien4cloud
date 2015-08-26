package alien4cloud.tool.compilation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import alien4cloud.tosca.ArchiveParser;
import alien4cloud.tosca.ArchivePostProcessor;
import alien4cloud.tosca.ArchiveUploadService;
import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingException;
import alien4cloud.tosca.parser.ParsingResult;
import alien4cloud.utils.FileUtil;

@Slf4j
public final class CompilationTool {

    private CompilationTool() {
    }

    private static final String ONLINE_PROFILE = "online";

    private static final String COMPILATION_TOOL_CONTEXT_XML = "compilation-tool-context.xml";

    /**
     * Help
     */
    private static final String HELP_OPTION = "h";

    /**
     * Turn to offline mode
     */
    private static final String OFFLINE_OPTION = "o";

    /**
     * Path to archive file
     */
    private static final String ARCHIVE_PATH_OPTION = "f";

    private static final Options COMMAND_LINE_OPTIONS = new Options();

    static {
        COMMAND_LINE_OPTIONS.addOption(HELP_OPTION, false, "Print help");
        COMMAND_LINE_OPTIONS.addOption(OFFLINE_OPTION, false, "Force offline");
        COMMAND_LINE_OPTIONS.addOption(ARCHIVE_PATH_OPTION, true, "Path to the archive can be zip file or directory");
    }

    private static void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("compilation-tool [-h] [-f path_to_archive_file] [-o]", COMMAND_LINE_OPTIONS);
    }

    private static void quitWithError(String message) {
        log.error(message);
        System.err.println(message);
        System.exit(1);
    }

    private static void quitWithError(Exception e) {
        log.error("Exception happened", e);
        quitWithError("Exception happened : [" + e.getMessage() + "], see log for more details");
    }

    private static void quitWithParsingError(ParsingException e) {
        errPrint("FAILURE - Failed to parse archive :");
        printErrors(e.getFileName(), e.getParsingErrors());
        System.exit(1);
    }

    private static void quitWithCompilationError(ParsingResult<?> parsingResult) {
        errPrint("FAILURE :");
        printErrors(parsingResult.getContext().getFileName(), parsingResult.getContext().getParsingErrors());
        System.exit(1);
    }

    private static void printErrors(String fileName, List<ParsingError> parsingErrors) {
        errPrint("\t- File [" + fileName + "] :");
        for (ParsingError error : parsingErrors) {
            errPrint("\t\t- Error code        : " + error.getErrorCode().toString());
            errPrint("\t\t- Context and error : " + error.getContext());
            errPrint("\t\t-                   : " + error.getProblem());
            errPrint("\t\t- Note              : " + error.getNote());
            errPrint("\t\t- Start             : " + error.getStartMark().toString());
            errPrint("\t\t- End               : " + error.getEndMark().toString());
        }
    }

    private static void errPrint(String line) {
        log.error(line);
        System.err.println(line);
    }

    public static void main(String[] args) {
        Boolean forceOffline = null;
        String archiveFilePath = null;
        CommandLineParser cmdLineParser = new PosixParser();
        try {
            CommandLine cmd = cmdLineParser.parse(COMMAND_LINE_OPTIONS, args);
            if (cmd.hasOption(HELP_OPTION)) {
                printUsage();
                System.exit(0);
            }
            if (cmd.hasOption(OFFLINE_OPTION)) {
                forceOffline = Boolean.valueOf(cmd.getOptionValue(OFFLINE_OPTION));
            }
            if (!cmd.hasOption(ARCHIVE_PATH_OPTION)) {
                printUsage();
                quitWithError("Option -" + ARCHIVE_PATH_OPTION + " is mandatory : The path to the archive must be specified");
            } else {
                archiveFilePath = cmd.getOptionValue(ARCHIVE_PATH_OPTION);
            }
        } catch (ParseException e) {
            quitWithError(e);
        }
        GenericXmlApplicationContext context = new GenericXmlApplicationContext();
        if (forceOffline == null) {
            context.getEnvironment().setActiveProfiles(ONLINE_PROFILE);
        }
        context.load(new ClassPathResource(COMPILATION_TOOL_CONTEXT_XML));
        context.refresh();

        ArchiveParser parser = context.getBean(ArchiveParser.class);
        ArchivePostProcessor postProcessor = context.getBean(ArchivePostProcessor.class);
        ParsingResult<ArchiveRoot> parsingResult = null;
        Path archiveNioPath = null;
        try {
            archiveNioPath = Paths.get(archiveFilePath);
            if (Files.isDirectory(archiveNioPath)) {
                Path tempCsar = Files.createTempFile("", ".csar");
                tempCsar.toFile().deleteOnExit();
                FileUtil.zip(archiveNioPath, tempCsar);
                archiveNioPath = tempCsar;
            }
        } catch (IOException | InvalidPathException | SecurityException e) {
            quitWithError(e);
        }
        try {
            parsingResult = parser.parse(archiveNioPath);
        } catch (ParsingException e) {
            quitWithParsingError(e);
        }

        postProcessor.postProcess(parsingResult);

        if (ArchiveUploadService.hasError(parsingResult, null)) {
            quitWithCompilationError(parsingResult);
        } else {
            log.info("SUCCESS - Archive is valid");
            System.out.println("SUCCESS - Archive is valid");
        }

        context.close();
    }
}
