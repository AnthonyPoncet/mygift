package dao

import RestCategory
import java.lang.IllegalStateException

/** Adapt table used only ad-hoc when needed **/
class AdaptTable(dbPath: String) {
    private val conn = DbConnection(dbPath)

    enum class STEP { ADD_RANK_TO_CATEGORY }

    fun execute(step: STEP) {
        when (step) {
            STEP.ADD_RANK_TO_CATEGORY -> addRankToCategory()
        }
    }

    /**
     * 1. Will add rank column if needed
     * 2. Will reset rank from 1 to X keeping current rank order
     *   --> Allow to init rank to default value
     *   --> If rank is too high because too many categories has been added and deleted, could fix that.
     */
    private fun addRankToCategory() {
        try {
            conn.executeQuery("SELECT rank from categories")
            println("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            println("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE categories ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId from categories")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        println("Will update category of users $userIds")

        val categoryAccessor = CategoryAccessor(conn)
        for (userId in userIds) {
            val categories = categoryAccessor.getUserCategories(userId)
            println("User $userId has categories $categories")
            categories.sortedBy { c -> c.rank }
            println("User $userId has sorted categories $categories")

            var newRank = 1L
            val newCategories = categories.stream().map { c -> c.copy(rank = newRank++) }
            println("User $userId will now have categories as $newCategories")

            for (category in newCategories) {
                categoryAccessor.modifyCategory(category.id, RestCategory(category.name, category.rank))
            }
            println("User $userId done")
        }

        println("Done!")
    }
}