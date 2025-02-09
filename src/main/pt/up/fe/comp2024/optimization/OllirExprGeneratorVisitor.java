package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ollir.OllirUtils;
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
    private static final String NL = "\n";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private static int logicalAndCounter = 0;
    private static int trueFalseCounter = 0;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    private static int getNewAndCounter(){
        return logicalAndCounter++;
    }

    private static int getNewTrueFalseCounter(){
        return trueFalseCounter++;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(TRUE_LITERAL, this::visitBoolean);
        addVisit(FALSE_LITERAL, this::visitBoolean);
        addVisit(OBJECT_DECLARATION, this::visitObjDecl);
        addVisit(OBJECT, this::visitObjDecl);
        addVisit(FUNCTION_CALL, this::visitFunctionCall);
        addVisit(FIELD_CALL, this::visitFieldCall);
        addVisit(NEGATION, this::visitNegation);
        addVisit(PARENTHESIS, this::visitParenthesis);
        addVisit(ARRAY_DECLARATION, this::visitArrayDecl);
        addVisit(ARRAY_SUBS, this::visitArraySubs);
        addVisit(DESCRIBED_ARRAY, this::visitDescribedArray);
        addVisit(LENGTH, this::visitArrayLength);

        setDefaultVisit(this::defaultVisit);
    }

    private boolean beingAssigned(JmmNode node){
        if (ASSIGN_STMT.check(node.getParent())){
            return node.getParent().getJmmChild(0).equals(node);
        }
        return false;
    }

    private OllirExprResult visitArrayLength(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var expr = visit(node.getJmmChild(0));
        var childType = TypeUtils.getExprType(node.getJmmChild(0), table);
        computation.append(expr.getComputation());
        var temp = OptUtils.getTemp();
        var type = ".i32";
        computation.append(temp).append(type).append(SPACE);
        computation.append(ASSIGN).append(type).append(SPACE);
        computation.append("arraylength(").append(expr.getCode()).append(")").append(type).append(END_STMT);
        code.append(temp).append(type);
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArrayDecl(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        Type type = new Type(node.getJmmChild(0).get("name"), true);
        var ollirType = OptUtils.toOllirType(type);
        var item = visit(node.getJmmChild(1));
        computation.append(item.getComputation());
        var temp = OptUtils.getTemp();
        computation.append(temp).append(ollirType).append(SPACE);
        computation.append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(array, ").append(item.getCode()).append(")").append(ollirType).append(END_STMT);
        code.append(temp).append(ollirType);
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitArraySubs(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var array = visit(node.getJmmChild(0));
        var index = visit(node.getJmmChild(1));
        computation.append(array.getComputation());
        computation.append(index.getComputation());

        String arrayName = array.getCode();
        String refName = arrayName.split("\\.")[0];
        Type type = TypeUtils.getExprType(node.getJmmChild(0), table);
        String ollirType = OptUtils.toOllirType(new Type(type.getName(), false));
        String indexName = index.getCode();
        if (beingAssigned(node)){
            code.append(refName).append("[").append(indexName).append("]").append(ollirType);
            return new OllirExprResult(code.toString(), computation);
        }
        var temp = OptUtils.getTemp();
        computation.append(temp).append(ollirType).append(SPACE);
        computation.append(ASSIGN).append(ollirType).append(SPACE);
        computation.append(refName).append("[").append(indexName).append("]").append(ollirType).append(END_STMT);
        code.append(temp).append(ollirType);
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitDescribedArray(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var numbers = node.getChildren();
        String temp = OptUtils.getTemp();
        var type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);
        computation.append(temp).append(ollirType).append(SPACE);
        computation.append(ASSIGN).append(ollirType).append(SPACE);
        computation.append("new(array, ").append(numbers.size()).append(".i32").append(")").append(ollirType).append(END_STMT);
        var typeElem = OptUtils.toOllirType(new Type(type.getName(), false));
        for (int i = 0; i < numbers.size(); i++) {
            var number = visit(numbers.get(i));
            computation.append(number.getComputation());
            computation.append(temp).append("[").append(i).append(typeElem).append("]").append(typeElem).append(SPACE);
            computation.append(ASSIGN).append(typeElem).append(SPACE);
            computation.append(number.getCode()).append(END_STMT);
        }
        code.append(temp).append(ollirType);

        return new OllirExprResult(code.toString(), computation);
    }


    private OllirExprResult visitParenthesis(JmmNode node, Void unused) {
        return visit(node.getJmmChild(0));
    }

    private JmmNode getOffParenthesis(JmmNode node){
        while (PARENTHESIS.check(node)){
            node = node.getJmmChild(0);
        }
        return node;
    }

    private OllirExprResult visitNegation(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();
        var expr = visit(node.getJmmChild(0));
        computation.append(expr.getComputation());
        var temp = OptUtils.getTemp();
        var type = ".bool";
        computation.append(temp).append(type).append(SPACE);
        computation.append(ASSIGN).append(type).append(SPACE);
        computation.append("!.bool").append(SPACE).append(expr.getCode()).append(END_STMT);

        code.append(temp).append(type);
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        var boolType = new Type("boolean", false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = node.get("value") + ollirBoolType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {
        boolean logicalAndExpr = node.get("op").equals("&&");
        if (logicalAndExpr){
            int trueFalseCounter = getNewTrueFalseCounter();
            OllirExprResult ollir = visitLogicalAnd(node, trueFalseCounter);
            String resComp = ollir.getComputation();
            StringBuilder finalComp = new StringBuilder();
            finalComp.append(resComp);
            finalComp.append("ENDAND_").append(trueFalseCounter).append(":").append(NL);
            return new OllirExprResult(ollir.getCode(), finalComp);
        }

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

    private OllirExprResult visitLogicalAnd(JmmNode node, int trueFalseCounter){
        var rhs = visit(node.getJmmChild(1));
        var lhsNode = node.getJmmChild(0);
        OllirExprResult lhs;
        if (PARENTHESIS.check(lhsNode)){
            lhsNode = getOffParenthesis(lhsNode);
        }
        if (BINARY_EXPR.check(lhsNode) && lhsNode.get("op").equals("&&")){
            lhs = visitLogicalAnd(lhsNode, trueFalseCounter);
        }
        else {
            lhs = visit(lhsNode);
        }
        StringBuilder computation = new StringBuilder();
        int andCounter = getNewAndCounter();
        computation.append(rhs.getComputation());
        computation.append(lhs.getComputation());
        String temp = OptUtils.getTemp();
        String type = ".bool";
        String code = temp + type;
        computation.append("if (").append(rhs.getCode()).append(") goto AND_").append(andCounter).append(END_STMT);
        computation.append(code).append(SPACE).append(ASSIGN).append(".bool").append(SPACE).append("false.bool").append(END_STMT);
        computation.append("goto ENDAND_").append(trueFalseCounter).append(END_STMT);
        computation.append("AND_").append(andCounter).append(":").append(NL);
        computation.append(temp).append(type).append(SPACE);
        computation.append(ASSIGN).append(type).append(SPACE);
        computation.append(lhs.getCode()).append(END_STMT);
        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var method = node.getAncestor(METHOD_DECL).get();
        var locals = table.getLocalVariables(method.get("name"));
        var params = table.getParameters(method.get("name"));
        boolean isLocalParam = false;
        for (var local : locals){
            if (local.getName().equals(node.get("name"))){
                isLocalParam = true;
                break;
            }
        }
        for (var param : params){
            if (param.getName().equals(node.get("name"))){
                isLocalParam = true;
                break;
            }
        }
        if (!isLocalParam) {
            for (var field : table.getFields()) {
                if (field.getName().equals(node.get("name"))) {
                    var typeCode = OptUtils.toOllirType(field.getType());
                    var temp = OptUtils.getTemp();
                    computation.append(temp).append(typeCode).append(SPACE);
                    computation.append(ASSIGN).append(typeCode).append(SPACE);
                    computation.append("getfield(this, ").append(field.getName()).append(typeCode).append(")").append(typeCode).append(END_STMT);
                    code.append(temp).append(typeCode);
                    return new OllirExprResult(code.toString(), computation);
                }
            }
        }
        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table);
        String ollirType = OptUtils.toOllirType(type);

        code.append(id).append(ollirType);

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitFieldCall(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        StringBuilder code = new StringBuilder();
        var field = TypeUtils.getExprType(node, table);
        var typeCode = OptUtils.toOllirType(field);
        var temp = OptUtils.getTemp();
        computation.append(temp).append(typeCode).append(SPACE);
        computation.append(ASSIGN).append(typeCode).append(SPACE);
        computation.append("getfield(this, ").append(field.getName()).append(typeCode).append(")").append(typeCode).append(END_STMT);
        code.append(temp).append(typeCode);
        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitObjDecl(JmmNode node, Void unused) {
        String id;
        StringBuilder computation = new StringBuilder();
        var temp = OptUtils.getTemp();
        if (OBJECT.check(node)) {
            id = table.getClassName();
        } else {
            id = node.get("name");
        }
        var type = "." + id;
        computation.append(temp).append(type).append(SPACE);
        computation.append(ASSIGN).append(type).append(SPACE);
        computation.append("new(").append(id).append(")").append(type).append(END_STMT);
        String code = temp + type;
        computation.append("invokespecial(").append(code).append(", " + '"' + "<init>" + '"').append(").V").append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private boolean hasVarargs(JmmNode methodCall){
        String methodName = methodCall.get("name");
        if(methodCall.getAncestor(CLASS_DECL).isPresent()) {
            List<JmmNode> methods = methodCall.getAncestor(CLASS_DECL).get().getChildren(METHOD_DECL);
            for (var method : methods) {
                if (method.getChildren(PARAM).isEmpty())
                    continue;
                if (method.get("name").equals(methodName)) {
                    var param = method.getChildren(PARAM).get(0);
                    while (param.getNumChildren() > 0){
                        var type = param.getJmmChild(0);
                        if (type.get("isVarArgs").equals("true")){
                            System.out.println(type);
                            return true;
                        }
                        if (param.getNumChildren() < 2)
                            break;
                        param = param.getJmmChild(1);
                    }
                    break;
                }
            }
        }

        return false;
    }

    private OllirExprResult visitFunctionCall(JmmNode node, Void unused) {
        StringBuilder invoker = new StringBuilder();
        boolean hasVarargs = false;
        var varRef = getOffParenthesis(node.getJmmChild(0));
        String varRefName;
        if (OBJECT.check(varRef)){
            varRefName = "this";
        }
        else {
            varRefName = varRef.get("name");
        }
        StringBuilder computation = new StringBuilder();
        boolean isStaticRef = false;
        for (var import_ : table.getImports()){
            var lst = import_.split("\\.");
            String importName = lst[lst.length - 1];
            if (importName.equals(varRefName)){
                isStaticRef = true;
                break;
            }
        }
        String methodName = '"'+node.get("name")+'"';
        if (isStaticRef){
            invoker.append("invokestatic(");
            invoker.append(varRefName).append(", ");
        }
        else {
            invoker.append("invokevirtual(");
            var varRefType = TypeUtils.getExprType(varRef, table);
            String varRefOllirType = OptUtils.toOllirType(varRefType);
            invoker.append(varRefName).append(varRefOllirType).append(", ");
            hasVarargs = hasVarargs(node);
        }
        invoker.append(methodName);
        String tempVarArgs = "";
        if (hasVarargs) {
            System.out.println(node);
            tempVarArgs = OptUtils.getTemp();
            computation.append(tempVarArgs).append(".array.i32").append(SPACE);
            computation.append(ASSIGN).append(".array.i32").append(SPACE);
            computation.append("new(array, ").append(node.getNumChildren()-table.getParameters(node.get("name")).size()).append(".i32").append(")").append(".array.i32").append(END_STMT);
        }
        for (int i = 1; i < node.getNumChildren(); i++) {
            var child = visit(node.getJmmChild(i));
            if (hasVarargs && i > table.getParameters(node.get("name")).size() - 1){
                computation.append(child.getComputation());
                computation.append(tempVarArgs).append("[").append(i-table.getParameters(node.get("name")).size()).append(".i32").append("]").append(".i32").append(SPACE);
                computation.append(ASSIGN).append(".i32").append(SPACE);
                computation.append(child.getCode()).append(END_STMT);
            }
            else {
                invoker.append(", ").append(child.getCode());
                computation.append(child.getComputation());
            }
        }
        if (hasVarargs){
            invoker.append(", ").append(tempVarArgs).append(".array.i32");
        }
        invoker.append(")");
        StringBuilder code = new StringBuilder();
        if(BINARY_EXPR.check(node.getParent()) || FUNCTION_CALL.check(node.getParent()) || RETURN_STMT.check(node.getParent()) || ASSIGN_STMT.check(node.getParent()) || ARRAY_SUBS.check(node.getParent()) || FIELD_CALL.check(node.getParent()) || LENGTH.check(node.getParent()) || NEGATION.check(node.getParent()) || PARENTHESIS.check(node.getParent()) || ARRAY_DECLARATION.check(node.getParent()) || OBJECT_DECLARATION.check(node.getParent())) {
            JmmNode parent = node.getParent();
            Type type = TypeUtils.getExprType(node, table);
            if (type.getName().isEmpty())
                type = TypeUtils.getExprType(parent, table);
            String ollirType = OptUtils.toOllirType(type);
            var temp = OptUtils.getTemp();
            computation.append(temp).append(ollirType).append(SPACE);
            computation.append(ASSIGN).append(ollirType).append(SPACE);
            computation.append(invoker).append(ollirType).append(END_STMT);
            code.append(temp).append(ollirType);
        }
        else {
            code.append(invoker);
        }

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
