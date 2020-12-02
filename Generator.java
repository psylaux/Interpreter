package plc.compiler;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 2;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        print("public final class Main {");
        newline(0);
        newline(1);
        print("public static void main(String[] args) {");
        for(int i = 0; i < ast.getStatements().size(); i++){
           newline(2);
            visit(ast.getStatements().get(i));
        }
        newline(1);
        print("}");
        newline(0);
        newline(0);
        print("}");
        newline(0);


        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        print(ast.getType(), " ", ast.getName());
        if(ast.getValue().isPresent()){
            print(" ","="," ",ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        print(ast.getName(), " ","="," ",ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        print("if (");
        print(ast.getCondition());
        print(") {");
        if (ast.getThenStatements().size() > 0) {
            ++indent;
            newline(indent);
            print(ast.getThenStatements().get(0));
            for(int i = 1; i < ast.getThenStatements().size(); i++) {
                if (i != ast.getThenStatements().size()-1) {
                    print(ast.getThenStatements().get(i));
                    newline(indent);
                }
            }
        } else { print("}");}
        if (!ast.getElseStatements().isEmpty()) {
            newline(--indent);
            print("} else {");
            newline(++indent);
            print(ast.getElseStatements().get(0));
            for(int i = 1; i < ast.getElseStatements().size(); i++) {
                if (i != ast.getElseStatements().size()-1) {
                    print(ast.getElseStatements().get(i));
                    newline(++indent);
                }
            }
        }
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        print("while (");
        visit(ast.getCondition());
        print(") {");
        for (int i = 0; i < ast.getStatements().size(); i++) {
            newline(3);
            visit(ast.getStatements().get(i));
        }
        newline(2);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        if(ast.getValue() instanceof String){
            print("\"");
            print(ast.getValue());
            print("\"");
        }else print(ast.getValue());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        print(ast.getLeft()," ", ast.getOperator()," ", ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Variable ast) {

        print(ast.getName());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        print(ast.getName(),"(");
        for(int i = 0; i < ast.getArguments().size(); i++){
            visit(ast.getArguments().get(i));
        }
        print(")");
        return null;
    }

}
