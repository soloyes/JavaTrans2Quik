package io.github.soloyes.quikbridge.subscription;

import io.github.soloyes.quikbridge.config.BridgeConfig;
import io.github.soloyes.quikbridge.config.ConfigException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionPlannerTest {

	private static List<Subscription> plan(String... kv) {
		Properties p = new Properties();
		for (int i = 0; i < kv.length; i += 2) {
			p.setProperty(kv[i], kv[i + 1]);
		}
		return new SubscriptionPlanner(BridgeConfig.fromProperties(p)).plan();
	}

	private static Subscription sub(String name, String classCode, String... codes) {
		return new Subscription(name, classCode, Arrays.asList(codes));
	}

	@Test
	void allClassesAllInstruments() {
		List<Subscription> plan = plan("subscription.all", "true");
		assertEquals(Collections.singletonList(sub("all", "")), plan);
		assertTrue(plan.get(0).allClasses());
		assertEquals("", plan.get(0).secCodesParam());
	}

	@Test
	void allClassesFilteredByInstruments() {
		List<Subscription> plan = plan("subscription.all", "true", "subscription.all.sec.codes", " LKOH, SBER ,,LKOH");
		assertEquals(Collections.singletonList(sub("all", "", "LKOH", "SBER")), plan);
		assertEquals("LKOH|SBER", plan.get(0).secCodesParam());
	}

	@Test
	void namedBundlesReadTheirOwnSecCodes() {
		// The 2019 code looked at the global order.book.sec.codes instead of the per-bundle key.
		List<Subscription> plan = plan(
			"subscription.names", "oil, metals",
			"subscription.class.code.oil", "TQBR",
			"subscription.sec.codes.oil", "LKOH,GAZP",
			"subscription.class.code.metals", "TQBR2",
			"subscription.sec.codes.metals", "PLZL");
		assertEquals(Arrays.asList(sub("oil", "TQBR", "LKOH", "GAZP"), sub("metals", "TQBR2", "PLZL")), plan);
		assertEquals("LKOH|GAZP", plan.get(0).secCodesParam());
	}

	@Test
	void bundleWithoutSecCodesMeansWholeClass() {
		List<Subscription> plan = plan("subscription.names", "fut", "subscription.class.code.fut", "SPBFUT");
		assertEquals(Collections.singletonList(sub("fut", "SPBFUT")), plan);
		assertTrue(plan.get(0).allSecCodes());
	}

	@Test
	void duplicateAndBlankNamesAreIgnored() {
		List<Subscription> plan = plan("subscription.names", "a,,a, a ",
			"subscription.class.code.a", "TQBR", "subscription.sec.codes.a", "SBER");
		assertEquals(Collections.singletonList(sub("a", "TQBR", "SBER")), plan);
	}

	@Test
	void bundlesOnTheSameClassAreMerged() {
		assertEquals(Collections.singletonList(sub("oil+banks", "TQBR", "LKOH", "SBER", "VTBR")), plan(
			"subscription.names", "oil,banks",
			"subscription.class.code.oil", "TQBR", "subscription.sec.codes.oil", "LKOH,SBER",
			"subscription.class.code.banks", "TQBR", "subscription.sec.codes.banks", "SBER,VTBR"));
	}

	@Test
	void mergeWithAWholeClassBundleSubscribesWholeClass() {
		assertEquals(Collections.singletonList(sub("oil+everything", "TQBR")), plan(
			"subscription.names", "oil,everything",
			"subscription.class.code.oil", "TQBR", "subscription.sec.codes.oil", "LKOH",
			"subscription.class.code.everything", "TQBR"));
	}

	@Test
	void bundleWithoutClassCodeKeyIsAConfigError() {
		ConfigException e = assertThrows(ConfigException.class,
			() -> plan("subscription.names", "oil", "subscription.sec.codes.oil", "LKOH"));
		assertTrue(e.getMessage().contains("subscription.class.code.oil"));
	}

	@Test
	void noBundlesGivesEmptyPlan() {
		assertTrue(plan().isEmpty());
		assertTrue(plan("subscription.names", " , ").isEmpty());
	}
}
