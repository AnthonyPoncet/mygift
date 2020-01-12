package dao

import RestCategory
import RestGift

/** Adapt table used only ad-hoc when needed **/
class AdaptTable(dbPath: String) {
    private val conn = DbConnection(dbPath)

    enum class STEP { ADD_RANK_TO_CATEGORY, ADD_RANK_TO_GIFT }

    fun execute(step: STEP) {
        when (step) {
            STEP.ADD_RANK_TO_CATEGORY -> addRankToCategory()
            STEP.ADD_RANK_TO_GIFT -> addRankToGift()
        }
    }

    /**
     * 1. Will add rank column if needed
     * 2. Will reset rank from 1 to X keeping current rank order
     *   --> Allow to init rank to default value
     *   --> If rank is too high because too many categories have been added and deleted, could fix that.
     */
    private fun addRankToCategory() {
        try {
            conn.executeQuery("SELECT rank FROM categories")
            println("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            println("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE categories ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM categories")
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

    /**
     * 1. Will add rank column if needed
     * 2. Will reset rank from 1 to X keeping current rank order
     *   --> Allow to init rank to default value
     *   --> If rank is too high because too many gifts have been added and deleted, could fix that.
     */
    private fun addRankToGift() {
        try {
            conn.executeQuery("SELECT rank FROM gifts")
            println("Column rank exists, will adapt rank values")
        } catch (e: Exception) {
            println("Column rank does not exist, add it")
            conn.executeUpdate("ALTER TABLE gifts ADD COLUMN 'rank' INTEGER NOT NULL DEFAULT 1")
        }

        val userIds = HashSet<Long>()
        val rs = conn.executeQuery("SELECT userId FROM gifts")
        while (rs.next()) {
            userIds.add(rs.getLong("userId"))
        }

        println("Will update gifts of users $userIds")

        val giftAccessor = GiftAccessor(conn)
        for (userId in userIds) {
            val allGifts = giftAccessor.getUserGifts(userId)
            val categoryIds = allGifts.map { g -> g.categoryId }.toSet()
            println("User $userId has gift on categories $categoryIds")
            for (category in categoryIds) {
                val gifts = allGifts.filter { g -> g.categoryId == category }
                println("\tCategory $category has gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")
                gifts.sortedBy { g -> g.rank }
                println("\tCategory $category has sorted gifts ${gifts.map { g -> "(${g.id}, ${g.rank})" }}")

                var newRank = 1L
                val newGifts = gifts.map { g -> g.copy(rank = newRank++) }
                println("\tCategory $category will now have gifts ${newGifts.map { g -> "(${g.id}, ${g.rank})" }}")

                for (gift in newGifts) {
                    giftAccessor.modifyGift(gift.id, RestGift(gift.name, gift.description, gift.price,
                        gift.whereToBuy, gift.categoryId, gift.picture, gift.rank))
                }
                println("\tCategory $category done\n")
            }
        }

        println("Done!")
    }
}