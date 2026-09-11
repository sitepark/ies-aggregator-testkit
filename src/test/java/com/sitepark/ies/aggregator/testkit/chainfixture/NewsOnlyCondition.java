package com.sitepark.ies.aggregator.testkit.chainfixture;

import com.sitepark.ies.aggregator.AssemblerCondition;
import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;

/** Custom condition used by {@link CondGated}: applies only when the scope object type is news. */
public final class NewsOnlyCondition implements AssemblerCondition {
  @Override
  public boolean appliesTo(Resolver context) {
    Resolver root = context.root();
    return root instanceof EntityResolver entity && "news".equals(entity.entity().type());
  }
}
