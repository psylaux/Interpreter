package plc.interpreter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;
// change above and below back to private
    private Parser(String input) {
        tokens = new TokenStream(Lexer.lex(input));
    }

    /**
     * Parses the input and returns the AST
     */
    public static Ast parse(String input) {
        return new Parser(input).parse();
    }

    /**
     * Repeatedly parses a list of ASTs, returning the list as arguments of an
     * {@link Ast.Term} with the identifier {@code "source"}.
     */
    private Ast parse()  {
        List<Ast> results = new ArrayList<>();
        if(!peek("(") && !peek("[") && tokens.tokens.size() != 0){
            throw new ParseException("Expected opening bracket or parenthesis",tokens.index);
        }else if(tokens.tokens.size() == 0){
            return new Ast.Term("source",results);
        }

        while(tokens.index < tokens.tokens.size()-1){
            results.add(parseAst());
            if(results.contains(null)){
                throw new ParseException("No identifier",tokens.index);
            }

        }
        return new Ast.Term("source",results);
    }
    private Ast term(){
        List<Ast> emptyset = new ArrayList<>();
        Ast.Term empty = new Ast.Term("ending",emptyset);
        List<Ast> args = new ArrayList<>();
        String name = "empty";
        if(peek("(")){
            match("(");
            name = tokens.get(0).getLiteral();
            if(name.equals(")")){

                return null;
            }
            tokens.advance();

            args.add(parseAst());
            if(args.contains(empty)){
                System.out.println("we in here " + name);
                tokens.advance();
                return new Ast.Term(name,emptyset);
            }
            if(!peek("(") && !peek("[")){
                tokens.advance();
            }
            if(tokens.index >= tokens.tokens.size()){
                return new Ast.Term(name,args);
            }
            while(!peek(")")){
                if(tokens.index >= tokens.tokens.size()){
                    return new Ast.Term(name,args);
                }
                args.add(parseAst());
                tokens.advance();

            }
            match(")");
        }else if(peek("[")){
            match("[");
            name = tokens.get(0).getLiteral();
            if(name.equals("]")){

                return null;
            }
            tokens.advance();
            args.add(parseAst());
            if(args.contains(empty)){
                tokens.advance();
                return new Ast.Term(name,emptyset);
            }
            if(!peek("(") && !peek("[")){
                tokens.advance();
            }
            if(tokens.index <= tokens.tokens.size()){
                return new Ast.Term(name,args);
            }
            while(!peek("]")){
                if(tokens.index >= tokens.tokens.size()){
                    return new Ast.Term(name,args);
                }

                args.add(parseAst());
                tokens.advance();

            }
            match("]");
        }
        return new Ast.Term(name,args);
    }

    /**
     * Parses an AST from the given tokens based on the provided grammar. Like
     * the lexToken method, you may find it helpful to have this call other
     * methods like {@code parseTerm()}. In a recursive descent parser, each
     * rule in the grammar would correspond with a {@code parseX()} function.
     *
     * Additionally, here is an example of parsing a function call in a language
     * like Java, which has the form {@code name(args...)}.
     *
     * <pre>
     * {@code
     *     private Ast.FunctionExpr parseFunctionExpr() {
     *         //In a real parser this would be more complex, as the parser
     *         //wouldn't know this should be a function call until reaching the
     *         //opening parenthesis, like name(... <- here. You won't have this
     *         //problem in this project, but will for the compiler project.
     *         if (!match(Token.Type.IDENTIFIER)) {
     *             throw new ParseException("Expected the name of a function.");
     *         }
     *         String name = tokens.get(-1).getLiteral();
     *         if (!match("(")) {
     *             throw new ParseException("Expected opening bra
     *         }
     *         List<Ast> args = new ArrayList<>();
     *         while (!match(")")) {
     *             //recursive call to parseExpr(), not shown here
     *             args.add(parseExpr());
     *             //next token must be a closing parenthesis or comma
     *             if (!peek(")") && !match(",")) {
     *                 throw new ParseException("Expected closing parenthesis or comma after argument.", tokens.get(-1).getIndex());
     *             }
     *         }
     *         return new Ast.FunctionExpr(name, args);
     *     }
     * }
     * </pre>
     */
    private Ast parseAst() {
        List<Ast> args = new ArrayList<>();
        if (tokens.get(0).getType() == Token.Type.NUMBER) {
            return number();
        } else if (tokens.get(0).getType() == Token.Type.IDENTIFIER) {
            return identifier();
        } else if (tokens.get(0).getType() == Token.Type.STRING) {
            return string();
        } else if (peek("(") || peek("[")) {
            return term();
        } else if(peek(")") || peek("]")){
            List<Ast> eset = new ArrayList<>();
            return new Ast.Term("ending",eset);
        }
            else throw new ParseException("Expected closing parenthesis or comma after argument.", tokens.get(0).getIndex());
    }
    private Ast identifier(){
        return new Ast.Identifier(tokens.get(0).getLiteral());
    }
    private Ast number(){
        BigDecimal num = new BigDecimal(tokens.get(0).getLiteral());
            return new Ast.NumberLiteral(num);
    }
    private Ast string(){
        String replaced = tokens.get(0).getLiteral().replace("\"","");
        String replacedSecond = replaced.replace("\\n","\n");
        return new Ast.StringLiteral(replacedSecond);
    }


    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        boolean matches = true;
        int peekIndex = 0;
        if(tokens.tokens.size() == 0){
            return false;
        }
        for(Object pat : patterns){
            if(pat instanceof String){
                if(!tokens.get(peekIndex).getLiteral().equals(pat)){
                    matches = false;
                    return matches;
                }
            }else if(pat instanceof Token.Type){
                if(tokens.get(peekIndex).getType() != pat){
                    matches = false;
                    return false;
                }
            }
            if(tokens.has(peekIndex+1)){
                peekIndex++;
            }
        }
        return matches;
    }


    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean matches = true;
        if(tokens.tokens.size() == 0){
            return false;
        }
        for(Object pat : patterns){
            if(pat instanceof String){
                if(!tokens.get(0).getLiteral().equals(pat)){
                    matches = false;
                    return matches;
                }
            }else if(pat instanceof Token.Type){
                if(tokens.get(0).getType() != pat){
                    matches = false;
                    return matches;
                }
            }
            tokens.advance();
        }
        return matches;
    }

    private static final class TokenStream {
//change 127, 129 and 130 back to private
        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            if(index + offset <= tokens.size()-1){
                return true;
            }else return false;
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            if(has(offset)){
                return tokens.get(index+offset);
            }else{
                System.out.println("Get Failure with index: " + index + " and offset: " + offset);
                Token token = tokens.get(0);
                return token;
            }

        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
