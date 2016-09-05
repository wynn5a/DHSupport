package io.github.wynn5a.dhxd

import com.google.googlejavaformat.java.Formatter
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import freemarker.template.Configuration
import org.apache.commons.lang.StringUtils
import java.io.File
import java.io.StringWriter
import java.sql.Connection
import java.sql.ResultSet
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by wayne5a on 2016/9/3.
 */
object GenerateUtils {
    val LOG: Logger = Logger.getInstance(GenerateUtils::class.simpleName!!)
    var project: Project? = null
    fun generateDomain(project: Project?, context: GeneratorContext) {
        this.project = project
        val connection = context.getConnection(project)
        if (connection == null) {
            DomainGeneratorUtils.showNotification(project, "Connection is null, cannot perform generation", NotificationType.ERROR)
            return
        }

        vmToString(context, connection)
    }

    fun vmToString(context: GeneratorContext, connection: Connection) {
        val templateFile = context.templateFile
        val (path, fileName) = getFilePathAndName(templateFile)

        if (StringUtils.isEmpty(path) || StringUtils.isEmpty(fileName)) {
            DomainGeneratorUtils.showNotification(project, "Invalid template file", NotificationType.ERROR)
            return
        }

        val cfg = Configuration(Configuration.getVersion())
        cfg.setDirectoryForTemplateLoading(File(path))
        cfg.defaultEncoding = "UTF-8"
        cfg.templateExceptionHandler = freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER
        cfg.logTemplateExceptions = false
        val vmContext = buildVmContext(context, connection)
        val temp = cfg.getTemplate(fileName)

        val out = StringWriter()
        temp.process(vmContext, out)
        val className = context.tableName.uppercaseUnderscoreToCamel()
        val destPath = "$path${File.separator}$className.java"
        writeToFile(out, destPath)
        DomainGeneratorUtils.showNotification(project, "Generated domain: $className successful, path: $destPath", NotificationType.INFORMATION)
        DomainGeneratorUtils.closePopup()
    }

    private fun writeToFile(out: StringWriter, destPath: String) {
        val result = out.toString()
        print(result)
        val formatted = Formatter().formatSource(result)
        FileUtil.writeToFile(File(destPath), formatted)
    }

    private fun getFilePathAndName(templateFile: String): Pair<String, String> {
        var path = ""
        var fileName = ""
        if (templateFile.contains("/")) {
            path = templateFile.substring(0, templateFile.lastIndexOf("/"))
            fileName = templateFile.substring(templateFile.lastIndexOf("/"), templateFile.length)
        } else if (templateFile.contains("\\")) {
            path = templateFile.substring(0, templateFile.lastIndexOf("\\"))
            fileName = templateFile.substring(templateFile.lastIndexOf("\\"), templateFile.length)
        }

        return Pair(path, fileName)
    }

    private fun buildVmContext(context: GeneratorContext, connection: Connection): Map<String, Any> {
        val vmContext = HashMap<String, Any>()
        vmContext.put(GeneratorConstance.DATE, SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date()))
        val tableName = context.tableName
        vmContext.put(GeneratorConstance.TABLE_NAME, tableName)
        vmContext.put(GeneratorConstance.CLASS_NAME, tableName.uppercaseUnderscoreToCamel())

        val tableComments = getTableComments(tableName, connection)
        vmContext.put(GeneratorConstance.TABLE_COMMENTS, tableComments?:"")

        var pkFields = getPkFields(tableName, connection)
        pkFields = pkFields.map { s->s.toLowerCase() }
        vmContext.put(GeneratorConstance.PK_FIELDS_STRING, pkFields)

