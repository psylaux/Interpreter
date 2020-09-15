package plc.interpreter;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regex's as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._-]+@[A-Za-z0-9-]*\\.[a-z]{2,3}"),
            FILE_NAMES = Pattern.compile("([A-Za-z]+)([.]{1}[A-Za-z])*[@]{1}([A-Za-z]+[.]{1}[A-Za-z]+)*[.]{1}(([j][a][v][a]){1}|([c][l][a][s][s]){1})"),
            EVEN_STRINGS = Pattern.compile("([A-Za-z]{10})|([A-Za-z]{12})|([A-Za-z]{14})|([A-Za-z]{16})|([A-Za-z]{18})|([A-Za-z]{20})"),
            INTEGER_LIST = Pattern.compile("\\[(\\d+)(,[ ]{0,1}\\d+)*\\]"),
            IDENTIFIER = Pattern.compile("(^([_a-zA-Z0-9\\+\\-\\:\\!\\?\\<\\>\\=\\.][^0-9.]))([a-zA-Z0-9\\+\\-\\:\\!\\?\\<\\>\\=\\.])*"),
            NUMBER = Pattern.compile("^([0-9]+|[\\+\\-][0-9])(\\.[0-9])?[0-9]*$"),
            STRING = Pattern.compile("^\\\"([^\\\\]|(\\\\b)*|(\\\\n)*|(\\\\r)*|(\\\\t)*|(\\\\\\')*|(\\\\\\\")*|(\\\\\\\\)*)*\\\"$");

}
