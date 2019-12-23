package main.eventbusImpl.events;

import lombok.Getter;

public class SubscriptionList {
	@Getter
	private final String classCode;
	@Getter
	private final String[] secCodes;
	@Getter
	private final String name;
	private final String delimiter = "|";

	public SubscriptionList(String classCode, String[] secCodes, String name) {
		this.classCode = classCode;
		this.secCodes = secCodes;
		this.name = name;
	}

	public String prettySecCodes() {
		return String.join(delimiter, secCodes);
	}

	public String prettyclassCode() {
		return this.getClassCode();
	}

	@Override
	public String toString() {
		return "SubscriptionName: " + name + "\n" +
			"SubscriptionList{" + "\n" +
			"\tclassCode: " + classCode + "\n" +
			"\tsecCodes: " + String.join(delimiter, secCodes) + "\n" +
			"}";
	}
}
