package plc.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the tokens and returns the parsed AST.
     */
    public static Ast parse(List<Token> tokens) throws ParseException {
        return new Parser(tokens).parseSource();
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException { // DONE

        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while(tokens.has(0)){

            statements.add(parseStatement());
            tokens.advance();


        }
        return new Ast.Source(statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, assignment, if, or while
     * statement, then it is an expression statement. See these methods for
     * clarification on what starts each type of statement.
     */
    public Ast.Statement parseStatement() throws ParseException { // DONE
        if(peek("LET")){
           return parseDeclarationStatement();
        }
        else if(peek("IF")){
            tokens.advance();
            return parseIfStatement();
        }else if(peek("WHILE")){
            tokens.advance();
             return parseWhileStatement();
        }else if(peek(Token.Type.IDENTIFIER)){
            if(tokens.get(1).getLiteral() != "="){
                return parseExpressionStatement();
            }
            return parseAssignmentStatement();
        }else return parseExpressionStatement();
    }

    /**
     * Parses the {@code expression-statement} rule. This method is called if
     * the next tokens do not start another statement type, as explained in the
     * javadocs of {@link #parseStatement()}.
     */
    public Ast.Statement.Expression parseExpressionStatement() throws ParseException {
        Ast.Expression e0 = parseExpression();

        return new Ast.Statement.Expression(e0);
    }

    /**
     * Parses the {@code declaration-statement} rule. This method should only be
     * called if the next tokens start a declaration statement, aka {@code let}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        if(peek("LET")){
            String name = "";
            String type = "";

            boolean empty = true;
            Optional<Ast.Expression> value = Optional.empty();
            tokens.advance();
            if(peek(Token.Type.IDENTIFIER)){
                name = tokens.get(0).getLiteral();
                tokens.advance();
            }
            if(peek(":")){
                tokens.advance();
            }
            if(peek(Token.Type.IDENTIFIER)){
                 type = tokens.get(0).getLiteral();
                tokens.advance();
            }
            if(peek("=")){
                tokens.advance();
                empty = false;
                value = Optional.of(parseExpression());

            }
             if(!peek(";")){
                 tokens.advance();
             }
             if(empty == false){
                 return new Ast.Statement.Declaration(name,type,value);
             }else return new Ast.Statement.Declaration(name,type,value);


        }else throw new ParseException("Invalid Declaration Statement", tokens.index);
    }

    /**
     * Parses the {@code assignment-statement} rule. This method should only be
     * called if the next tokens start an assignment statement, aka both an
     * {@code identifier} followed by {@code =}.
     */
    public Ast.Statement.Assignment parseAssignmentStatement() throws ParseException {
        String placeholder = "";
        Ast.Expression express = new Ast.Expression.Literal(placeholder);
        if(peek(Token.Type.IDENTIFIER)){
            Token ident = tokens.get(0);
            String name = tokens.get(0).getLiteral();
            express = new Ast.Expression.Variable(tokens.get(0).getLiteral());
            tokens.advance();
            if(peek("=")){
                tokens.advance();
            }else throw new ParseException("Invalid Assignment Statement missing equal sign", tokens.index);
            Ast.Expression value = parseExpression();
            return new Ast.Statement.Assignment(name,value);
        }else throw new ParseException("Invalid Assignment Statement", tokens.index);
    }

    /**
     * Parses the {@code if-statement} rule. This method should only be called
     * if the next tokens start an if statement, aka {@code if}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        String placeholder = "";
        Ast.Expression condition = parseExpression();
        if(peek("THEN")){
            tokens.advance();
            List<Ast.Statement> thenStatements = new ArrayList<Ast.Statement>();
            List<Ast.Statement> elseStatements = new ArrayList<Ast.Statement>();
            boolean elseconfirmed = false;
            while(!peek("END")){
                if(tokens.get(0).getLiteral() == "ELSE"){
                    elseconfirmed = true;
                    tokens.advance();
                }
                if(elseconfirmed == false){
                    Ast.Statement state = parseStatement();
                    thenStatements.add(state);
                    tokens.advance();
                }
                if(elseconfirmed == true){
                    Ast.Statement state = parseStatement();
                    elseStatements.add(state);
                    tokens.advance();
                }



            }

            return new Ast.Statement.If(condition,thenStatements,elseStatements);
        }else throw new ParseException("Invalid While Statement missing Then", tokens.index);
    }

    /**
     * Parses the {@code while-statement} rule. This method should only be
     * called if the next tokens start a while statement, aka {@code while}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        String placeholder = "";
        Ast.Expression condition = parseExpression();
        if(peek("DO")){
            tokens.advance();
            List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
            while(!peek("END")){
                Ast.Statement state = parseStatement();
                statements.add(state);
                tokens.advance();
                if(tokens.get(0).getLiteral() == ";"){
                    tokens.advance();
                }

            }
            return new Ast.Statement.While(condition,statements);
        }else throw new ParseException("Invalid While Statement missing Do", tokens.index);

    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        String placeholder = "";
        Ast.Expression express = new Ast.Expression.Literal(placeholder);
        if(peek(Token.Type.DECIMAL)){
            express = new Ast.Expression.Literal(new BigDecimal(tokens.get(0).getLiteral()));
            tokens.advance();
            if(peek(Token.Type.IDENTIFIER)){
                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
            }
        }else if(peek(Token.Type.INTEGER)){
            express = new Ast.Expression.Literal(new BigInteger(tokens.get(0).getLiteral()));
            tokens.advance();
            if(peek(Token.Type.IDENTIFIER)){
                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
            }
        }else if(peek(Token.Type.STRING)){
            String lit = tokens.get(0).getLiteral();
            String sub = lit.substring(1,lit.length()-1);
            express = new Ast.Expression.Literal(sub);
            tokens.advance();
            if(peek(Token.Type.IDENTIFIER)){
                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
            }
        }else if(peek(Token.Type.IDENTIFIER) && !peek("TRUE") && !peek("FALSE")){
            Token ident = tokens.get(0);
            express = new Ast.Expression.Variable(tokens.get(0).getLiteral());
            tokens.advance();
            if(peek(Token.Type.IDENTIFIER)){
                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
            }else if(peek("(")){
                tokens.advance();
                List<Ast.Expression> args = new ArrayList<Ast.Expression>();
                while(!peek(")")){
                    if(peek(",")){
                        tokens.advance();
                    }
                    args.add(parseExpression());
                }
                express = new Ast.Expression.Function(ident.getLiteral(), args);
            }
        }else if(peek("TRUE")){
            express = new Ast.Expression.Literal(true);
            tokens.advance();
//            if(peek(";")){
//                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
//            }
        }else if(peek("FALSE")){
            express = new Ast.Expression.Literal(false);
            tokens.advance();
//            if(peek(";")){
//                return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
//            }
        }else if(peek("(")){
            tokens.advance();
            express =  new Ast.Expression.Group(parseExpression());
            if(peek(")")){
                tokens.advance();
            }
        }
        boolean twoarg = false;
        Ast.Expression express2 = new Ast.Expression.Literal(placeholder);
        Token token = new Token(Token.Type.OPERATOR,"-",0);
        if(peek("*") ||peek("/")){
            token = tokens.get(0);
            express2 = parseMultiplicativeExpression();
            twoarg = true;
        }else if(peek("==") ||peek("!=") ){
            token = tokens.get(0);
            express2 = parseEqualityExpression();
            twoarg = true;
        }else if(peek("+") || peek("-")){
            token = tokens.get(0);
            express2 = parseAdditiveExpression();
            twoarg = true;
        }
        if(twoarg == false) {
            return express;
        }
        else{
            return new Ast.Expression.Binary(token.getLiteral(),express,express2);
        }

    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseEqualityExpression() throws ParseException {
        String placeholder = "";
        Ast.Expression e0 = new Ast.Expression.Literal(placeholder);
        Token operator = new Token(Token.Type.OPERATOR,"-",0);
        if (peek("==") ||peek("!=")) {
            tokens.advance();
            e0 = parsePrimaryExpression();
        } else throw new ParseException("Invalid Equality Expression", tokens.index);
        boolean valid = true;
        while (valid == true) {
            if (peek("==") ||peek("!=")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseEqualityExpression());
            } else if (peek("+") || peek("-")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseAdditiveExpression());
            } else {
                valid = false;
                break;
            }
        }
        return e0;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        String placeholder = "";
        Ast.Expression e0 = new Ast.Expression.Literal(placeholder);
        Token operator = new Token(Token.Type.OPERATOR,"-",0);
        if (peek("-") ||peek("+")) {
            tokens.advance();
            e0 = parsePrimaryExpression();
        } else throw new ParseException("Invalid Equality Expression", tokens.index);
        boolean valid = true;
        while (valid == true) {
            if (peek("*") ||peek("/")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseMultiplicativeExpression());
            } else if (peek("-") ||peek("+")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseAdditiveExpression());
            } else {
                valid = false;
                break;
            }
        }
        return e0;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        String placeholder = "";
        Ast.Expression e0 = new Ast.Expression.Literal(placeholder);
        Token operator = new Token(Token.Type.OPERATOR,"-",0);
        if (peek("*") ||peek("/")) {
            tokens.advance();
            e0 = parsePrimaryExpression();
        } else throw new ParseException("Invalid Equality Expression", tokens.index);
        boolean valid = true;
        while (valid == true) {
            if (peek("*") ||peek("/")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseMultiplicativeExpression());
            } else if (peek("+") ||peek("-")) {
                operator = tokens.get(0);
                e0 = new Ast.Expression.Binary(operator.getLiteral(),e0,parseAdditiveExpression());
            } else {
                valid = false;
                break;
            }
        }
        return e0;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if(peek(Token.Type.DECIMAL)){
                tokens.advance();
                return new Ast.Expression.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        }else if(peek(Token.Type.INTEGER)){
            tokens.advance();
            return new Ast.Expression.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        }else if(peek(Token.Type.STRING)){
            tokens.advance();
            String lit = tokens.get(-1).getLiteral();
            String sub = lit.substring(1,lit.length()-1);
            return new Ast.Expression.Literal(sub);
        }else if(peek(Token.Type.IDENTIFIER) && !peek("TRUE") && !peek("FALSE")){
            tokens.advance();
            Token ident = tokens.get(-1);
             if(peek("(")){
                tokens.advance();
                List<Ast.Expression> args = new ArrayList<Ast.Expression>();
                while(!peek(")")){
                    if(peek(",")){
                        tokens.advance();
                    }
                    args.add(parseExpression());

                }
                return new Ast.Expression.Function(ident.getLiteral(), args);
            }
            return new Ast.Expression.Variable(tokens.get(-1).getLiteral());
        }else if(peek("TRUE")){
            tokens.advance();
            return new Ast.Expression.Literal(true);
        }else if(peek("FALSE")){
            tokens.advance();
            return new Ast.Expression.Literal(false);
        }else if(peek("(")){
            tokens.advance();
            Ast.Expression e1 =  new Ast.Expression.Group(parseExpression());
            if(peek(")")){
                tokens.advance();
            }
            return e1;

        }else throw new ParseException("Invalid Primary Expression", tokens.index);
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
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError();
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
