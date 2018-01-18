package eu.erasmuswithoutpaper.registry.echovalidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.echovalidator.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.InvalidResponseError;
import eu.erasmuswithoutpaper.registry.internet.sec.ResponseCodingDecoder;

class DecodingHelper {

  private final Map<String, ResponseCodingDecoder> decoders;
  private Set<String> requiredCodings = new HashSet<>();
  private Set<String> acceptableCodings = new HashSet<>();

  DecodingHelper() {
    this.decoders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
  }

  private ResponseCodingDecoder getDecoder(String coding) throws InvalidResponseError {
    if (!this.decoders.containsKey(coding)) {
      throw new InvalidResponseError("Unsupported Content-Encoding: " + coding);
    }
    return this.decoders.get(coding);
  }

  private List<String> getOrderedCodings(Response response) {
    List<String> result = new ArrayList<>();
    String value = response.getHeader("Content-Encoding");
    if (value == null) {
      return result;
    }
    String[] items = value.split(", *");
    for (int i = items.length - 1; i >= 0; i--) {
      if (items[i].length() > 0) {
        result.add(items[i]);
      }
    }
    return result;
  }

  void addDecoder(ResponseCodingDecoder decoder) {
    if (this.decoders.containsKey(decoder.getContentEncoding())) {
      throw new RuntimeException(
          "Expecting unique Content-Encoding handlers, but this one is repeated: "
              + decoder.getContentEncoding());
    }
    this.decoders.put(decoder.getContentEncoding(), decoder);
  }

  void decode(InlineValidationStep step, Response response) throws Failure {
    Set<String> unsatisfied = new HashSet<>(this.requiredCodings);
    for (String coding : this.getOrderedCodings(response)) {
      try {
        ResponseCodingDecoder decoder = this.getDecoder(coding);
        decoder.decode(response);
      } catch (InvalidResponseError e) {
        throw new Failure(e.getMessage(), Status.FAILURE, response);
      }
      step.addResponseSnapshot(response);
      unsatisfied.remove(coding);
      if (!this.acceptableCodings.contains(coding)) {
        throw new Failure("The response was (successfully) encoded with the '" + coding
            + "' coding, but the client didn't declare this encoding as acceptable "
            + "(it wasn't listed in the Accept-Encoding header).", Status.FAILURE, response);
      }
    }
    if (this.getOrderedCodings(response).size() > 0) {
      throw new RuntimeException("One of the decoders failed to pop its own coding from "
          + "response's Content-Encoding header.");
    }
    if (unsatisfied.size() > 0) {
      throw new Failure(
          "Expecting the response to be encoded with "
              + unsatisfied.stream().collect(Collectors.joining(" and ")),
          Status.FAILURE, response);
    }
  }

  void setAcceptableCodings(Collection<String> acceptableCodings) {
    this.acceptableCodings = new HashSet<>(acceptableCodings);
  }

  void setRequiredCodings(Collection<String> requiredCodings) {
    this.requiredCodings = new HashSet<>(requiredCodings);
  }
}
