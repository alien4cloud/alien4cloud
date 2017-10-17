package org.alien4cloud.tosca.catalog;

import java.util.List;

import org.alien4cloud.tosca.model.Csar;

import alien4cloud.tosca.model.ArchiveRoot;
import alien4cloud.tosca.parser.ParsingContext;
import alien4cloud.tosca.parser.ParsingError;
import alien4cloud.tosca.parser.ParsingResult;

public class ArchiveParserUtil {

    private ArchiveParserUtil(){}

    /**
     * Create a simple result without all the parsed data but just the {@link Csar} object as well as the eventual errors.
     *
     * @return The simple result out of the complex parsing result.
     */
    public static ParsingResult<Csar> toSimpleResult(ParsingResult<ArchiveRoot> parsingResult) {
        ParsingResult<Csar> simpleResult = cleanup(parsingResult);
        simpleResult.setResult(parsingResult.getResult().getArchive());
        return simpleResult;
    }

    public static <T> ParsingResult<T> cleanup(ParsingResult<?> result) {
        ParsingContext context = new ParsingContext(result.getContext().getFileName());
        context.getParsingErrors().addAll(result.getContext().getParsingErrors());
        return new ParsingResult<T>(null, context);
    }

    public static ParsingContext toParsingContext(String filename, List<ParsingError> parsingErrors) {
        ParsingContext context = new ParsingContext(filename);
        context.setParsingErrors(parsingErrors);
        return context;
    }
}
