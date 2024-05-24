package pt.up.fe.comp2024.optimization;

import org.antlr.v4.runtime.misc.Pair;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;

import java.util.List;

public class ConstantFolding {
    public Pair<JmmSemanticsResult,Boolean> optimize(JmmSemanticsResult semanticsResult) {

        JmmNode root = semanticsResult.getRootNode();
        List<JmmNode> methods = root.getDescendants(Kind.METHOD_DECL);
        boolean isChanged = false;
        for (JmmNode method : methods) {
            isChanged = isChanged || foldConstants(method);
        }
        return new Pair<>(semanticsResult, isChanged);
    }

    private boolean foldConstants(JmmNode node) {
        boolean isChanged = false;
        if (primitiveIntBinaryExpr(node)) {
            JmmNode newNode = node.getJmmChild(0).copyNode();
            JmmNode leftNode = node.getJmmChild(0);
            JmmNode rightNode = node.getJmmChild(1);
            int left = Integer.parseInt(leftNode.get("value"));
            int right = Integer.parseInt(rightNode.get("value"));

            int result = switch (node.get("op")) {
                case "+" -> left + right;
                case "-" -> left - right;
                case "*" -> left * right;
                case "/" -> left / right;
                default -> 0;
            };
            newNode.put("value", Integer.toString(result));
            leftNode.detach();
            rightNode.detach();
            node.replace(newNode);
            isChanged = true;
        }
        for (JmmNode child : node.getChildren()) {
            isChanged = isChanged || foldConstants(child);
        }
        return isChanged;
    }

    private boolean primitiveIntBinaryExpr(JmmNode node) {
        return Kind.BINARY_EXPR.check(node) && Kind.INTEGER_LITERAL.check(node.getJmmChild(0)) && Kind.INTEGER_LITERAL.check(node.getJmmChild(1));
    }
}
