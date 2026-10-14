package invalidextension.failing;

import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;

/** An extension whose configure method fails. */
public final class FailingExtension implements Extension {

  @ConfigureFactory
  @SuppressWarnings("DoNotCallSuggester") // it fails on purpose
  public static void configureInjection(ConfigurationContext context) {
    throw new IllegalArgumentException("cannot configure");
  }

  @Override
  public void initialize() {
    // nothing to start
  }
}
