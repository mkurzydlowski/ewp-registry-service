package eu.erasmuswithoutpaper.registry.documentbuilder;

/**
 * A collection of primary EWP element definitions, handled by the Registry Service.
 *
 * <p>
 * This enumeration is just a convenience created especially for the purpose of being called with
 * {@link BuildParams#setExpectedKnownElement(KnownElement)} method. It doesn't serve any other real
 * purpose.
 * </p>
 */
public class KnownElement {

  /**
   * The root of the Discovery API v4 response.
   */
  public static final KnownElement RESPONSE_MANIFEST_V4 =
      new KnownElement(KnownNamespace.RESPONSE_MANIFEST_V4, "manifest", "Discovery Manifest file");


  /**
   * The root of the Registry API v1 catalogue response.
   */
  public static final KnownElement RESPONSE_REGISTRY_V1_CATALOGUE = new KnownElement(
      KnownNamespace.RESPONSE_REGISTRY_V1, "catalogue", "Registry Service <catalogue> response");

  /**
   * The root of the common &lt;error-response/&gt;, as defined in the
   * <a href='https://github.com/erasmus-without-paper/ewp-specs-architecture'>EWP Architecture
   * document</a>.
   */
  public static final KnownElement COMMON_ERROR_RESPONSE =
      new KnownElement(KnownNamespace.COMMON_TYPES_V1, "error-response", "Generic Error Response");

  private final KnownNamespace namespace;
  private final String elementName;
  private final String humanReadableName;

  private KnownElement(KnownNamespace namespace, String elementName, String humanReadableName) {
    this.namespace = namespace;
    this.elementName = elementName;
    this.humanReadableName = humanReadableName;
  }

  /**
   * @return Plain-text, human-readable name for this element, e.g.
   *         <code>"Discovery Manifest file"</code>, or
   *         <code>"Registry &lt;catalogue&gt; response"</code>.
   */
  public String getHumanReadableName() {
    return this.humanReadableName;
  }

  /**
   * @return The local name of the element.
   */
  public String getLocalName() {
    return this.elementName;
  }

  /**
   * @return The namespace URI of the element.
   */
  public String getNamespaceUri() {
    return this.namespace.getNamespaceUri();
  }

  @Override
  public String toString() {
    return '{' + this.getNamespaceUri() + '}' + this.getLocalName();
  }
}

