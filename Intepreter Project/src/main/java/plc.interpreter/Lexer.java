package plc.interpreter;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException}.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are
 * helpers, they're not necessary but their use will make the implementation a
 * lot easier. Regex isn't the most performant way to go but it gets the job
 * done, and the focus here is on the concept.
 */
public final class Lexer {

     final CharStream chars;

     Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Lexes the input and returns the list of tokens.
     */
    public static List<Token> lex(String input) throws ParseException {
        return new Lexer(input).lex();
    }

    /**
     * Repeatedly lexes the next token using {@link #lexToken()} until the end
     * of the input is reached, returning the list of tokens lexed. This should
     * also handle skipping whitespace.
     */
     List<Token> lex() throws ParseException {

            List<Token> result = new ArrayList<Token>();
          //  Token token = lexToken();
            //System.out.println(token);
           // result.add(token);
            while(chars.index != chars.input.length()){
                if(chars.get(0) == ' ' ){
                    chars.advance();
                    chars.reset();
                }
                else if(chars.get(0) == ' ' && chars.input.length() == 1){
                    return result;
                }
               // System.out.println(chars.get(0));
                Token temp = lexToken();
                result.add(temp);

            }
        return result;
    }

    /**
     * Lexes the next token. It may be helpful to have this call other methods,
     * such as {@code lexIdentifier()} or {@code lexNumber()}, based on the next
     * character(s).
     *
     * Additionally, here is an example of lexing a character literal (not used
     * in this assignment) using the peek/match methods below.
     *
     * <pre>
     * {@code
     *     private plc.interpreter.Token lexCharacter() {
     *         if (!match("\'")) {
     *             //Your lexer should prevent this from happening, as it should
     *             // only try to lex a character literal if the next character
     *             // begins a character literal.
     *             //Additionally, the index being passed back is a 'ballpark'
     *             // value. If we were doing proper diagnostics, we would want
     *             // to provide a range covering the entire error. It's really
     *             // only for debugging / proof of concept.
     *             throw new ParseException("Next character does not begin a character literal.", chars.index);
     *         }
     *         if (!chars.has(0) || match("\'")) {
     *             throw new ParseException("Empty character literal.",  chars.index);
     *         } else if (match("\\")) {
     *             //lex escape characters...
     *         } else {
     *             chars.advance();
     *         }
     *         if (!match("\'")) {
     *             throw new ParseException("Unterminated character literal.", chars.index);
     *         }
     *         return chars.emit(Token.Type.CHARACTER);
     *     }
     * }
     * </pre>
     */
     Token lexToken() throws ParseException {
         Token token;
         if(peek("[+-0-9]") || chars.get(0) >= '0' && chars.get(0) <= '9'){ // the next character is a + or - or a digit
             chars.index++;
             if(peek("[0-9]") || chars.get(-1) >= '0' && chars.get(-1) <= '9'){ // the next next character is a digit or the next character is a digit
                 chars.index--;
                 token = lexNumber();
             }else {
                 chars.index--;
                 token = lexIdentifier();
             }

             return token;
         }
         else if(peek("[_a-zA-Z\\:\\!\\?\\<\\>\\=\\.]")){
             token = lexIdentifier();
             return token;
         }else if(peek("[()#.]")){
             token = lexOperator();
             return token;
         }
         else{
                 throw new ParseException("Unterminated character literal.", chars.index);
             }
         }



     Token lexIdentifier() {
        if(match("[+-_a-zA-Z\\:\\!\\?\\<\\>\\=\\.]")){
            while(match("[._a-zA-Z-\\:\\!\\?\\<\\>\\=\\.]")){
            }
            return chars.emit(Token.Type.IDENTIFIER);
        }
         throw new ParseException("Invalid Identifier", chars.index);
    }

     Token lexNumber() {

        if(match("[0-9-+]")){
            while (match("[0-9.]")) {
            }

            if(chars.get(-1) == '.'){
                chars.reset();
            }
            //TEST CASE LIKE 58.01.29.3
            return chars.emit(Token.Type.NUMBER);
        }else{
            throw new ParseException("Invalid Number", chars.index);
        }

    }
     Token lexString() throws ParseException {
        throw new UnsupportedOperationException(); //TODO
    }
    Token lexOperator() throws ParseException {
        if(match("[()#.]")){
            return chars.emit(Token.Type.OPERATOR);
        }else {
            throw new ParseException("Invalid Operator", chars.index);
        }
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true for the sequence {@code 'a', 'b', 'c'}
     */
     boolean peek(String... patterns) {
         boolean matches = false;

         int peekIndex = 0;

             for(String patString : patterns){
                 Pattern pattern = Pattern.compile(patString);
                     Matcher matcher = pattern.matcher(chars.input.valueOf(chars.get(peekIndex)));

                     matches = matcher.matches();
                     if(matches == false){
                         return matches;
                     }
                    if(chars.has(peekIndex+1)){
                        peekIndex++;
                    }
             }
             return matches;
    }

    /**
     * Returns true in the same way as peek, but also advances the CharStream to
     * if the characters matched.
     */
     boolean match(String... patterns) {
         boolean matches = false;
         for(String patString : patterns){
             Pattern pattern = Pattern.compile(patString);
             if (chars.has(0) == false){
                 break;
             }

             Matcher matcher = pattern.matcher(chars.input.valueOf(chars.get(0)));

             matches = matcher.matches();
             if(matches == false && chars.input.length() > patterns.length){
                 return matches;
             }else if(matches == false){
                 chars.index = chars.index-chars.length;
                 return matches;
             }
             if(matches == true && chars.input.length() < patterns.length && chars.has(1) == false){
                continue;
             } else if(matches == true && chars.input.length() < patterns.length ){
                 continue;
             } else if(matches == true){
                 chars.advance();
             }
         }
         return matches;
    }

    /**
     * This is basically a sequence of characters. The index is used to maintain
     * where in the input string the lexer currently is, and the builder
     * accumulates characters into the literal value for the next token.
     */
     static final class CharStream {

         final String input; // the string being lexed

         int index = 0; // start
         int length = 0; // current or offset
         CharStream(String input) {
            this.input = input;
        }

        /**
         * Returns true if there is a character at index + offset.
         */
        public boolean has(int offset) {
            if(index + offset <= input.length()-1){
                return true;
            }else return false;
        }

        /**
         * Gets the character at index + offset.
         */
        public char get(int offset) {
                if(has(offset)){
                    return input.charAt(index+offset);
                }else{
                    System.out.println("GET FAILURE WITH INDEX: " + index);
                    System.out.println("string length: "+ input.length());
                    return ' ';
                }
        }

        /**
         * Advances to the next character, incrementing the current index and
         * length of the literal being built.
         */
        public void advance() {
            index++;
            length++;
        }

        /**
         * Resets the length to zero, skipping any consumed characters.
         */
        public void reset() {
            length = 0;
        }

        /**
         * Returns a token of the given type with the built literal and resets
         * the length to zero. The index of the token should be the
         * <em>starting</em> index.
         */
        public Token emit(Token.Type type) {
            String text = input.substring(index-length,index);
            Token token = new Token(type,text,index-length);
            reset();
            return token;
        }

    }

}
