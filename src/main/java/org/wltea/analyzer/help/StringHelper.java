package org.wltea.analyzer.help;

import java.util.*;
import java.util.stream.Collectors;

/**
 * StringHelper
 *
 * @author Qicz
 * @since 2021/7/9 14:39
 */
public final class StringHelper {

	public static boolean nonBlank(String string) {
		return Objects.nonNull(string) && !"".equals(string.trim());
	}

	public static List<String> filterBlank(Collection<String> strings) {
		if (Objects.isNull(strings)) {
			return Collections.emptyList();
		}
		return strings.stream().filter(StringHelper::nonBlank).map(String::trim).collect(Collectors.toList());
	}

	public static Set<String> filterBlank(Set<String> strings) {
		if (Objects.isNull(strings)) {
			return Collections.emptySet();
		}
		return strings.stream().filter(StringHelper::nonBlank).map(String::trim).collect(Collectors.toSet());
	}
}
