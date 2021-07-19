package org.wltea.analyzer.help;

import java.util.Objects;

/**
 * DefaultValueHelper
 *
 * @author Qicz
 * @since 2021/7/12 13:40
 */
public final class DefaultValueHelper {

	public static <T> T defaultIfNull(T obj, T defaultValue) {
		return Objects.isNull(obj) ? defaultValue : obj;
	}
}
