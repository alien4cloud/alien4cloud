package alien4cloud.tosca.container.model.type;

import alien4cloud.tosca.container.model.ToscaInheritableElement;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * An Artifact Type is a reusable entity that defines the type of one or more Artifact Templates which in turn serve as deployment artifacts for Node Templates
 * or implementation artifacts for Node Type and Relationship Type interface operations. For example, an Artifact Type “WAR File�? might be defined for
 * describing web application archive files. Based on this Artifact Type, one or more Artifact Templates representing concrete WAR files can be defined and
 * referenced as deployment or implementation artifacts.
 * </p>
 * <p>
 * An Artifact Type can define the structure of observable properties via a Properties Definition, i.e. the names, data types and allowed values the properties
 * defined in Artifact Templates using an Artifact Type or instances of such Artifact Templates can have. Note that properties defined by an Artifact Type are
 * assummed to be invariant across the contexts in which corresponding artifacts are used – as opposed to properties that can vary depending on the context. As
 * an example of such an invariant property, an Artifact Type for a WAR file could define a “signature�? property that can hold a hash for validating the actual
 * artifact proper. In contrast, the path where the web application contained in the WAR file gets deployed can vary for each place where the WAR file is used.
 * </p>
 * <p>
 * An Artifact Type can inherit definitions and semantics from another Artifact Type by means of the DerivedFrom element. Artifact Types can be declared as
 * abstract, meaning that they cannot be instantiated. The purpose of such abstract Artifact Types is to provide common properties for re-use in specialized,
 * derived Artifact Types. Artifact Types can also be declared as final, meaning that they cannot be derived by other Artifact Types
 * </p>
 * 
 * @author luc boutier
 */
@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
public class ArtifactType extends ToscaInheritableElement {
}