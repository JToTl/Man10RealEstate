package red.man10.realestate.region

import com.google.gson.Gson
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import red.man10.man10score.ScoreDatabase
import red.man10.realestate.Command
import red.man10.realestate.Plugin
import red.man10.realestate.util.Logger
import red.man10.realestate.util.MySQLManager
import red.man10.realestate.util.Utility
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class Region {

    companion object{

        val regionData = ConcurrentHashMap<Int, Region>()
        val gson = Gson()

        fun formatStatus(status: Status):String{
            return when(status){
                Status.PROTECTED -> "保護"
                Status.ON_SALE -> "販売中"
                Status.LOCK -> "ロック(使用不可)"
                Status.FREE -> "フリー"
                else -> status.value
            }
        }

        fun formatSpan(span:Int):String{
            return when(span){
                0 -> "1ヶ月"
                1 -> "1週間"
                2 -> "毎日"
                else -> "不明"
            }
        }

        fun asyncLoad(){

            Logger.logger("土地の読み込み開始")

            Plugin.async.execute {
                regionData.clear()

                val sql = MySQLManager(Plugin.plugin,"Man10RealEstate Loading")

                val rs = sql.query("SELECT * FROM region;")?:return@execute

                while (rs.next()){

                    val id = rs.getInt("id")

                    val rg = Region()

                    rg.id = id
                    rg.name = rs.getString("name")
                    rg.world = rs.getString("world")
                    rg.server = rs.getString("server")

                    val uuid = rs.getString("owner_uuid")

                    if (uuid!=null && uuid!="null"){
                        rg.ownerUUID = UUID.fromString(uuid)
                        rg.ownerName = Bukkit.getOfflinePlayer(UUID.fromString(uuid)).name
                    }
                    try {
                        rg.status = Status.valueOf(rs.getString("status"))
                    }catch (e:Exception){
                        Bukkit.getLogger().info("$id ${rs.getString("status")}")
                    }
                    try {
                        rg.taxStatus = TaxStatus.valueOf(rs.getString("tax_status"))
                    }catch (e:Exception){
                        Bukkit.getLogger().info("$id ${rs.getString("tax_status")}")
                    }

                    rg.price = rs.getDouble("price")

                    rg.span = rs.getInt("span")

                    rg.startPosition = Triple(
                        rs.getInt("sx"),
                        rs.getInt("sy"),
                        rs.getInt("sz")
                    )
                    rg.endPosition = Triple(
                        rs.getInt("ex"),
                        rs.getInt("ey"),
                        rs.getInt("ez")
                    )

                    rg.teleport = Location(
                        Bukkit.getWorld(rg.world),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                    )

                    rg.data = gson.fromJson(rs.getString("data"),RegionData::class.java)

                    regionData[id] = rg

                    if (Bukkit.getWorld(rg.world) == null){
                        Logger.logger("存在しないワールドの土地",id)
//                        rg.asyncDelete()
//                        Bukkit.getLogger().warning("id:${id}は存在しないワールドだったので、削除しました!")
                    }else {
                        regionData[id] = rg
                    }
                }
                rs.close()
                sql.close()
            }
        }


        //ログイン時にスコアを確認
        fun asyncLoginProcess(p:Player){
            Plugin.async.execute {
                val data = regionData.filterValues { it.ownerUUID == p.uniqueId }
                val score = ScoreDatabase.getScore(p.uniqueId)
                data.forEach {
                    val city = City.where(it.value.teleport)!!
                    if (city.ownerScore>score){
                        it.value.status = Status.LOCK
                    }else{
                        if(it.value.status==Status.LOCK){
                        it.value.status = Status.PROTECTED
                        }
                    }
                }
            }
        }

        fun create(pos1:Triple<Int,Int,Int>,pos2:Triple<Int,Int,Int>,name:String,price:Double,tp:Location,p:Player):Int{

            val data = RegionData(false,0.0,0.0)

            val query = "INSERT INTO region " +
                    "(server, world, name, status, price, " +
                    "x, y, z, pitch, yaw, " +
                    "sx, sy, sz, ex, ey, ez, data) " +
                    "VALUES(" +
                    "'${Plugin.serverName}', " +
                    "'${tp.world.name}', " +
                    "'${MySQLManager.escapeStringForMySQL(name)}', " +
                    "'OnSale', " +
                    "$price, " +
                    "${tp.x}, " +
                    "${tp.y}, " +
                    "${tp.z}, " +
                    "${tp.pitch}, " +
                    "${tp.yaw}, " +
                    "${pos1.first}, " +
                    "${pos1.second}, " +
                    "${pos1.third}, " +
                    "${pos2.first}, " +
                    "${pos2.second}, " +
                    "${pos2.third}, " +
                    "'${gson.toJson(data)}'); "

            val mysql = MySQLManager(Plugin.plugin,"Man10RealEstate CreateRegion")

            mysql.execute(query)

            val rs = mysql.query("SELECT * FROM region ORDER BY id DESC LIMIT 1;")?:return -1

            if (!rs.next())return -1

            val id = rs.getInt("id")

            rs.close()
            mysql.close()

            val rg = Region()

            rg.name = name
            rg.id = id
            rg.startPosition = pos1
            rg.endPosition = pos2
            rg.teleport = tp

            rg.world = tp.world.name
            rg.server = Plugin.serverName

            rg.price = price

            rg.data = data

            regionData[id] = rg

            Logger.logger(p,"土地を作成",id)

            return id

        }
    }

    var id = 0
    var name = "RegionName"
    var ownerUUID : UUID? = null
    var ownerName : String? = if (ownerUUID == null) "サーバー" else Bukkit.getOfflinePlayer(ownerUUID!!).name
    var status : Status = Status.ON_SALE //Lock,Danger,Free,OnSale,Protected
    var taxStatus : TaxStatus = TaxStatus.SUCCESS //SUCCESS,WARN,FREE

    var world = "builder"
    var server = "server"

    var startPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    var endPosition: Triple<Int,Int,Int> = Triple(0,0,0)
    lateinit var teleport : Location

    var price : Double = 0.0
    var span = 0 //0:month 1:week 2:day

    val userList=HashMap<UUID,User>()

    lateinit var data : RegionData


    fun asyncSave(){

        MySQLManager.mysqlQueue.add("UPDATE region SET " +
                "owner_uuid = '${ownerUUID}', " +
                "owner_name = '${if (ownerUUID == null)null
                else{Bukkit.getOfflinePlayer(ownerUUID!!).name}}', " +
                "x = ${teleport.x}," +
                "y = ${teleport.y}, " +
                "z = ${teleport.z}, " +
                "pitch = ${teleport.pitch}, " +
                "yaw = ${teleport.yaw}, " +
                "sx = ${startPosition.first}, " +
                "sy = ${startPosition.second}, " +
                "sz = ${startPosition.third}, " +
                "ex = ${endPosition.first}, " +
                "ey = ${endPosition.second}, " +
                "ez = ${endPosition.third}, " +
                "status = '${status}', " +
                "tax_status = '${taxStatus}'," +
                "price = ${price}, " +
                "profit = 0, " +
                "span = ${span}," +
                "data = '${gson.toJson(data)}' " +
                "WHERE id = $id")

    }

    @Synchronized
    fun buy(p: Player){

        val city = City.where(teleport)
        val score = ScoreDatabase.getScore(p.uniqueId)

        if (city == null){
            Utility.sendMessage(p,"§c§l都市の外に土地があります。運営に報告してください")
            return
        }

        if (taxStatus == TaxStatus.WARN){
            Utility.sendMessage(p,"§c§lこの土地は税金滞納のため購入ができません")
            return
        }

        if (status != Status.ON_SALE){
            Utility.sendMessage(p, "§c§lこの土地は販売されていません！")
            return
        }

        if (p.uniqueId == ownerUUID){
            Utility.sendMessage(p, "§c§lあなたはこの土地のオーナーです！")
            return
        }

        if (city.ownerScore > score){
            Utility.sendMessage(p, "§c§lあなたにはこの土地を買うためのスコアが足りません！")
            return
        }

        if(!canOwn(p))return

        if (!Plugin.vault.withdraw(p.uniqueId,price)){
            Utility.sendMessage(p, "§c§l電子マネーが足りません！")
            return
        }

        if (ownerUUID != null){
            Plugin.bank.deposit(ownerUUID!!,price,"Man10RealEstate RegionProfit","土地の売上")
        }

        setOwner(p)
        status = Status.PROTECTED
        asyncSave()

        Logger.logger(p,"土地を購入",id)

        Utility.sendMessage(p, "§a§l土地の購入成功！")
    }

    fun init(status: Status = Status.ON_SALE, default: Double? = null){
        val city = City.where(teleport)?:return
        ownerUUID = null
        ownerName = null
        price = default?:city.defaultPrice
        this.status = status
        this.taxStatus = TaxStatus.SUCCESS
        this.data = RegionData(false,0.0,0.0, city.name)
        User.asyncDeleteAllRegionUser(id)
        asyncSave()
    }

    fun asyncDelete(){
        MySQLManager.mysqlQueue.add("DELETE FROM `region` WHERE  `id`=$id;")
        User.asyncDeleteAllRegionUser(id)
        Logger.logger("土地を削除",id)
    }

    //賃料の支払い
    fun payRent(){
        User.userMap.filterKeys { pair -> pair.second == id }.values.forEach { it.payRent() }
        Logger.logger("賃料の支払い",id)
    }

    //住人の取得
    fun getUsers(): List<User> {
        return User.fromRegion(id)
    }


    fun canOwn(player:Player):Boolean{
        if(Plugin.ownableCityNum!=-1){
            val cities=Utility.playerLivedCities(player)
            println(cities)
            if(cities.size>=Plugin.ownableCityNum&&!cities.contains(data.city)){

                Utility.sendMessage(player,"§c§lこれ以上他の都市に住むことはできません")

                return false
            }
        }
        return true
    }

    fun addUser(player:Player){

        if(getUsers().size < (City.cityData[data.city]?.maxUser ?: -1)){
            User(player.uniqueId,id)
                    .asyncSave()

            Utility.sendMessage(player, "§a§lあなたは住人になりました！")

            ownerUUID?.let { uuid ->
                Bukkit.getPlayer(uuid)?.let { owner->
                    Utility.sendMessage(owner, "§a§l${player.name}が住人になりました！")
                }
            }
        }
        else{
            Utility.sendMessage(player, "§c§l土地の居住可能人数が上限に達しています")
        }

    }

    fun setStatus(player:Player,newStatus: Status){

        if (player.hasPermission(Command.OP)){
            Utility.sendMessage(player,"§a§l${id}の土地の状態を${newStatus}に変更しました")
            this.status=newStatus
            return
        }


        if (status ==Status.LOCK){
            Utility.sendMessage(player,"§c§lロック状態の土地のステータス変更はできません")
            return
        }

        if (ownerUUID == player.uniqueId) {
            Utility.sendMessage(player,"§e§l土地のステータスを${Status.display(newStatus)}に変更しました")
            this.status=newStatus
            return
        }

        if(newStatus==Status.LOCK){//OP以外がlockにできないように
            return
        }

        val user=User.get(player,id)?:run {
            Utility.sendMessage(player,"§c§l権限がありません")
            return
        }

        if(user.allowAll && user.status == "Share"){
            Utility.sendMessage(player,"§e§l土地のステータスを${Status.display(newStatus)}に変更しました")
            this.status=newStatus
            return
        }


        Utility.sendMessage(player,"§c§l権限がありません")

    }

    fun setOwner(player:Player){
        ownerUUID=player.uniqueId
        ownerName=player.name
    }

    fun removeOwner():Boolean{
        ownerUUID=null
        ownerName=null
        return true
    }

    fun reloadBelongingCity(){
        data.city=City.where(teleport)?.name
    }

    fun showRegionData(p:Player){
        Utility.sendMessage(p, "§a==========${name}§a§lの情報==========")
        Utility.sendMessage(p, "§aID:$id")
        Utility.sendMessage(p, "§aステータス:${formatStatus(status)}")
        Utility.sendMessage(p, "§aオーナー:${ownerName}")
        Utility.sendMessage(p, "§a値段:${Utility.format(price)}")
        Utility.sendMessage(p, "§a税額:${Utility.format(City.getTax(id))}")
        if (taxStatus == Region.TaxStatus.WARN){
            Utility.sendMessage(p, "§c§l税金が未払いです")
        }
        Utility.sendMessage(p, "§a==========================================")

        Utility.sendClickMessage(
            p,
            "§d§lブックマークする！＝＞[ブックマーク！]",
            "mre bookmark $id",
            "ブックマークをすると、/mreメニューから テレポートをすることができます"
        )

        if (status == Status.ON_SALE){
            Utility.sendClickMessage(
                p,
                "§a§l§n[土地を買う！]",
                "mre buyconfirm $id",
                "§e§l値段:${Utility.format(price)}"
            )
        }
    }

    data class RegionData(
        var denyTeleport : Boolean,
        var defaultPrice : Double,
        var tax : Double,
        var city:String?=null
    )

    enum class TaxStatus(val value : String){
        SUCCESS("SUCCESS"),
        WARN("WARN"),
        FREE("FREE")
    }

    enum class Status(val value : String){
        ON_SALE("OnSale"),
        PROTECTED("Protected"),
        LOCK("Lock"),
        FREE("Free"),
        DANGER("Danger");

        companion object{
            fun display(status: Status):String{
                return when(status){
                    ON_SALE->"販売中"
                    PROTECTED->"保護"
                    LOCK->"ロック"
                    FREE->"フリー"
                    DANGER->"無法地帯"
                }
            }
        }
    }
}