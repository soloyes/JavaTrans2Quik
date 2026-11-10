package io.github.soloyes.quikbridge.subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One TRANS2QUIK_SUBSCRIBE_ORDERS / TRANS2QUIK_SUBSCRIBE_TRADES call:
 * a class code (empty = all classes) and instrument codes (empty = all instruments of the class).
 */
public final class Subscription {
	/** Separator of instrument codes in the DLL's Seccodes argument. */
	public static final String SEC_CODE_SEPARATOR = "|";

	private final String name;
	private final String classCode;
	private final List<String> secCodes;

	public Subscription(String name, String classCode, List<String> secCodes) {
		this.name = Objects.requireNonNull(name);
		this.classCode = classCode == null ? "" : classCode;
		this.secCodes = Collections.unmodifiableList(new ArrayList<>(secCodes));
	}

	public String name() {
		return name;
	}

	public String classCode() {
		return classCode;
	}

	public List<String> secCodes() {
		return secCodes;
	}

	public boolean allClasses() {
		return classCode.isEmpty();
	}

	public boolean allSecCodes() {
		return secCodes.isEmpty();
	}

	/** Seccodes argument for the DLL, e.g. {@code "LKOH|GAZP"}; empty string = all instruments. */
	public String secCodesParam() {
		return String.join(SEC_CODE_SEPARATOR, secCodes);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Subscription)) return false;
		Subscription that = (Subscription) o;
		return name.equals(that.name) && classCode.equals(that.classCode) && secCodes.equals(that.secCodes);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, classCode, secCodes);
	}

	@Override
	public String toString() {
		return name + "{class=" + (allClasses() ? "<all>" : classCode)
			+ ", sec=" + (allSecCodes() ? "<all>" : secCodesParam()) + "}";
	}
}
