package io.github.soloyes.quikbridge.subscription;

import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the {@code subscription.*} settings into the list of subscribe calls.
 *
 * <pre>
 * subscription.all=true                  one call for all classes;
 * subscription.all.sec.codes=LKOH,SBER   ...limited to these instruments (empty = everything)
 *
 * subscription.all=false
 * subscription.names=shares,futures      named bundles, one call per class code
 * subscription.class.code.shares=TQBR    required for every bundle (may be empty = all classes)
 * subscription.sec.codes.shares=LKOH     optional, empty or absent = whole class
 * </pre>
 *
 * Bundle names are trimmed and de-duplicated. Bundles with the same class code are merged into one call,
 * since the DLL subscribes per class; if any of them asks for the whole class, the merged call does too.
 */
public final class SubscriptionPlanner {
	private static final Logger log = LoggerFactory.getLogger(SubscriptionPlanner.class);

	public static final String ALL = "subscription.all";
	public static final String ALL_SEC_CODES = "subscription.all.sec.codes";
	public static final String NAMES = "subscription.names";
	public static final String CLASS_CODE_PREFIX = "subscription.class.code.";
	public static final String SEC_CODES_PREFIX = "subscription.sec.codes.";

	private final BridgeConfig config;

	public SubscriptionPlanner(BridgeConfig config) {
		this.config = config;
	}

	public List<Subscription> plan() {
		List<Subscription> result;
		if (config.getBoolean(ALL, false)) {
			List<String> secCodes = splitCodes(config.getString(ALL_SEC_CODES, ""));
			result = Collections.singletonList(new Subscription("all", "", secCodes));
		} else {
			result = planBundles();
		}
		if (result.isEmpty()) {
			log.warn("Subscription plan is empty: no orders or trades will be received");
		}
		for (Subscription s : result) {
			log.info("Planned subscription {}", s);
		}
		return result;
	}

	private List<Subscription> planBundles() {
		Map<String, String> nameByClass = new LinkedHashMap<>();
		Map<String, Set<String>> codesByClass = new LinkedHashMap<>();
		Set<String> wholeClass = new LinkedHashSet<>();

		for (String name : splitCodes(config.getString(NAMES, ""))) {
			String classKey = CLASS_CODE_PREFIX + name;
			if (!config.has(classKey)) {
				throw new ConfigException("Subscription '" + name + "' has no '" + classKey + "' property");
			}
			String classCode = config.getString(classKey);
			List<String> codes = splitCodes(config.getString(SEC_CODES_PREFIX + name, ""));

			nameByClass.merge(classCode, name, (a, b) -> a + "+" + b);
			Set<String> acc = codesByClass.computeIfAbsent(classCode, k -> new LinkedHashSet<>());
			if (codes.isEmpty()) {
				wholeClass.add(classCode);
			} else {
				acc.addAll(codes);
			}
		}

		List<Subscription> result = new ArrayList<>();
		for (Map.Entry<String, String> e : nameByClass.entrySet()) {
			String classCode = e.getKey();
			List<String> codes = wholeClass.contains(classCode)
				? Collections.<String>emptyList()
				: new ArrayList<>(codesByClass.get(classCode));
			result.add(new Subscription(e.getValue(), classCode, codes));
		}
		return result;
	}

	/** Comma-separated list, trimmed, blanks dropped, duplicates removed, order kept. */
	static List<String> splitCodes(String csv) {
		Set<String> out = new LinkedHashSet<>();
		for (String part : csv.split(",")) {
			String t = part.trim();
			if (!t.isEmpty()) {
				out.add(t);
			}
		}
		return new ArrayList<>(out);
	}
}
