package com.sitepark.ies.aggregator.testkit;

import com.sitepark.ies.aggregator.resolver.EntityResolver;
import com.sitepark.ies.aggregator.resolver.GroupDescriptor;
import com.sitepark.ies.aggregator.resolver.GroupResolver;
import com.sitepark.ies.aggregator.resolver.Resolver;
import com.sitepark.ies.aggregator.resolver.ResolverPath;
import com.sitepark.ies.aggregator.resolver.Revision;
import com.sitepark.ies.aggregator.resolver.User;
import com.sitepark.ies.aggregator.value.ResolvedValue;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * {@link Resolver} view over a single {@link Repository} entry.
 *
 * <p>The access method decides how a field value is interpreted, matching the production semantics:
 * {@link #resolve(String)} treats a string as an id reference to another entry, whereas {@link
 * #value(String)} treats it as a scalar. This is why {@code parent} (navigated via {@code resolve})
 * follows a reference while {@code sp_imageCopyright} (read via {@code value}) stays a plain text.
 *
 * <p>An entry separates its structural metadata (its own {@code id}, an {@code anchor}, a {@code
 * parent} link, the {@code group} block, the {@code media} block, {@code media_options}, ...) from
 * its editable CMS fields, which live under a nested {@code content} node. A field access probes
 * the entry level first and falls back to {@code content} — <b>except for the revision data</b>
 * ({@code created}, {@code changed} and their {@code *By} blocks), which only the descriptor
 * answers, because in production they are not fields at all (see {@link #field(String)}). Values
 * nested
 * inside a content field (e.g. a {@code media} block, a {@code {"link": ...}} reference, or an
 * {@code sp_*} config object) keep their natural flat shape and are found directly.
 *
 * <p>It also implements {@link GroupResolver} (hence {@link EntityResolver}) so production code can
 * navigate the containing group via {@code root().parentGroup()}. Group navigation ({@link
 * #parentGroup()}/{@link #parentGroupPath()}) follows the entry's {@code parent} id reference — a
 * parent is always a group, as it is in the CMS.
 *
 * <p>The group-specific members are backed by the entry's {@code group} block, whose fields mirror
 * them one to one; the sub-group and entity lists are id references into the {@link Repository}:
 *
 * <pre>{@code
 * "5000": {
 *   "id": 5000,
 *   "anchor": "site-root",
 *   "group": {
 *     "lang": "de",
 *     "rootSiteGroup": true,
 *     "micrositeRootSiteGroup": false,
 *     "subGroups": ["5100"],
 *     "entities": ["1000", "1330"]
 *   }
 * }
 * }</pre>
 *
 * <p>An entry without a {@code group} block keeps the neutral defaults (no children, no language, no
 * site-root marker); the presence of the block is also what makes an entry findable as a group by
 * {@link ScenarioRootResolverFactory}.
 */
final class RepositoryResolver implements GroupResolver {

  /** Nested node holding the entry's editable CMS fields, probed after the structural fields. */
  private static final String CONTENT_KEY = "content";

  /** The entry's link to its containing group — structural metadata, not a CMS field. */
  private static final String PARENT_KEY = "parent";

  /**
   * Master data of the entry. There is no CMS field by these names, in any scenario or in any
   * article, so a field access must not find them — only the descriptor answers them.
   *
   * <p>Letting a scenario serve them as fields has hidden two production bugs already: {@code
   * created} cost {@code base.teaser.date} its value, and {@code id} made every download of an
   * uploaded file look up its URI for object 0, which published no link at all.
   *
   * <p>Only keys proven not to be fields belong here. {@code objectType}, for one, does not: the
   * article really carries it, which is how {@code InternalConfigLoader} reads it with {@code
   * getPrimitiveText("objectType")}. Blocking a key that production can read would break correct
   * code instead of catching broken code. {@code anchor}, {@code filename} and {@code version} are
   * unproven either way and are therefore left out.
   */
  private static final Set<String> MASTER_DATA_KEYS =
      Set.of("created", "changed", "createdBy", "changedBy", "id");

  /** Structural block holding the entry's group data. */
  private static final String GROUP_KEY = "group";

  /** Structural block holding the entry's media asset. */
  private static final String MEDIA_KEY = "media";

  /** Field a scenario spells out a section's own, reordering-stable id under. */
  private static final String UUID_KEY = "uuid";

  private final ResolverPath resolverPath;
  private final Map<String, Object> node;
  private final Repository repository;
  private final String assignedKey;
  private final GroupDescriptor descriptor = new NodeDescriptor();

  /** A resolver on an entry, which starts a {@link #nodeKey()} of its own. */
  RepositoryResolver(ResolverPath resolverPath, Map<String, Object> node, Repository repository) {
    this(resolverPath, node, repository, "");
  }

  RepositoryResolver(
      ResolverPath resolverPath,
      Map<String, Object> node,
      Repository repository,
      String assignedKey) {
    this.resolverPath = resolverPath;
    this.node = node;
    this.repository = repository;
    this.assignedKey = assignedKey;
  }

  @Override
  public boolean isEmpty() {
    return this.node.isEmpty();
  }

  @Override
  public ResolverPath path() {
    return this.resolverPath;
  }

  /**
   * Mirrors the production derivation, digest and all: the raw key is composed unhashed and hashed
   * once here, at the boundary, because {@link Resolver#nodeKey()} is contracted to be opaque. A
   * scenario spelling out the same uuid as the CMS therefore yields the very same key.
   */
  @Override
  public String nodeKey() {
    String rawKey = this.rawKey();
    return rawKey.isEmpty() ? "" : sha256(rawKey);
  }

  /**
   * The unhashed key, which is what the nodes below this one continue: the node's own {@code uuid}
   * where a scenario spells one out, otherwise the key it was assigned by the node it was resolved
   * from — its own, the field name of that step already included — and otherwise the {@code id} of
   * the entry it is. An entry of a scenario therefore behaves like an object of the CMS, and two
   * sections of one iterate are told apart by their uuid rather than by their position.
   */
  private String rawKey() {
    String uuid = this.value(UUID_KEY).asString("");
    if (!uuid.isEmpty()) {
      return uuid;
    }
    if (!this.assignedKey.isEmpty()) {
      return this.assignedKey;
    }
    return this.structural("id").asString("");
  }

  /**
   * The raw key of the <em>child</em> resolved under {@code name} — this node's raw key with the
   * step's field name appended, so a node never shares its key with the nodes below it.
   */
  private String keyOfChild(String name) {
    String rawKey = this.rawKey();
    return rawKey.isEmpty() ? "" : rawKey + '.' + name;
  }

  private static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is not available", e);
    }
  }

  @Override
  public Resolver resolve(String key) {
    return this.resolverPath.descend(
        key, (path) -> this.repository.toResolver(path, this.field(key), this.keyOfChild(key)));
  }

  /**
   * Follows a link field to the linked entity. A link carries its target either as a bare id
   * reference or wrapped under a {@code "link"} key ({@code {"link": "4711"}}); {@code resolveLink}
   * unwraps that {@code link} shape implicitly, whereas {@link #resolve(String)} stays on the
   * wrapper node.
   */
  @Override
  public EntityResolver resolveLink(String key) {
    Object value = this.field(key);
    if (value instanceof Map<?, ?> map && map.get("link") != null) {
      value = map.get("link");
    }
    final Object target = value;
    Resolver resolver =
        this.resolverPath.descend(
            key, (path) -> this.repository.toResolver(path, target, this.keyOfChild(key)));
    return resolver instanceof EntityResolver entity
        ? entity
        : EntityResolver.empty(resolver.path());
  }

  /**
   * Follows a link to a user account. In a scenario such a field is simply the user block itself,
   * the same shape a revision's user uses.
   */
  @Override
  public User resolveUser(String key) {
    return this.user(key);
  }

  /**
   * The user of the given block, or the empty user if the entry carries none.
   *
   * <p>Reached structurally, not as a field: a {@code changedBy} block belongs to the revision, and
   * {@link #MASTER_DATA_KEYS} keeps a field access away from it.
   */
  private User user(String key) {
    Resolver user =
        this.resolverPath.descend(
            key,
            (path) -> this.repository.toResolver(path, this.node.get(key), this.keyOfChild(key)));
    if (user.isEmpty()) {
      return User.empty();
    }
    return new ScenarioUser(
        user.value("id").asString(""),
        user.value("anchor").asString(""),
        user.value("name").asString(""),
        user.value("firstName").asString(""),
        user.value("lastName").asString(""));
  }

  @Override
  public List<Resolver> resolveList(String key) {
    Object value = this.field(key);
    if (value instanceof List<?> list) {
      List<Resolver> resolvers = new ArrayList<>();
      for (int index = 0; index < list.size(); index++) {
        Object element = list.get(index);
        // The position belongs in the key, exactly as it does in production: without it every
        // element of the list would inherit the same one.
        String childKey = this.keyOfChild(key + '[' + index + ']');
        resolvers.add(
            this.resolverPath.descend(
                key, (path) -> this.repository.toResolver(path, element, childKey)));
      }
      return resolvers;
    }
    if (value instanceof Map) {
      return List.of(
          this.resolverPath.descend(
              key, (path) -> this.repository.toResolver(path, value, this.keyOfChild(key))));
    }
    return List.of();
  }

  @Override
  public ResolvedValue value(String key) {
    Object value = this.field(key);
    if (value == null || value instanceof Map) {
      return ResolvedValue.empty();
    }
    return ResolvedValue.of(value);
  }

  @Override
  public GroupDescriptor entity() {
    return this.descriptor;
  }

  @Override
  public @Nullable GroupResolver parentGroup() {
    Object parent = this.node.get(PARENT_KEY);
    if (parent == null) {
      return null;
    }
    Resolver resolver =
        this.resolverPath.descend(
            PARENT_KEY,
            (path) -> this.repository.toResolver(path, parent, this.keyOfChild(PARENT_KEY)));
    return resolver instanceof GroupResolver group ? group : null;
  }

  /**
   * The containing groups, <b>outermost first</b> — the order the production resolver and the
   * legacy {@code __object.path} both use, so a consumer may read the list as a path from the
   * repository root down to the entry.
   */
  @Override
  public List<GroupResolver> parentGroupPath() {
    List<GroupResolver> path = new ArrayList<>();
    for (GroupResolver group = this.parentGroup(); group != null; group = group.parentGroup()) {
      path.addFirst(group);
    }
    return path;
  }

  @Override
  public List<GroupResolver> subGroups() {
    return List.copyOf(this.groupMembers("subGroups"));
  }

  @Override
  public List<EntityResolver> entities() {
    return List.copyOf(this.groupMembers("entities"));
  }

  /** The sub-groups followed by the entities, the order the CMS lists a group's children in. */
  @Override
  public List<EntityResolver> children() {
    List<EntityResolver> children = new ArrayList<>(this.subGroups());
    children.addAll(this.entities());
    return List.copyOf(children);
  }

  /** Returns the entry's group data, or an empty map if the entry is not a group. */
  private Map<String, Object> group() {
    Object group = this.node.get(GROUP_KEY);
    return group instanceof Map ? asObjectMap(group) : Map.of();
  }

  /**
   * Resolves a list of id references in the group block, skipping ids without an entry. Each member
   * becomes a new root within this resolver's navigation chain, so the navigation history is kept.
   */
  private List<RepositoryResolver> groupMembers(String key) {
    if (!(this.group().get(key) instanceof List<?> ids)) {
      return List.of();
    }
    List<RepositoryResolver> members = new ArrayList<>();
    for (Object id : ids) {
      RepositoryResolver member =
          id instanceof String reference
              ? this.repository.enterRoot(this.resolverPath, reference)
              : null;
      if (member != null) {
        members.add(member);
      }
    }
    return members;
  }

  /**
   * Resolves a field value, probing the entry's structural metadata first and falling back to the
   * nested {@code content} node when no structural field matches.
   *
   * <p>{@link #MASTER_DATA_KEYS} are the exception: they are never read from the entry level, because
   * in production they cannot be read as fields at all. {@code BaseInformationVOResolver.value(...)}
   * asks {@code getInformation(...)}, which knows the field catalog and nothing else, so master
   * data is only ever answered by the descriptor.
   *
   * @param key the field name
   * @return the raw field value, or {@code null} if neither the entry nor its {@code content} node
   *     carries it
   */
  private @Nullable Object field(String key) {
    if (!(this.isEntry() && MASTER_DATA_KEYS.contains(key)) && this.node.containsKey(key)) {
      return this.node.get(key);
    }
    Object content = this.node.get(CONTENT_KEY);
    if (content instanceof Map<?, ?> contentMap) {
      return contentMap.get(key);
    }
    return null;
  }

  /**
   * Whether this node stands for a repository entry rather than a nested data block.
   *
   * <p>Only an entry has master data a field access must not reach — a nested block such as a
   * medium carries its {@code id} as an ordinary field, and blocking it there would be wrong.
   */
  private boolean isEntry() {
    return this.node.containsKey(CONTENT_KEY);
  }

  /** Reads the entry's structural metadata, which no field access may reach. */
  private ResolvedValue structural(String key) {
    Object value = this.node.get(key);
    return value == null || value instanceof Map ? ResolvedValue.empty() : ResolvedValue.of(value);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asObjectMap(Object value) {
    return (Map<String, Object>) value;
  }

  /**
   * The entry's metadata, read from the very same node the fields come from: {@code id}, {@code
   * objectType}, {@code name} and {@code anchor} at the entry level, the site-root markers and the
   * language from its {@code group} block.
   *
   * <p>An entry that is no group answers the group-specific members with their neutral defaults, so a
   * scenario only has to spell out a {@code group} block where a group is actually meant.
   */
  private final class NodeDescriptor implements GroupDescriptor {

    @Override
    public int id() {
      return structural("id").asInt(0);
    }

    @Override
    public String qualifiedId() {
      return structural("qualifiedId").asString("");
    }

    @Override
    public String version() {
      return structural("version").asString("");
    }

    @Override
    public String type() {
      return structural("objectType").asString("");
    }

    @Override
    public String name() {
      return structural("name").asString("");
    }

    @Override
    public String anchor() {
      return structural("anchor").asString("");
    }

    @Override
    public String filename() {
      return structural("filename").asString("");
    }

    /**
     * An entry whose own node carries the {@code media} block <em>is</em> a medium. An upload
     * carries its block below the field it was uploaded into, so the entry holding it stays an
     * ordinary page - the same distinction the production adapter makes between a media article and
     * an article with a binary in one of its fields.
     */
    @Override
    public boolean isMedia() {
      return RepositoryResolver.this.node.get(MEDIA_KEY) instanceof Map;
    }

    @Override
    public Revision created() {
      return revision("created");
    }

    @Override
    public Revision changed() {
      return revision("changed");
    }

    @Override
    public boolean isRootSite() {
      return Boolean.TRUE.equals(group().get("rootSiteGroup"));
    }

    @Override
    public boolean isMicrositeRootSite() {
      return Boolean.TRUE.equals(group().get("micrositeRootSiteGroup"));
    }

    @Override
    public String lang() {
      return group().get("lang") instanceof String lang ? lang : "";
    }

    /**
     * Reads a revision from the entry: the timestamp from {@code <key>} as epoch milliseconds, the
     * user from the sibling block {@code <key>By}. An entry without the timestamp answers the
     * empty revision, one without the user block a {@link User#empty()}.
     */
    private Revision revision(String key) {
      ResolvedValue value = RepositoryResolver.this.structural(key);
      if (value.isEmpty()) {
        return Revision.empty();
      }
      Instant at = Instant.ofEpochMilli(value.asLong());
      User by = RepositoryResolver.this.user(key + "By");
      return new Revision() {
        @Override
        public Instant at() {
          return at;
        }

        @Override
        public User by() {
          return by;
        }
      };
    }
  }

  /** The user data of a scenario's {@code createdBy} / {@code changedBy} block. */
  private record ScenarioUser(
      String id, String anchor, String name, String firstName, String lastName) implements User {}
}
