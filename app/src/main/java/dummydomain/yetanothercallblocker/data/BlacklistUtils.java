package dummydomain.yetanothercallblocker.data;

import android.text.TextUtils;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BlacklistUtils {

    private static final Pattern LEGACY_PATTERN = Pattern.compile("\\+?[0-9%_]+");

    public static boolean isValidPattern(String pattern) {
        return !TextUtils.isEmpty(pattern) && patternError(pattern) == null;
    }

    /**
     * @return a description of the syntax problem, or null if the pattern compiles.
     * An empty pattern compiles, so callers that reject it have to check for it
     * themselves and can show their own message (see {@link #isValidPattern(String)}).
     */
    public static String patternError(String pattern) {
        if (TextUtils.isEmpty(pattern)) return null;

        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            String description = e.getDescription();
            return !TextUtils.isEmpty(description) ? description : e.getMessage();
        }

        return null;
    }

    public static boolean matches(String pattern, String normalizedNumber) {
        if (TextUtils.isEmpty(pattern) || TextUtils.isEmpty(normalizedNumber)) return false;

        try {
            return Pattern.compile(pattern).matcher(normalizedNumber).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /**
     * Converts a pattern of the old SQL LIKE flavour ({@code %} for any number of digits,
     * {@code _} for exactly one) into the equivalent regular expression. A pattern that
     * contains anything else is already a regular expression and is returned unchanged.
     * <p>
     * This runs once over the database at the schema 1 to 2 upgrade, and the CSV importer
     * applies it to the old human readable form after {@link #legacyHumanReadableToPattern}.
     */
    public static String legacyPatternToRegex(String legacyPattern) {
        if (legacyPattern == null || !LEGACY_PATTERN.matcher(legacyPattern).matches()) {
            return legacyPattern;
        }

        StringBuilder regex = new StringBuilder(legacyPattern.length() + 4);
        for (int index = 0; index < legacyPattern.length(); index++) {
            char character = legacyPattern.charAt(index);
            switch (character) {
                case '%':
                    regex.append(".*");
                    break;
                case '_':
                    regex.append('.');
                    break;
                case '+':
                    regex.append("\\+");
                    break;
                default:
                    regex.append(character);
                    break;
            }
        }

        return regex.toString();
    }

    /**
     * Translates the wildcards of the old human readable form, as written by old backups,
     * into the old stored form. Only the CSV importers need this.
     */
    public static String legacyHumanReadableToPattern(String pattern) {
        return pattern.replace('*', '%').replace('#', '_');
    }

}
