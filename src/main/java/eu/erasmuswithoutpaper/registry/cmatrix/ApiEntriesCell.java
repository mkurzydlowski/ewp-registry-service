package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.w3c.dom.Element;

/**
 * This is used for ordering XML API entries by their version.
 */
class ApiEntriesCell extends CoverageMatrixCell {

  @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
  private static class ApiVersionComparator implements Comparator<Element> {

    @Override
    public int compare(Element e1, Element e2) {
      List<Integer> p1 = this.extractVersionInts(e1);
      List<Integer> p2 = this.extractVersionInts(e2);
      for (int i = 0; i < p1.size() && i < p2.size(); i++) {
        int result = p1.get(i).compareTo(p2.get(i));
        if (result != 0) {
          return result;
        }
      }
      Integer s1 = p1.size();
      Integer s2 = p2.size();
      return s1.compareTo(s2);
    }

    private List<Integer> extractVersionInts(Element elem) {
      List<Integer> result = new ArrayList<>(3);
      String version = $(elem).attr("version");
      if (version == null) {
        // Should not happen, but just in case.
        version = "0.0.0";
      }
      for (String entry : version.split("\\.")) {
        result.add(this.parseInt(entry));
      }
      return result;
    }

    private Integer parseInt(String entry) {
      try {
        return Integer.parseUnsignedInt(entry);
      } catch (NumberFormatException e) {
        return Integer.MAX_VALUE;
      }
    }
  }

  final KnownElement[] apiEntryClasses;
  final RegistryClient client;
  final HeiEntry hei;
  final List<Element> matchedApiEntries;
  final KnownElement lastClass;

  ApiEntriesCell(int colorClass, RegistryClient client, HeiEntry hei,
      KnownElement... apiEntryClasses) {
    super(colorClass);
    this.client = client;
    this.hei = hei;
    this.apiEntryClasses = apiEntryClasses;
    this.matchedApiEntries = new ArrayList<>();

    this.addClass("ewpst__apiEntriesCell");
    this.lastClass = this.apiEntryClasses[this.apiEntryClasses.length - 1];
    for (int i = 0; i < this.apiEntryClasses.length; i++) {
      KnownElement apiClass = this.apiEntryClasses[i];
      ApiSearchConditions conds = new ApiSearchConditions();
      conds.setRequiredHei(this.hei.getId());
      conds.setApiClassRequired(apiClass.getNamespaceUri(), apiClass.getLocalName());
      this.matchedApiEntries.addAll(this.client.findApis(conds));
    }
    this.matchedApiEntries.sort(new ApiVersionComparator().reversed());
    // Check the first one. Is it the most recent API class?
    boolean isLastClassImplemented = (this.matchedApiEntries.size() > 0
        && this.lastClass.matches(this.matchedApiEntries.get(0)));
    if (isLastClassImplemented) {
      this.addClass("ewpst__apiEntriesCell--containsUpToDate");
    }
  }


}
