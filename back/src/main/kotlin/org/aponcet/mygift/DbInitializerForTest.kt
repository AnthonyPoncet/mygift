package org.aponcet.mygift

import org.aponcet.mygift.dbmanager.DatabaseManager
import org.aponcet.mygift.dbmanager.NewCategory
import org.aponcet.mygift.dbmanager.NewGift
import java.time.LocalDate

class DbInitializerForTest(databaseManager: DatabaseManager) {

    init {
        //Clean db
        databaseManager.cleanTables()

        //Create User
        val aze = databaseManager.addUser("aze", "aze", "black_cat.png").id
        val azeDCat = databaseManager.getUserCategories(aze)[0].id
        println("username: aze, pwd: aze ==> id: $aze - Default category id: $azeDCat")
        val eza = databaseManager.addUser("eza", "eza", "red_cat.png").id
        databaseManager.addCategory(eza, NewCategory("Second catégorie"))
        val ezaCats = databaseManager.getUserCategories(eza)
        val ezaDCat = ezaCats[0].id
        val ezaSCat = ezaCats[1].id
        println("username: eza, pwd: eza ==> id: $eza - Default category id: $eza, \"Second catégorie\" id: $ezaSCat")
        val other = databaseManager.addUser("other", "other", null).id
        println("usernamme: other, pwd; other ==> id: $other")

        //Fill gift
        databaseManager.addGift(aze,
            NewGift(
                "One",
                "First description with spécial char and ' ",
                "10€",
                "http://mysite.com",
                azeDCat,
                null
            ), false)
        databaseManager.addGift(aze,
            NewGift("No desc", null, "20$", "a place", azeDCat, null), false)
        databaseManager.addGift(aze,
            NewGift(
                "No price",
                "There is no price",
                null,
                "http://mysite.com or ici, 75000 Paris",
                azeDCat,
                null
            ), false)
        databaseManager.addGift(aze,
            NewGift(
                "No where to buy",
                "There is no where to buy",
                "30 - 40£",
                null,
                azeDCat,
                null
            ), false)
        databaseManager.addGift(aze,
            NewGift("Only mandatory", null, null, null, azeDCat, "pc.png"), true)
        println("5 gifts added to aze")

        databaseManager.addGift(eza,
            NewGift("A first one", null, null, null, ezaDCat, "pc.png"), false)
        databaseManager.addGift(eza,
            NewGift("A second one", null, null, null, ezaDCat, null), false)
        databaseManager.addGift(eza,
            NewGift(
                "One in another cat",
                null,
                null,
                null,
                ezaSCat,
                "book.png"
            ), false)
        println("3 gift added to eza")

        //They are friend
        databaseManager.createFriendRequest(aze, eza)
        var fr = databaseManager.getReceivedFriendRequests(eza)[0].id
        databaseManager.acceptFriendRequest(eza, fr)
        println("aze and eza are now friend")
        databaseManager.createFriendRequest(other, aze)
        fr = databaseManager.getReceivedFriendRequests(aze)[0].id
        databaseManager.acceptFriendRequest(aze, fr)
        println("other and aze are now friend")

        //Events
        databaseManager.createEventAllForAll("Christmas", aze, null, LocalDate.now(), setOf(aze, eza))
        databaseManager.createEventAllForOne("Birthday aze", aze, null, LocalDate.now(), aze, setOf(eza))
        databaseManager.createEventAllForOne("Birthday eza", aze, null, LocalDate.now(), eza, setOf(aze))
        println("On ALL_FOR_ALL event, and two ALL_FOR_ONE events created")
    }
}