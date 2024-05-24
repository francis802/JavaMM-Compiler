package pt.up.fe.comp2024.optimization_jasmin;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast2jasmin.AstToJasmin;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp2024.ast.Kind;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AstToJasminImpl implements AstToJasmin {
    @Override
    public JasminResult toJasmin(JmmSemanticsResult semanticsResult) {

        var generator = new JasminGeneratorVisitor(semanticsResult.getSymbolTable());
        var code = generator.visit(semanticsResult.getRootNode());

        return new JasminResult(semanticsResult, code, Collections.emptyList());
    }

    private final Map<String, JmmNode> constants = new HashMap<>();

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        JmmNode root = semanticsResult.getRootNode();
        //TODO: Just do this for each method
        propagateConstants(root);
        for (var value : constants.entrySet()) {
            //TODO: Remove Var Decl as well
            value.getValue().detach();
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

    private boolean isBeingAssignedVariable(JmmNode node) {
        return Kind.ASSIGN_STMT.check(node);
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