        val columnProperties = getColumnProperties(tableName, connection)
        val columns = ArrayList<Column>(columnProperties.size)
        for (columnProperty in columnProperties) {
            if (columnProperty.type.equals("BigDecimal") && !vmContext.containsKey(GeneratorConstance.SHOW_BIG_DECIMAL)) {
                vmContext.put(GeneratorConstance.SHOW_BIG_DECIMAL, true)
            }
            columns.add(toColumn(columnProperty))
        }
        vmContext.put(GeneratorConstance.COLUMNS, columns)
        return vmContext
    }

    private fun toColumn(columnProperty: ColumnProperty): Column {
        val name = columnProperty.name.toLowerCase()
        val comment = columnProperty.comment
        val type = columnProperty.type
        val methodName = name.uppercaseUnderscoreToCamel()
        val paraName = methodName[0].toLowerCase() + methodName.substring(1)
        val parameterType = if ("String".equals(type)) "1" else "5"
        return Column(name, type, comment.toString(), methodName, parameterType, paraName)
    }

    private fun getPkFields(tableName: String, connection: Connection): List<String> {
        val sql = """
            SELECT CU.column_name
            FROM USER_CONS_COLUMNS CU, USER_CONSTRAINTS AU
            WHERE CU.CONSTRAINT_NAME = AU.CONSTRAINT_NAME
            AND AU.CONSTRAINT_TYPE = 'P'
            AND AU.TABLE_NAME = ?
        """

        val result = executeQuery(sql, connection, tableName)
        val pkFields = ArrayList<String>()

        while (result!!.next()) {
            val columnName = result.getString("column_name")
            pkFields.add(columnName)
        }

        return pkFields
    }

    fun getColumnProperties(tableName: String, connection: Connection): List<ColumnProperty> {
        val sql = """
            SELECT T.COLUMN_NAME, T.DATA_TYPE, T.NULLABLE, T.DATA_LENGTH, C.COMMENTS
            FROM USER_TAB_COLUMNS T, USER_COL_COMMENTS C
            WHERE T.TABLE_NAME = C.TABLE_NAME
            AND T.COLUMN_NAME = C.COLUMN_NAME
            AND T.TABLE_NAME = ?
        """
        val result = executeQuery(sql, connection, tableName)

        val columnProperties = arrayListOf<ColumnProperty>()
        while (result!!.next()) {
            val name = result.getString("COLUMN_NAME")
            val type = result.getString("DATA_TYPE")
            val nullable = result.getString("NULLABLE")
            val length = result.getString("DATA_LENGTH")
            val comment = result.getString("COMMENTS")?:""
            val property = ColumnProperty(name, toJavaType(type), comment, toInt(length), toBoolean(nullable))
            columnProperties.add(property)
        }

        return columnProperties
    }

    private fun toBoolean(nullable: String?): Boolean {
        return "Y".equals(nullable) || StringUtils.isEmpty(nullable)
    }

    private fun toInt(length: String?): Int {
        if (StringUtils.isNotEmpty(length)) {
            length!!.toInt()
        }

        return -1
    }

    private fun toJavaType(type: String?): String {
        when (type) {
            "VARCHAR2", "CHAR" -> return "String"
            "NUMBER" -> return "BigDecimal"
            "" -> {
                LOG.warn("unsupported type: $type for generating domain")
            }
        }
        return "String"
    }

    /**
     * use jdbc for simple query action
     */
    private fun getTableComments(tableName: String, connection: Connection): String? {
        val sql = "SELECT COMMENTS FROM USER_TAB_COMMENTS WHERE TABLE_NAME = ? "
        val result = executeQuery(sql, connection, tableName)
        while (result!!.next()) {
            return result.getString("COMMENTS")
        }
        return ""
    }

    private fun executeQuery(sql: String, connection: Connection, tableName: String): ResultSet? {
        val ps = connection.prepareStatement(sql)
        ps.setString(1, tableName)
        return ps.executeQuery()
    }

    fun String.uppercaseUnderscoreToCamel(): String {
        if (StringUtils.isEmpty(this)) {
            return this
        }

        if (this.contains("__")) {
            LOG.error("Table name: $this should not contains '__'")
            return this
        }

        var result = this.toLowerCase().split("_").map { s ->
            if (s.length >= 1) {
                s[0].toUpperCase() + s.substring(1)
            } else s
        }.joinToString("")

        if (this.startsWith("_")) {
            result = "_$result"
        }

        if (this.endsWith("_")) {
            result += "_"
        }

        return result
    }

}