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
            var varDecls = method.getChildren(Kind.VAR_DECL);
            var assignStmts = method.getDescendants(Kind.ASSIGN_STMT);
            boolean found;
            for (var varDecl : varDecls) {
                found = false;
                for (var assignStmt : assignStmts) {
                    if (assignStmt.getJmmChild(0).get("name").equals(varDecl.get("name"))) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    varDecl.detach();
                }
            }
            constants.clear();
        }

        System.out.println(root.toTree());

        return semanticsResult;
    }

    private void propagateConstants(JmmNode node) {
        boolean childrenPropagated = false;

         if (isVariableUsage(node)) {
            String variable = getVariableName(node);
            if (constants.containsKey(variable) && !usedInLoop(node)) {
                JmmNode constantIntNode = getConstantValue(constants.get(variable));
                System.out.println("Replacing " + variable + " with " + constantIntNode.get("value"));
                replaceWithConstant(node, constantIntNode);
            }
        }
        else if (isIntegerAssignment(node)) {
            System.out.println("Found integer assignment");
            String variable = getAssignedVariable(node);
            if (constants.containsKey(variable)) {
                JmmNode nodeToRemove = constants.get(variable);
                nodeToRemove.detach();
                constants.remove(variable);
            }
            constants.put(variable, node);
        }
        else if (isAssignedVariable(node)) {
             for (JmmNode child : node.getChildren()) {
                 propagateConstants(child);
             }
             childrenPropagated = true;
            System.out.println("Found variable assignment, but not to a simple primitive value");
            String variable = getAssignedVariable(node);
            constants.remove(variable);
        }
        if (!childrenPropagated) {
            for (JmmNode child : node.getChildren()) {
                propagateConstants(child);
            }
        }
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

    private boolean isSelfAssignment(JmmNode node) {
        if(Kind.ASSIGN_STMT.check(node)) {
            JmmNode left = node.getJmmChild(0);
            if (!Kind.VAR_REF_EXPR.check(left)) {
                return false;
            }
            JmmNode right = node.getJmmChild(1);
            if(!Kind.VAR_REF_EXPR.check(right)){
                var varRefs = right.getDescendants(Kind.VAR_REF_EXPR);
                for (var varRef : varRefs) {
                    if (varRef.get("name").equals(left.get("name"))) {
                        return true;
                    }
                }
                return false;
            }
            else {
                return right.get("name").equals(left.get("name"));
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
