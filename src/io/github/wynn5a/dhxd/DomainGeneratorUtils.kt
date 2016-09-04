package io.github.wynn5a.dhxd

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory

/**
 * Created by wayne5a on 2016/9/2.
 */
object DomainGeneratorUtils {
    private var popup: JBPopup? = null
    fun showGenerateContextSettingDialog(project: Project?) {
        val settingComponent = GeneratorSettingForm(project).panel
        popup = JBPopupFactory.getInstance().createComponentPopupBuilder(settingComponent, null)
                .setTitle("Domain Generator Setting").setShowShadow(true).setCancelOnClickOutside(false)
                .createPopup()
        popup!!.showCenteredInCurrentWindow(project!!)
    }

    fun getPopup() = popup

    fun showNotification(project: Project?, msg: String, type: NotificationType) {
        Notification("DuoHao Support", "Need attention", msg, type).notify(project)
    }

    fun closePopup() {
        this.popup!!.dispose()
    }

}
