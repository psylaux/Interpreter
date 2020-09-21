package plc.interpreter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regex's as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._-]+@[A-Za-z0-9-]*\\.[a-z]{2,3}"),
            FILE_NAMES = Pattern.compile("(?<name>^([a-zA-Z-])+)(\\.)[a-zA-Z-\\.]*(java|class)$"),
            EVEN_STRINGS = Pattern.compile("^(.{10})$|^(.{12})$|^(.{14})$|^(.{16})$|^(.{18})$|^(.{20})$"),
            INTEGER_LIST = Pattern.compile("\\[(\\d*)(,[ ]{0,1}\\d+)*\\]"),
            IDENTIFIER = Pattern.compile("^([_a-zA-Z\\+\\-\\:\\!\\?\\<\\>\\=\\.][^0-9.])([a-zA-Z0-9\\+\\-\\:\\!\\?\\<\\>\\=\\.])*"),
            NUMBER = Pattern.compile("^([0-9]+|[\\+\\-][0-9])(\\.[0-9])?[0-9]*$"),
            STRING = Pattern.compile("^\\\"([^\\\\]|(\\\\b)*|(\\\\n)*|(\\\\r)*|(\\\\t)*|(\\\\\\')*|(\\\\\\\")*|(\\\\\\\\)*)*\\\"$");

}
