package alien4cloud.tool.compilation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

import alien4cloud.tool.compilation.exception.CompilationToolRuntimeException;
import alien4cloud.tosca.container.archive.ArchiveParser;
import alien4cloud.tosca.container.archive.ArchivePostProcessor;
import alien4cloud.tosca.container.archive.IArchiveValidator;
import alien4cloud.tosca.container.exception.CSARDuplicatedElementDeclarationException;
import alien4cloud.tosca.container.exception.CSARParsingException;
import alien4cloud.tosca.container.model.CloudServiceArchive;
import alien4cloud.tosca.container.validation.CSARError;
import alien4cloud.tosca.container.validation.CSARValidationResult;
import alien4cloud.utils.FileUtil;

@Slf4j
public final class CompilationTool {
    
    private CompilationTool(){}

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

    private static void quitWithParsingError(CSARParsingException e) {
        log.error("Parsing error", e);
        log.error("FAILURE - Compilation errors :");
        System.err.println("FAILURE - Compilation errors :");
        log.error("\t- File [" + e.getFileName() + "] :");
        System.err.println("\t- File [" + e.getFileName() + "] :");
        log.error("\t\t+ " + e.createCSARError());
        System.err.println("\t\t+ " + e.createCSARError());
        System.exit(1);
    }

    private static void quitWithCompilationError(Map<String, Set<CSARError>> errors) {
        log.error("FAILURE - Compilation errors :");
        System.err.println("FAILURE - Compilation errors :");
        for (Map.Entry<String, Set<CSARError>> errorEntry : errors.entrySet()) {
            String fileName = errorEntry.getKey();
            Set<CSARError> errorList = errorEntry.getValue();
            log.error("\t- File [" + fileName + "] :");
            System.err.println("\t- File [" + fileName + "] :");
            for (CSARError error : errorList) {
                log.error("\t\t+ " + error);
                System.err.println("\t\t+ " + error);
            }
        }
        System.exit(1);
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
        IArchiveValidator validator = context.getBean(IArchiveValidator.class);
        ArchiveParser parser = context.getBean(ArchiveParser.class);
        ArchivePostProcessor postProcessor = context.getBean(ArchivePostProcessor.class);
        CloudServiceArchive archive = null;
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
            archive = parser.parseArchive(archiveNioPath);
        } catch (CSARParsingException e) {
            quitWithParsingError(e);
        }
        try {
            postProcessor.postProcessArchive(archive);
        } catch (CSARDuplicatedElementDeclarationException e) {
            quitWithParsingError(e);
        }
        CSARValidationResult validationResult = null;
        try {
            validationResult = validator.validateArchive(archive);
        } catch (CompilationToolRuntimeException e) {
            quitWithError(e);
        }
        if (validationResult.isValid()) {
            log.info("SUCCESS - Archive is valid");
            System.out.println("SUCCESS - Archive is valid");
        } else {
            quitWithCompilationError(validationResult.getErrors());
        }
        context.close();
    }
}
