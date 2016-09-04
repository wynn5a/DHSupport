package io.github.wynn5a.dhxd

/**
 * Created by wayne5a on 2016/9/3.
 */
data class ColumnProperty(val name: String) {
    var type: String = "String"
    var comment: String? = null
    var length: Int? = null
    var nullable: Boolean = true

    constructor(name: String, type: String, comment: String, length: Int, nullable: Boolean) : this(name) {
        this.type = type
        this.comment = comment
        this.length = length
        this.nullable = nullable
    }
}