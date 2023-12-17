package org.vicotorcooperlol.gombok;

import com.goide.psi.GoAnonymousFieldDefinition;
import com.goide.psi.GoFieldDeclaration;
import com.goide.psi.GoFieldDefinition;
import com.goide.psi.GoFile;
import com.goide.psi.GoMethodDeclaration;
import com.goide.psi.GoReceiver;
import com.goide.psi.GoSignature;
import com.goide.psi.GoSpecType;
import com.goide.psi.GoStructType;
import com.goide.psi.GoType;
import com.goide.psi.GoTypeDeclaration;
import com.goide.psi.GoTypeSpec;
import com.google.common.collect.Lists;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PSI file structure:
 * <p>
 * File layer:
 * {@link GoFile}
 * {@link com.goide.psi.GoTypeDeclaration},{@link com.goide.psi.GoMethodDeclaration},{@link com.goide.psi.GoFunctionDeclaration},{@link com.goide.psi.GoPackageClause},{@link com.goide.psi.GoImportList}
 * <p>
 * Type Declaration layer
 * {@link com.goide.psi.GoTypeDeclaration}
 * {@link com.goide.psi.GoTypeSpec}
 * {@link com.goide.psi.GoSpecType}
 * {@link com.goide.psi.GoStructType}
 * {@link com.goide.psi.GoFieldDeclaration}
 * {@link com.goide.psi.GoFieldDefinition}
 */

public class PsiFileUtils {

