package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ConstantPropagation {
    private final Map<String, JmmNode> constants = new HashMap<>();

    public Pair<JmmSemanticsResult,Boolean> optimize(JmmSemanticsResult semanticsResult) {

        JmmNode root = semanticsResult.getRootNode();
        List<JmmNode> methods = root.getDescendants(Kind.METHOD_DECL);
        boolean isChanged = false;
        for (JmmNode method : methods) {
            isChanged = isChanged || propagateConstants(method);
            var varRefExprs = method.getDescendants(Kind.VAR_REF_EXPR);
            var varDecls = method.getChildren(Kind.VAR_DECL);
            boolean found;
            for (var varDecl : varDecls) {
                found = false;
                for (var varRef : varRefExprs) {
                    if (varRef.get("name").equals(varDecl.get("name"))){
                        found = true;
                        if (constants.get(varRef.get("name")) != null && constants.get(varRef.get("name")).equals(varRef.getParent())){
                            found = false;
                        }
                        else {
                            break;
                        }
                    }
                }
                if (!found) {
                    varDecl.detach();
                    for (var key : constants.keySet()) {
                        if (Objects.equals(key, varDecl.get("name"))) {
                            constants.get(key).detach();
                        }
                    }
                }
            }
            constants.clear();
        }
        return new Pair<>(semanticsResult, isChanged);
    }

    private boolean propagateConstants(JmmNode node) {
        boolean isChanged = false;

         if (isVariableUsage(node)) {
            String variable = getVariableName(node);
            if (constants.containsKey(variable) && !usedInLoop(node)) {
                JmmNode constantIntNode = getConstantValue(constants.get(variable));
                replaceWithConstant(node, constantIntNode);
                isChanged = true;
            }
        }
        else if (isIntegerAssignment(node)) {
            String variable = getAssignedVariable(node);
            if (constants.containsKey(variable)) {
                JmmNode nodeToRemove = constants.get(variable);
                nodeToRemove.detach();
                constants.remove(variable);
                isChanged = true;
            }
            constants.put(variable, node);
        }
        else if (isAssignedVariable(node)) {
             for (JmmNode child : node.getChildren()) {
                 isChanged = isChanged || propagateConstants(child);
             }
             var varRefExprs = node.getJmmChild(1).getDescendants(Kind.VAR_REF_EXPR);
             if (!varRefExprs.isEmpty() && !Kind.VAR_REF_EXPR.check(node.getJmmChild(1))) {
                 String variable = getAssignedVariable(node);
                 constants.remove(variable);
             }
        }
        for (JmmNode child : node.getChildren()) {
            isChanged = isChanged || propagateConstants(child);
        }
        return isChanged;
    }

    private boolean usedInLoop(JmmNode node) {
        if(node.getAncestor(Kind.WHILE_STMT).isPresent()) {
            var loop = node.getAncestor(Kind.WHILE_STMT).get();
            var assignStmts = loop.getDescendants(Kind.ASSIGN_STMT);
            for (var assignStmt : assignStmts) {
                if (assignStmt.getJmmChild(0).get("name").equals(node.get("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAssignedVariable(JmmNode node) {
        return Kind.ASSIGN_STMT.check(node) && constants.containsKey(node.getJmmChild(0).get("name"));
    }

    private boolean isIntegerAssignment(JmmNode node) {
        return Kind.ASSIGN_STMT.check(node) && Kind.INTEGER_LITERAL.check(node.getJmmChild(1));
    }

    private String getAssignedVariable(JmmNode node) {
        return node.getJmmChild(0).get("name");
    }

    private JmmNode getConstantValue(JmmNode node) {
        return node.getJmmChild(1);
    }

    private boolean isVariableUsage(JmmNode node) {
        if (Kind.VAR_REF_EXPR.check(node)) {
            return !Kind.ASSIGN_STMT.check(node.getParent()) || !node.getParent().getJmmChild(0).equals(node);
        }
        return false;
    }

    private String getVariableName(JmmNode node) {
        return node.get("name");
    }

    private void replaceWithConstant(JmmNode node, JmmNode constantIntNode) {
        JmmNode newNode = constantIntNode.copy();
        newNode.put("value", constantIntNode.get("value"));
        node.replace(newNode);
    }
}
