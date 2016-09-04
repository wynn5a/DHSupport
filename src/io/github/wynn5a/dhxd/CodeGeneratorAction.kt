package io.github.wynn5a.dhxd

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project

/**
 * Created by wayne5a on 2016/9/2.
 */
class CodeGeneratorAction : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.getData(PlatformDataKeys.PROJECT)
        showConfigDialog(project)
    }

    /**
     * get setting and generate
     */
    private fun showConfigDialog(project: Project?) {
        DomainGeneratorUtils.showGenerateContextSettingDialog(project)
    }

}
