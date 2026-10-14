package invalidextension.nonstatic;

import com.sitepark.ies.extension.api.ConfigurationContext;
import com.sitepark.ies.extension.api.Extension;
import com.sitepark.ies.extension.api.annotations.ConfigureFactory;

/** An extension production refuses: its configure method is not static. */
public final class NonStaticExtension implements Extension {

  @ConfigureFactory
  public void configureInjection(ConfigurationContext context) {
    // never called
  }

  @Override
  public void initialize() {
    // nothing to start
  }
}
