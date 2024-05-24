package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstantPropagation {
    private final Map<String, JmmNode> constants = new HashMap<>();

    //TODO: Case where i = i + CONSTANT (loop or no loop)

    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode root = semanticsResult.getRootNode();
        List<JmmNode> methods = root.getDescendants(Kind.METHOD_DECL);
        for (JmmNode method : methods) {
            propagateConstants(method);
            for (var value : constants.entrySet()) {
                value.getValue().detach();
            }
            for (var key : constants.keySet()) {
                var varDecls = method.getChildren(Kind.VAR_DECL);
                for (var varDecl : varDecls) {
                    if (varDecl.get("name").equals(key)) {
                        varDecl.detach();
                    }
                }
            }
            constants.clear();
        }

        System.out.println(root.toTree());

        return semanticsResult;
    }

    private void propagateConstants(JmmNode node) {
        if (isVariableUsage(node)) {
            String variable = getVariableName(node);
            if (constants.containsKey(variable)) {
                JmmNode constantIntNode = getConstantValue(constants.get(variable));
                System.out.println("Replacing " + variable + " with " + constantIntNode.get("value"));
                replaceWithConstant(node, constantIntNode);
            }
        }
        if (isIntegerAssignment(node)) {
            System.out.println("Found integer assignment");
            String variable = getAssignedVariable(node);
            if (constants.containsKey(variable)) {
                JmmNode nodeToRemove = constants.get(variable);
                nodeToRemove.detach();
                constants.remove(variable);
            }
            constants.put(variable, node);
        }
        for (JmmNode child : node.getChildren()) {
            propagateConstants(child);
        }
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
