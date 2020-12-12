package plc.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Ast> {

    public Scope scope;

    public Analyzer(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Ast visit(Ast.Source ast) throws AnalysisException {
        for(int i = 0; i < ast.getStatements().size(); i++){
            visit(ast.getStatements().get(i));
        }
        return ast;
    }

    /**
     * Statically validates that visiting a statement returns a statement.
     */
    private Ast.Statement visit(Ast.Statement ast) throws AnalysisException {
        return (Ast.Statement) visit((Ast) ast);
    }

    @Override
    public Ast.Statement.Expression visit(Ast.Statement.Expression ast) throws AnalysisException {
        Ast.Expression expression = visit(ast.getExpression());
        if (expression instanceof Ast.Expression.Function) {
            return new Ast.Statement.Expression(expression);
        }
        throw new AnalysisException("not an Ast.Expression.Function");

    }

    @Override
    public Ast.Statement.Declaration visit(Ast.Statement.Declaration ast) throws AnalysisException {

        Optional<Ast.Expression> val = Optional.empty();
        Stdlib.Type leftType = Stdlib.getType(ast.getType());

        if (leftType.equals(Stdlib.Type.VOID)) {
            throw new AnalysisException("Type of variable cannot be Void");
        }
        scope.define(ast.getName(), leftType);

        if(ast.getValue().isPresent()) {
            Ast.Expression value = visit(ast.getValue().get());
            Stdlib.Type rightType = value.getType();
            checkAssignable(leftType, rightType);
            visit(new Ast.Statement.Assignment(ast.getName(), ast.getValue().get()));
            val = Optional.of(value);
        }
        
        return new Ast.Statement.Declaration(ast.getName(), leftType.getJvmName(), val); 

    }

    @Override
    public Ast.Statement.Assignment visit(Ast.Statement.Assignment ast) throws AnalysisException {

        Ast.Expression expression = visit(ast.getExpression());

        Stdlib.Type leftType = scope.lookup(ast.getName());
        Stdlib.Type rightType = expression.getType();
        checkAssignable(leftType, rightType);

        return new Ast.Statement.Assignment(ast.getName(), expression);

    }

    @Override
    public Ast.Statement.If visit(Ast.Statement.If ast) throws AnalysisException {

        Ast.Expression expression = visit(ast.getCondition());
        List<Ast.Statement> thenStatements = new ArrayList<>();
        List<Ast.Statement> elseStatements = new ArrayList<>();

        for (int i = 0; i < ast.getThenStatements().size(); i++) {
           thenStatements.add(visit(ast.getThenStatements().get(i)));
        }

        for (int i = 0; i < ast.getElseStatements().size(); i++) {
          elseStatements.add(visit(ast.getElseStatements().get(i)));
        }

        if (expression.getType() != Stdlib.Type.BOOLEAN) {
            throw new AnalysisException("Condition must be of Boolean type");
        } else if (ast.getThenStatements().size() == 0) {
            throw new AnalysisException("Statements list is empty");
        } else {
            return new Ast.Statement.If(expression, thenStatements, elseStatements);
        }
    }

    @Override
    public Ast.Statement.While visit(Ast.Statement.While ast) throws AnalysisException {

        Ast.Expression expression = visit(ast.getCondition());
        List<Ast.Statement> statements = new ArrayList<>();
        for (int i = 0; i < ast.getStatements().size(); i++) {
           statements.add(visit(ast.getStatements().get(i)));
        }

        if (expression.getType().equals(Stdlib.Type.BOOLEAN)) {
            return new Ast.Statement.While(expression, statements);
        }

        throw new AnalysisException("Condition must be of Boolean type");

    }

    /**
     * Statically validates that visiting an expression returns an expression.
     */
    private Ast.Expression visit(Ast.Expression ast) throws AnalysisException {
        return (Ast.Expression) visit((Ast) ast);
    }

    @Override
    public Ast.Expression.Literal visit(Ast.Expression.Literal ast) throws AnalysisException {
        if(ast.getValue() instanceof Boolean){
            return new Ast.Expression.Literal(Stdlib.Type.BOOLEAN, ast.getValue());
        }else if(ast.getValue() instanceof BigInteger){
            if(((BigInteger) ast.getValue()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) == 1){
                throw new AnalysisException("Invalid Literal: Integer too large");
            }else return new Ast.Expression.Literal(Stdlib.Type.INTEGER,((BigInteger) ast.getValue()).intValue());
        }else if(ast.getValue() instanceof BigDecimal){
            if(((BigDecimal) ast.getValue()).compareTo(BigDecimal.valueOf(Double.MAX_VALUE)) == 1){
                throw new AnalysisException("Invalid Literal: Decimal too large");
            }else return new Ast.Expression.Literal(Stdlib.Type.DECIMAL,((BigDecimal) ast.getValue()).doubleValue());
        }else if(ast.getValue() instanceof String){
            String test = (String) ast.getValue();
            Pattern pattern = Pattern.compile("[A-Za-z0-9_!?./+-/*]*");
            Matcher matcher = pattern.matcher(test);
            boolean matches = matcher.matches();
            if(matches == false){
                throw new AnalysisException("Invalid Literal: invalid string");
            }else return new Ast.Expression.Literal(Stdlib.Type.STRING,ast.getValue());
        }else throw new AnalysisException("Invalid Literal");
    }

    @Override
    public Ast.Expression.Group visit(Ast.Expression.Group ast) throws AnalysisException {
        Ast.Expression expression = visit(ast.getExpression()); //visit first, then getType
        return new Ast.Expression.Group(expression.getType(), expression);
    }

    @Override
    public Ast.Expression.Binary visit(Ast.Expression.Binary ast) throws AnalysisException {
        if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")){
            Ast.Expression left = visit(ast.getLeft());
            Ast.Expression right = visit(ast.getRight());
            if(left.getType() == Stdlib.Type.VOID || right.getType() == Stdlib.Type.VOID){
                throw new AnalysisException("Invalid Boolean Binary Expression");
            }else return new Ast.Expression.Binary(Stdlib.Type.BOOLEAN,ast.getOperator(),left,right);

        }else if(ast.getOperator().equals("+")){
            Ast.Expression left = visit(ast.getLeft());
            Ast.Expression right = visit(ast.getRight());
            if((left.getType() == Stdlib.Type.STRING && right.getType() != Stdlib.Type.VOID) || (right.getType() == Stdlib.Type.STRING && right.getType() != Stdlib.Type.VOID)){
                return new Ast.Expression.Binary(Stdlib.Type.STRING, ast.getOperator(), left,right);
            }else if(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.INTEGER){
                return new Ast.Expression.Binary(Stdlib.Type.INTEGER,ast.getOperator(),left,right);
            }else if((left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.INTEGER) ||(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.DECIMAL) || (left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.DECIMAL)){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), left,right);
            }else throw new AnalysisException("Invalid addition operation");
        }else if(ast.getOperator().equals("-")){
            Ast.Expression left = visit(ast.getLeft());
            Ast.Expression right = visit(ast.getRight());
            if(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.INTEGER){
                return new Ast.Expression.Binary(Stdlib.Type.INTEGER,ast.getOperator(),left,right);
            }else if((left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.INTEGER) ||(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.DECIMAL) || (left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.DECIMAL)){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), left,right);
            }else throw new AnalysisException("Invalid minus operation");
        }else if(ast.getOperator().equals("*")){
            Ast.Expression left = visit(ast.getLeft());
            Ast.Expression right = visit(ast.getRight());
            if(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.INTEGER){
                return new Ast.Expression.Binary(Stdlib.Type.INTEGER,ast.getOperator(),left,right);
            }else if((left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.INTEGER) ||(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.DECIMAL) || (left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.DECIMAL)){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), left,right);
            }else throw new AnalysisException("Invalid star operation");
        }else if(ast.getOperator().equals("/")){
            Ast.Expression left = visit(ast.getLeft());
            Ast.Expression right = visit(ast.getRight());
            if(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.INTEGER){
                return new Ast.Expression.Binary(Stdlib.Type.INTEGER,ast.getOperator(),left,right);
            }else if((left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.INTEGER) ||(left.getType() == Stdlib.Type.INTEGER && right.getType() == Stdlib.Type.DECIMAL) || (left.getType() == Stdlib.Type.DECIMAL && right.getType() == Stdlib.Type.DECIMAL)){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), left,right);
            }else throw new AnalysisException("Invalid divison operation");
        }else throw new AnalysisException("Invalid binary expression");
    }

    @Override
    public Ast.Expression.Variable visit(Ast.Expression.Variable ast) throws AnalysisException {
        if(scope.lookup(ast.getName()) == null){
            throw new AnalysisException("The variable is not defined");
        }else{
            if((Object) scope.lookup(ast.getName()) instanceof Boolean){
                return new Ast.Expression.Variable(Stdlib.Type.BOOLEAN,ast.getName());
            }else if((Object) scope.lookup(ast.getName()) instanceof Integer){
                return new Ast.Expression.Variable(Stdlib.Type.INTEGER,ast.getName());
            }else if((Object) scope.lookup(ast.getName()) instanceof Double){
                return new Ast.Expression.Variable(Stdlib.Type.DECIMAL,ast.getName());
            }else if((Object) scope.lookup(ast.getName()) instanceof String){
                return new Ast.Expression.Variable(Stdlib.Type.STRING,ast.getName());
            } else throw new AnalysisException("The variable is not of the desired type");
        }
    }

    @Override
    public Ast.Expression.Function visit(Ast.Expression.Function ast) throws AnalysisException {
        String name = ast.getName();
        int arity = ast.getArguments().size();
        Stdlib.Function func =  Stdlib.getFunction(name,arity);
        Ast.Expression temp = ast;
        if(func != null){
            List<Ast.Expression> visitedArgs = new ArrayList<Ast.Expression>();
            List<Stdlib.Type> parameterTypes = func.getParameterTypes();
            for(int i = 0; i < ast.getArguments().size(); i++){
                temp = visit(ast.getArguments().get(i));
                visitedArgs.add(temp);
            }
            for(int j = 0; j < parameterTypes.size(); j++){

                checkAssignable(visitedArgs.get(j).getType(),parameterTypes.get(j));
            }
            return new Ast.Expression.Function(func.getReturnType(),func.getJvmName(), visitedArgs);
        }else throw new AnalysisException("The function has not been defined");

    }

    /**
     * Throws an AnalysisException if the first type is NOT assignable to the target type. * A type is assignable if and only if one of the following is true:
     *  - The types are equal, as according to Object#equals
     *  - The first type is an INTEGER and the target type is DECIMAL
     *  - The first type is not VOID and the target type is ANY
     */
    public static void checkAssignable(Stdlib.Type type, Stdlib.Type target) throws AnalysisException {
        if(type.equals(target)){

        }else if(type == Stdlib.Type.INTEGER && target == Stdlib.Type.DECIMAL){

        }else if(type != Stdlib.Type.VOID && target == Stdlib.Type.ANY){

        }else throw new AnalysisException("The first type is NOT assignable to the target type");
    }

}
