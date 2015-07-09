package alien4cloud.plugin.exception;

public class MissingPlugingDescriptorFileException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingPlugingDescriptorFileException() {
        super("Your plugin don't have the META-INF/plugin.yml file");
    }
}
