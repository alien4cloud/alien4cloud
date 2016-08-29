package alien4cloud.tosca.parser;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.yaml.snakeyaml.nodes.Node;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ParsingContextExecution {
    private final static ThreadLocal<Context> CONTEXT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Create a new context from a given file name.
     */
    public static void init() {
        CONTEXT_THREAD_LOCAL.set(new Context());
    }

    /**
     * Allow to know if a context is initialized for the current thread.
     * 
     * @return True if the context is initialized, false if not.
     */
    public static boolean exist() {
        return CONTEXT_THREAD_LOCAL.get() != null;
    }

    /**
     * Set the registry of parsers to use for the parsing.
     *
     * @param registry the registry of parsers to use for the parsing.
     */
    public static void setRegistry(Map<String, INodeParser> registry) {
        CONTEXT_THREAD_LOCAL.get().setRegistry(registry);
    }

    /**
     * Get th name of the file under parsing.
     * 
     * @return The name of the file under parsing.
     */
    public static String getFileName() {
        return CONTEXT_THREAD_LOCAL.get().getFileName();
    }

    /**
     * Get th name of the file under parsing.
     *
     * @return The name of the file under parsing.
     */
    public static void setFileName(String fileName) {
        CONTEXT_THREAD_LOCAL.get().setFileName(fileName);
    }

    /**
     * Get the registry of parsers.
     * 
     * @return The registry of parsers.
     */
    public static Map<String, INodeParser> getRegistry() {
        return CONTEXT_THREAD_LOCAL.get().getRegistry();
    }

    /**
     * Get an identity hashmap parsed object to yaml node.
     *
     * @return identity hashmap parsed object to yaml node.
     */
    public static Map<Object, Node> getObjectToNodeMap() {
        return CONTEXT_THREAD_LOCAL.get().getObjectToNodeMap();
    }

    /**
     * Set the parent object in the object hierarchy.
     *
     * @param parent the parent object in the object hierarchy.
     */
    public static void setParent(Object parent) {
        CONTEXT_THREAD_LOCAL.get().setParent(parent);
    }

    /**
     * Get the parent object in the object hierarchy.
     *
     * @return the parent object in the object hierarchy.
     */
    public static Object getParent() {
        return CONTEXT_THREAD_LOCAL.get().getParent();
    }

    /**
     * Get the instance of the root object under parsing.
     *
     * @return the instance of the root object under parsing.
     */
    public static <T> T getRootObj() {
        return (T) CONTEXT_THREAD_LOCAL.get().getRoot().getWrappedInstance();
    }

    /**
     * Get the bean wrapper of the root object under parsing.
     *
     * @return the bean wrapper of the root object under parsing.
     */
    public static BeanWrapper getRoot() {
        return CONTEXT_THREAD_LOCAL.get().getRoot();
    }

    /**
     * Set the bean wrapper of the root object under parsing.
     * 
     * @param beanWrapper the bean wrapper of the root object under parsing.
     */
    public static void setRoot(BeanWrapper beanWrapper) {
        CONTEXT_THREAD_LOCAL.get().setRoot(beanWrapper);
    }

    /**
     * Get the list of parsing errors.
     * 
     * @return The list of parsing errors.
     */
    public static List<ParsingError> getParsingErrors() {
        return CONTEXT_THREAD_LOCAL.get().getParsingErrors();
    }

    /**
     * Get the inner Parsing context that contains the actual resulting bean as well as the list of errors.
     *
     * @return the inner Parsing context that contains the actual resulting bean as well as the list of errors.
     */
    public static ParsingContext getParsingContext() {
        return CONTEXT_THREAD_LOCAL.get().getParsingContext();
    }

    /**
     * Destroy the execution context.
     */
    public static void destroy() {
        CONTEXT_THREAD_LOCAL.remove();
    }

    @Getter
    @Setter
    public static class Context {
        /** Root node under parsing. */
        private BeanWrapper root;
        /** Eventually, the current node parent object. */
        private Object parent;
        /** Map of parsers by type */
        private Map<String, INodeParser> registry;
        /** Map of all parsed objects and the node from which they where parsed. */
        private Map<Object, Node> objectToNodeMap = new IdentityHashMap<>();
        /** The parsing context. */
        private final ParsingContext parsingContext = new ParsingContext();

        public void setFileName(String fileName) {
            parsingContext.setFileName(fileName);
        }

        public String getFileName() {
            return parsingContext.getFileName();
        }

        public List<ParsingError> getParsingErrors() {
            return parsingContext.getParsingErrors();
        }
    }
}