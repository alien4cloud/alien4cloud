package alien4cloud.tosca.parser;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeanWrapper;

@Slf4j
public class ParsingContextExecution {

    @Getter
    @Setter
    private BeanWrapper root;

    private final Queue<DefferedParsingValueExecutor> deferredParsers = new PriorityQueue<DefferedParsingValueExecutor>();

    @Getter
    private final ParsingContext parsingContext;

    /** Map of parsers by type */
    @Getter
    @Setter
    private Map<String, INodeParser> registry;

    /** Eventually, the current node parent object. */
    @Getter
    @Setter
    private Object parent;

    public ParsingContextExecution(String fileName) {
        parsingContext = new ParsingContext(fileName);
    }

    public String getFileName() {
        return parsingContext.getFileName();
    }

    public List<ParsingError> getParsingErrors() {
        return parsingContext.getParsingErrors();
    }

    public void addDeferredParser(DefferedParsingValueExecutor runnable) {
        if (log.isDebugEnabled()) {
            log.debug("Adding deferred parser for key : " + runnable.getKey());
        }
        // if the order is not defined, we use the queue size to keep FIFO behavior
        if (runnable.getDeferredOrder() == 0) {
            runnable.setDeferredOrder(deferredParsers.size());
        }
        deferredParsers.offer(runnable);
    }

    public void runDefferedParsers() {
        while (!deferredParsers.isEmpty()) {
            DefferedParsingValueExecutor runnable = deferredParsers.poll();
            if (log.isDebugEnabled()) {
                log.debug("Finally executing deferred parser for key : " + runnable.getKey());
            }
            runnable.run();
        }
    }

}