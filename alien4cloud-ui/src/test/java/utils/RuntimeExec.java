package utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

/**
 * Runtime exec is used to execute shell command locally.
 */
@Slf4j
public class RuntimeExec {

    /**
     * Utility to execute a runtime command.
     *
     * @param command
     * @param env
     * @return
     */
    public static ExecReturn execCommand(String command, String[] env) {
        Process p = null;
        String[] result = null;
        String[] error = null;
        int returnCode = -1;

        // For some reasons not all shell commands may be available so we write the commands to shell script and execute it.
        String fileName = UUID.randomUUID().toString() + ".sh";
        Path filePath = Paths.get(fileName);
        try {
            Files.write(filePath, command.getBytes());

            if (env != null) {
                p = Runtime.getRuntime().exec("sh " + fileName, env);
            } else {
                p = Runtime.getRuntime().exec("sh " + fileName);
            }
            error = readFlux(p.getErrorStream());
            result = readFlux(p.getInputStream());
            p.waitFor();
            returnCode = p.exitValue();
        } catch (IOException | InterruptedException e) {
            log.info("Error while executing local command {} with env {}.", command, env, e);
        } finally {
            if (Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    log.error("Encountered error while cleaning file {}", filePath.toAbsolutePath(), e);
                }
            }
        }
        return new ExecReturn(result, error, returnCode);
    }

    protected static String[] readFlux(InputStream flux) throws IOException {
        InputStreamReader streamReader = new InputStreamReader(flux);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        String line = null;
        List<String> result = new ArrayList<>();

        line = bufferedReader.readLine();
        while (line != null && !line.isEmpty()) {
            result.add(line);
            line = bufferedReader.readLine();
        }

        return result.toArray(new String[result.size()]);
    }

    /**
     * Get the error results as a single string.
     *
     * @param execReturn The exec return
     * @return The single string error message (multiline).
     */
    public static String errorAsString(ExecReturn execReturn) {
        StringBuilder sb = new StringBuilder();
        for (String errorLine : execReturn.getErrorLines()) {
            sb.append(errorLine).append(System.lineSeparator());
        }
        return sb.toString();
    }
}