    /**
     * structure:
     * {@link GoFile}
     * {@link com.goide.psi.GoTypeDeclaration}
     * {@link com.goide.psi.GoTypeSpec}
     * {@link com.goide.psi.GoSpecType contextlessUnderlyingType	STRUCT_TYPE/INTERFACE_TYPE}
     *
     * @param e AnActionEvent
     * @return list of go struct declarations
     */
    public static Map<String, GoTypeDeclaration> getAllStructDeclarationInGoFile(AnActionEvent e) {
        GoFile psiFile = (GoFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        return Optional.ofNullable(psiFile)
                .map(goFile -> PsiTreeUtil.getChildrenOfType(psiFile, GoTypeDeclaration.class))
                .stream()
                .flatMap(Stream::of)
                .filter(PsiFileUtils::isStructTypeDeclaration)
                .collect(Collectors.toMap(PsiFileUtils::getStructName, Function.identity()));
    }


    public static String getStructName(GoTypeDeclaration declaration) {
        return Optional.ofNullable(declaration)
                .map(d -> PsiTreeUtil.getChildOfType(d, GoTypeSpec.class))
                .map(GoTypeSpec::getName)
                .orElse("");
    }

    public static boolean isStructTypeDeclaration(GoTypeDeclaration declaration) {
        return Optional.ofNullable(declaration)
                .map(d -> PsiTreeUtil.getChildOfType(d, GoTypeSpec.class))
                .map(goTypeSpec -> PsiTreeUtil.getChildOfType(goTypeSpec, GoSpecType.class))
                .map(goSpecTypes -> PsiTreeUtil.getChildrenOfType(goSpecTypes, GoStructType.class))
                .map(Lists::newArrayList)
                .map(l -> !l.isEmpty())
                .orElse(false);
    }

    /**
     * structure:
     * {@link GoFile}
     * {@link com.goide.psi.GoTypeDeclaration}
     * {@link com.goide.psi.GoTypeSpec}
     * {@link com.goide.psi.GoSpecType}
     * {@link com.goide.psi.GoStructType}
     * {@link com.goide.psi.GoFieldDeclaration multiple}
     * {@link com.goide.psi.GoFieldDefinition,com.goide.psi.GoType, definiation.name, type.text}，{@link com.goide.psi.GoAnonymousFieldDefinition}
     * <p>
     * for {@link com.goide.psi.GoAnonymousFieldDefinition}
     * if it is an interface
     * then the structure looks like this:
     * {@link com.goide.psi.GoAnonymousFieldDefinition elementType ANONYMOUS_FIELD_DEFINITION}
     * {@link com.goide.psi.GoType contextlessUnderlyingType INTERFACE_TYPE}
     * {@link com.goide.psi.GoTypeReferenceExpression}
     * {@link PsiElement identifier}
     * <p>
     * if it is a struct
     * then the structure looks like this:
     * {@link com.goide.psi.GoAnonymousFieldDefinition elementType ANONYMOUS_FIELD_DEFINITION, name Base, text base.Base}
     * {@link com.goide.psi.GoType contextlessUnderlyingType STRUCT_TYPE}
     * {@link com.goide.psi.GoTypeReferenceExpression}
     * {@link com.goide.psi.GoTypeReferenceExpression}
     * {@link PsiElement identifier}
     */
    public static List<Field> getStructAllFields(GoTypeDeclaration declaration) {
        List<GoFieldDeclaration> fieldDeclarationList = Optional.ofNullable(declaration)
                .map(d -> PsiTreeUtil.getChildOfType(d, GoTypeSpec.class))
                .map(goTypeSpec -> PsiTreeUtil.getChildOfType(goTypeSpec, GoSpecType.class))
                .map(goSpecTypes -> PsiTreeUtil.getChildOfType(goSpecTypes, GoStructType.class))
                .map(goStructType -> PsiTreeUtil.getChildrenOfType(goStructType, GoFieldDeclaration.class))
                .map(Lists::newArrayList)
                .orElse(Lists.newArrayList());

        return fieldDeclarationList.stream()
                .filter(PsiFileUtils::isNotAnonymous)
                .map(PsiFileUtils::getFieldFromNonAnonymousFieldDeclaration)
                .collect(Collectors.toList());
    }

    public static List<TextRange> getStructAllGetterSetterTextRange(AnActionEvent e, List<String> structNameList) {
        GoFile psiFile = (GoFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        List<String> pointerNames = structNameList.stream().map(name -> Constants.Pointer + name).collect(Collectors.toList());
        return Optional.ofNullable(psiFile)
                .map(goFile -> PsiTreeUtil.getChildrenOfType(psiFile, GoMethodDeclaration.class))
                .stream()
                .flatMap(Stream::of)
                .filter(goMethodDeclaration -> StringUtils.startsWith(getMethodName(goMethodDeclaration), "Get") || StringUtils.startsWith(getMethodName(goMethodDeclaration), "Set"))
                .map(goMethodDeclaration -> PsiTreeUtil.getChildOfType(goMethodDeclaration, GoReceiver.class))
                .filter(Objects::nonNull)
                .filter(goReceiver -> goReceiver.getType() != null && pointerNames.contains(goReceiver.getType().getText()))
                .map(goReceiver -> PsiTreeUtil.getParentOfType(goReceiver, GoMethodDeclaration.class))
                .filter(Objects::nonNull)
                .map(PsiElement::getTextRange)
                .sorted((t1, t2) -> t2.getStartOffset() - t1.getStartOffset())
                .collect(Collectors.toList());
    }

    private static String getMethodName(GoMethodDeclaration goMethodDeclaration) {
        return Optional.ofNullable(goMethodDeclaration)
                .map(declaration -> PsiTreeUtil.getChildOfType(declaration, GoSignature.class))
                .map(PsiElement::getPrevSibling)
                .filter(Objects::nonNull)
                .map(PsiElement::getText)
                .orElse("");
    }


    public static boolean isNotAnonymous(GoFieldDeclaration fieldDeclaration) {
        return Optional.ofNullable(fieldDeclaration)
                .map(d -> PsiTreeUtil.getChildrenOfType(d, GoAnonymousFieldDefinition.class))
                .map(Lists::newArrayList)
                .map(List::isEmpty)
                .orElse(true);
    }

    public static Field getFieldFromNonAnonymousFieldDeclaration(GoFieldDeclaration declaration) {
        String name = Optional.ofNullable(declaration)
                .map(d -> PsiTreeUtil.getChildOfType(d, GoFieldDefinition.class))
                .map(GoFieldDefinition::getName)
                .orElse("");
        String type = Optional.ofNullable(declaration)
                .map(d -> PsiTreeUtil.getChildOfType(d, GoType.class))
                .map(GoType::getText)
                .orElse("");
        return new Field(name, type);
    }

    public static GoTypeDeclaration getSelectedGoTypeDeclaration(AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);

        GoFile psiFile = (GoFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        int offset = editor.getCaretModel().getOffset();

        return Optional.ofNullable(psiFile)
                .map(f -> f.findElementAt(offset))
                .map(psiElement -> PsiTreeUtil.getContextOfType(psiElement, GoTypeDeclaration.class))
                .orElse(null);
    }
}
