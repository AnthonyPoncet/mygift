import java.time.LocalDate

class DbInitializerForTest(private val databaseManager: DatabaseManager) {

    init {
        //Clean db
        databaseManager.cleanTables()

        //Create User
        val aze = databaseManager.addUser(UserInformation("aze", "aze")).id
        val azeDCat = databaseManager.getUserCategories(aze)[0].id
        println("username: aze, pwd: aze ==> id: $aze - Default category id: $azeDCat")
        val eza = databaseManager.addUser(UserInformation("eza", "eza")).id
        databaseManager.addCategory(eza, RestCategory("Second catégorie"))
        val ezaCats = databaseManager.getUserCategories(eza)
        val ezaDCat = ezaCats[0].id
        val ezaSCat = ezaCats[1].id
        println("username: eza, pwd: eza ==> id: $eza - Default category id: $eza, \"Second catégorie\" id: $ezaSCat")

        //Fill gift
        databaseManager.addGift(aze, RestGift("One", "First description", "10€", "http://mysite.com", azeDCat))
        databaseManager.addGift(aze, RestGift("No desc", null, "20$", "a place", azeDCat))
        databaseManager.addGift(aze, RestGift("No price", "There is no price", null, "http://mysite.com or ici, 75000 Paris", azeDCat))
        databaseManager.addGift(aze, RestGift("No where to buy", "There is no where to buy", "30 - 40£", null, azeDCat))
        databaseManager.addGift(aze, RestGift("Only mandatory", null, null, null, azeDCat))
        println("5 gifts added to aze")

        databaseManager.addGift(eza, RestGift("A first one", null, null, null, ezaDCat))
        databaseManager.addGift(eza, RestGift("A second one", null, null, null, ezaDCat))
        databaseManager.addGift(eza, RestGift("One in another cat", null, null, null, ezaSCat))
        println("3 gift added to eza")

        //They are friend
        databaseManager.createFriendRequest(aze, eza)
        val fr = databaseManager.getReceivedFriendRequests(eza)[0].id
        databaseManager.acceptFriendRequest(eza, fr)
        println("aze and eza are now friend")

        //Events
        databaseManager.createEventAllForAll("Christmas", aze, null, LocalDate.now(), setOf(aze, eza))
        databaseManager.createEventAllForOne("Birthday aze", aze, null, LocalDate.now(), aze, setOf(eza))
        databaseManager.createEventAllForOne("Birthday eza", aze, null, LocalDate.now(), eza, setOf(aze))
        println("On ALL_FOR_ALL event, and two ALL_FOR_ONE events created")
    }
}