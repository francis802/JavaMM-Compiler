package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);

        setDefaultVisit(this::defaultVisit);
    }


    private String visitAssignStmt(JmmNode node, Void unused) {

        var lhs = exprVisitor.visit(node.getJmmChild(0));
        var rhs = exprVisitor.visit(node.getJmmChild(1));

        StringBuilder code = new StringBuilder();
        // code to compute the children
        code.append(lhs.getComputation());
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);


        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);
        code.append(rhs.getCode());
        if(FUNCTION_CALL.check(node.getJmmChild(1))) {
            code.append(typeString);
        }

        code.append(END_STMT);

        return code.toString();
    }

    private String visitFunctionCall(JmmNode node, Void unused) {
        var func = exprVisitor.visit(node);

        StringBuilder code = new StringBuilder();
        code.append(func.getComputation());

        Type thisType = TypeUtils.getExprType(node.getJmmChild(0), table);
        String typeString = OptUtils.toOllirType(thisType);

        code.append(func.getCode());
        boolean isStatic = false;
        for (var import_ : table.getImports()) {
            if (import_.contains(node.get("name"))) {
                isStatic = true;
                break;
            }
        }
        if(isStatic) {
            code.append(".V");
        } else {
            code.append(typeString);
        }

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {
        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getJmmChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {
        System.out.println(node.getChildren().get(node.getNumChildren() - 1).getChildren());
        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");
        boolean isMain = NodeUtils.getBooleanAttribute(node, "isMain", "false");

        if (isPublic) {
            code.append("public ");
        }
        if (isMain) {
            code.append("static ");
        }


        // name
        var name = node.get("name");
        code.append(name);

        // param
        if(isMain){
            code.append("(args.array.String)");
        } else {
            code.append("(");

            var params = node.getChildren(PARAM);
            for (int i = 0; i < params.size(); i++) {
                var param = params.get(i);
                var paramCode = visit(param);
                code.append(paramCode);
                if (i < (params.size() - 1)) {
                    code.append(", ");
                }
            }
            code.append(")");
        }

        // type
        if(isMain){
            code.append(".V");
        }
        else {
            var retType = OptUtils.toOllirType(node.getJmmChild(0));
            code.append(retType);
        }
        code.append(L_BRACKET);

        // rest of its children stmts
        var afterParam = 2;

        if(node.getChildren(PARAM).isEmpty()) {
            afterParam = 1;
        }

        if(isMain) {
            afterParam = 0;
        }

        for (int i = afterParam; i < node.getNumChildren(); i++) {
            var child = node.getJmmChild(i);
            var childCode = visit(child);
            code.append(childCode);
        }

        if(isMain) {
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }



    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        if(!Objects.equals(table.getSuper(), "")){
            code.append(" extends ");
            code.append(table.getSuper());
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);
            if(VAR_DECL.check(child)) {
                var type = child.getJmmChild(0);
                result = ".field " + child.get("name") + OptUtils.toOllirType(type) + ";";
                code.append(NL);

            }

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }


    private String visitProgram(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        for (var child : node.getChildren()) {
            code.append(visit(child));
        }

        return code.toString();
    }
}
