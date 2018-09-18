package it.ding.sonar.check.locator;

import static it.ding.sonar.data.CommonData.CSS_LOCATOR_VALUE_CHECK;
import static it.ding.sonar.util.CommonUtil.getIdentifier;
import static it.ding.sonar.util.CommonUtil.getLocatorValueMapInAnnotation;
import static it.ding.sonar.util.CommonUtil.methodInvocationIsElementFinder;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.AnnotationTree;
import org.sonar.plugins.java.api.tree.BaseTreeVisitor;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;

@Rule(key = CSS_LOCATOR_VALUE_CHECK)
public class CssLocatorValueCheck extends BaseTreeVisitor implements JavaFileScanner {

    private JavaFileScannerContext context;

    private static final List<String> CSS_LOCATORS = asList("cssselector", "css");

    // At least two times a space or ">"
    private static final String AVOID_CSS_LOCATOR_REGEX = "(([^>]*\\s+[^>]*)|(.*\\s*>\\s*.*)){2,}";

    @Override
    public void scanFile(JavaFileScannerContext context) {
        this.context = context;

        scan(context.getTree());
    }

    @Override
    public void visitAnnotation(AnnotationTree tree) {
        Map<String, String> locatorsInAnnotation = getLocatorValueMapInAnnotation(tree);

        for (Map.Entry<String, String> locator : locatorsInAnnotation.entrySet()) {
            String locatorStrategy = locator.getKey();
            String locatorValue = locator.getValue();

            checkLocator(tree, locatorStrategy, locatorValue);
        }
    }

    @Override
    public void visitMethodInvocation(MethodInvocationTree tree) {
        if (methodInvocationIsElementFinder(tree) && !tree.arguments().isEmpty()) {
            String locatorStrategy = getIdentifier(tree).name();
            String locatorValue = ((LiteralTree) tree.arguments().get(0)).value();

            checkLocator(tree, locatorStrategy, locatorValue);
        }
    }

    private void checkLocator(ExpressionTree expressionTree, String locatorStrategy, String locatorValue) {
        String value = locatorValue.replace("\"", "");

        if (CSS_LOCATORS.contains(locatorStrategy.toLowerCase()) && value.matches(AVOID_CSS_LOCATOR_REGEX)) {
            context.reportIssue(this, expressionTree,
                "Avoid using " + locatorStrategy + " locator tied to page layout");
        }
    }

}
