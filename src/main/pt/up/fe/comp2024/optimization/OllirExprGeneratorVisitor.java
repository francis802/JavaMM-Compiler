package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(OBJECT_DECLARATION, this::visitObjDecl);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);

        setDefaultVisit(this::defaultVisit);
    }


    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitObjDecl(JmmNode node, Void unused) {
        var id = node.get("name");

        String code = "new(" + id + ")." + id;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder invoker = new StringBuilder();
        var varRef = node.getJmmChild(0);
        StringBuilder computation = new StringBuilder();
        boolean isStaticRef = false;
        for (var import_ : table.getImports()){
            if (import_.equals(varRef.get("name"))){
                isStaticRef = true;
                break;
            }
        }
        String methodName = '"'+node.get("name")+'"';
        if (isStaticRef){
            invoker.append("invokestatic(");
            invoker.append(varRef.get("name")).append(", ");
        }
        else {
            invoker.append("invokevirtual(");
            var varRefType = TypeUtils.getExprType(varRef, table);
            String varRefOllirType = OptUtils.toOllirType(varRefType);
            invoker.append(varRef.get("name")).append(varRefOllirType).append(", ");
        }
        invoker.append(methodName);
        for (int i = 1; i < node.getNumChildren(); i++) {
            var child = visit(node.getJmmChild(i));
            invoker.append(", ").append(child.getCode());
            computation.append(child.getComputation());
        }
        invoker.append(")");
        StringBuilder code = new StringBuilder();
        code.append(invoker);

        return new OllirExprResult(code.toString(), computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
