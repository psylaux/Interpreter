package plc.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
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
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Statement.Declaration visit(Ast.Statement.Declaration ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Statement.Assignment visit(Ast.Statement.Assignment ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Statement.If visit(Ast.Statement.If ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Statement.While visit(Ast.Statement.While ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
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
            }else throw new AnalysisException("Invalid dash operation");
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
        if(func != null){
            List<Stdlib.Type> parameterTypes = func.getParameterTypes();
            for(int i = 0; i < ast.getArguments().size(); i++){
                visit(ast.getArguments().get(i));
            }
            for(int j = 0; j < parameterTypes.size(); j++){

                checkAssignable(ast.getArguments().get(j).getType(),parameterTypes.get(j));
            }
            return new Ast.Expression.Function(func.getReturnType(),func.getJvmName(), ast.getArguments());
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
