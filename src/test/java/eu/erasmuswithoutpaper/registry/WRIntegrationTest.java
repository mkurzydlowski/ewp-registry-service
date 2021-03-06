package eu.erasmuswithoutpaper.registry;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * A subclass of {@link WRTest} for integration testing.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class WRIntegrationTest extends WRTest {

  @Value("${local.server.port}")
  private int port;

  /**
   * The base URL at which the application on the test integration web server can be accessed.
   */
  protected String baseURL;

  @PostConstruct
  private void init() {
    this.baseURL = "http://localhost:" + this.port;
  }
}
