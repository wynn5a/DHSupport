package io.github.wynn5a.dhxd

/**
 * Created by wayne5a on 2016/9/4.
 */
class Column(var name: String, var type: String) {
    var comment: String = ""
    var methodName: String = ""
    var parameterType: String = "1"
    var paraName: String = ""

    constructor(name: String, type: String, comment: String, methodName: String, parameterType: String, paraName: String) : this(name, type) {
        this.comment = comment
        this.methodName = methodName
        this.paraName = paraName
        this.parameterType = parameterType
    }
}