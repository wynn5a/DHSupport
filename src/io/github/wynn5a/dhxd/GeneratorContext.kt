package io.github.wynn5a.dhxd

import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/**
 * Created by wayne5a on 2016/9/3.
 */

data class GeneratorContext(val host: String = "127.0.0.1",
                            val port: String = "1521",
                            val service: String,
                            val userName: String,
                            val password: String,
                            var tableName: String,
                            var templateFile: String) {

    init {
        this.tableName = this.tableName.toUpperCase()
    }

    private val LOG: Logger = Logger.getInstance(GeneratorContext::class.simpleName!!)
    fun getConnection(project: Project?): Connection? {
        val connectStr = "jdbc:oracle:thin:@$host:$port:$service"
        Class.forName("oracle.jdbc.driver.OracleDriver");
        var connection: Connection? = null
        try {
            connection = DriverManager.getConnection(connectStr, userName, password)
        } catch (e: SQLException) {
            LOG.error("cannot get connection user context:$connectStr", e)
            DomainGeneratorUtils.showNotification(project, "Can't get connection for $connectStr", NotificationType.ERROR)
        }
        return connection
    }
}