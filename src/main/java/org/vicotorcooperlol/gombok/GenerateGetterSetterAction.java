package org.vicotorcooperlol.gombok;


import com.goide.psi.GoFile;
import com.goide.psi.GoTypeDeclaration;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GenerateGetterSetterAction extends AnAction {
    private static final String GO_STRUCT_NOT_FOUND_ERR_MSG = "no go struct declaration found in current file";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // get all struct declarations
        Map<String, GoTypeDeclaration> typeDeclarations = PsiFileUtils.getAllStructDeclarationInGoFile(e);
        if (MapUtils.isEmpty(typeDeclarations)) {
            this.showMessageErr(e, GO_STRUCT_NOT_FOUND_ERR_MSG);
            return;
        }

        // get struct names and their fields
        Map<String, List<Field>> structName2Fields = typeDeclarations.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> PsiFileUtils.getStructAllFields(entry.getValue())));


        List<String> outputs = structName2Fields.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(field -> field.toOutput(entry.getKey()))
                )
                .collect(Collectors.toList());

        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        Document doc = editor.getDocument();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            // get old getters and setters start with "Get" and "Set" and delete them first
            List<String> structNameList = new ArrayList<>(structName2Fields.keySet());
            PsiFileUtils.getStructAllGetterSetterTextRange(e, structNameList)
                    .forEach(range -> doc.deleteString(range.getStartOffset(), range.getEndOffset()));
            this.formatGoFile(e);

            doc.insertString(doc.getTextLength(), "\n" + StringUtils.join(outputs.toArray()));
            this.formatGoFile(e);
        });
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        boolean isGoFile = this.isGoFile(editor);

        e.getPresentation().setEnabledAndVisible(isGoFile);
    }

    private boolean isGoFile(final Editor editor) {
        Document document = editor.getDocument();

        String extension = Optional.of(FileDocumentManager.getInstance())
                .map(manager -> manager.getFile(document))
                .map(VirtualFile::getExtension)
                .orElse(null);
        return extension != null && extension.equalsIgnoreCase("go");
    }


    private void showMessageErr(@NotNull AnActionEvent e, String msg) {
        Project project = e.getProject();
        Messages.showMessageDialog(project, msg, "Failed", Messages.getInformationIcon());
    }

    private void formatGoFile(AnActionEvent e) {
        GoFile psiFile = (GoFile) e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        if (psiFile == null) {
            return;
        }
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        CodeStyleManager styleManager = CodeStyleManager.getInstance(project);
        styleManager.reformat(psiFile, true);
    }
}
