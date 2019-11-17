package dao

abstract class DaoAccessor {
    abstract fun getTableName() : String
    abstract fun createIfNotExists()

    protected fun errorMessage(query: String, vararg parameters: String): String {
        return "Execution of '$query' with parameter ${parameters.joinToString(prefix = "[", postfix = "]")} throw an exception"
    }
}