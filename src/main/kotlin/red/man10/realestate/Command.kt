package red.man10.realestate

import net.kyori.adventure.text.Component
import org.apache.commons.lang.math.NumberUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin.Companion.WAND_NAME
import red.man10.realestate.Plugin.Companion.disableWorld
import red.man10.realestate.Plugin.Companion.es
import red.man10.realestate.Plugin.Companion.plugin
import red.man10.realestate.Utility.format
import red.man10.realestate.Utility.sendClickMessage
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.menu.InventoryMenu
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.region.User
import java.util.*

object Command:CommandExecutor {

    private const val USER = "mre.user"
    private const val GUEST = "mre.guest"
    const val OP = "mre.op"

    private val numbers = mutableListOf<Int>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (sender !is Player)return false

        if (label == "mre"){

            if (args.isNullOrEmpty()){

                if (!hasPermission(sender,GUEST))return false

                InventoryMenu.mainMenu(sender)
                return true
            }

            when(args[0]){

                "buy" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 2)return false

                    val id = args[1].toIntOrNull()?:return false

                    es.execute {
                        Region.buy(sender,id)
                    }
                    return true
                }

                "buycheck" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 2)return false

                    val id = args[1].toIntOrNull()?:return false

                    val data = Region.get(id)?:return false

                    if (data.status != "OnSale"){
                        sendMessage(sender,"§4§lこの土地は販売されていません！")
                        return false
                    }

                    sendMessage(sender,"§c§l値段：${format(data.price)}")
                    sendMessage(sender,"§c§lID：${id}")
                    sendMessage(sender,"§a§l現在のオーナー：${Region.getOwner(data)}")
                    sendMessage(sender,"§e§l本当に購入しますか？(購入しない場合は無視してください)")

                    sendClickMessage(sender,"§a§l[購入する](§6§l電子マネー${format(data.price)}円)","mre buy $id")

                    return true
                }

                "good" ->{

                    if (args.size !=2)return false

                    if (!hasPermission(sender,GUEST))return false

                    User.setLike(sender,args[1].toIntOrNull()?:return true)

                    return true
                }

                "adduser" ->{

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3){
                        sendMessage(sender,"§c§l/mre adduser <ID> <ユーザー名>")
                        return false
                    }
                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id)){ return false }

                    val data = Region.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return false
                    }


                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§lユーザーがオフラインの可能性があります！")
                        return false
                    }

                    if (p.uniqueId == data.ownerUUID){
                        return false
                    }

                    if (User.get(p,id) != null){
                        sendMessage(sender,"§c§lこのユーザーは既に住人です！")
                        return false
                    }


                    es.execute {
                        if (!City.liveScore(id,p)){
                            sendMessage(sender,"ユーザーのスコアが足りません！")
                            return@execute
                        }

                        val number = Random().nextInt()

                        numbers.add(number)


                        sendMessage(p,"§a§l=================土地の情報==================")
                        sendMessage(p,"§a§lオーナー：${sender.name}")
                        sendMessage(p,"§a§l土地のID：$id")
                        sendMessage(p,"§a§l===========================================")

                        sendClickMessage(p,"§e§l住人になる場合は§nここを§e§lクリック！","mre acceptuser $id ${sender.name} $number")

                        sendMessage(sender,"§a§l現在承諾待ちです....")
                        return@execute

                    }

                    return true

                }

                "acceptuser"->{

                    if (!hasPermission(sender,GUEST))return false

                    if (args.size != 4)return false

                    val number = args[3].toIntOrNull()?:return false

                    if (!numbers.contains(number))return false

                    numbers.remove(number)

                    User.create(sender,args[1].toInt())

                    sendMessage(sender,"§a§lあなたは住人になりました！")

                    sendMessage(Bukkit.getPlayer(args[2])!!,"§a§l${sender.name}が住人の入居に承諾しました！")

                    return true

                }

                "removeuser" ->{

                    if (args.size != 3)return false

                    if (!hasPermission(sender,USER))return false

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオフラインなので退去させられません！")
                        return false
                    }

                    User.remove(p,id)

                    sendMessage(sender,"§a§l退去できました！")
                    return true

                }

                "span" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size!=3)return false

                    val id = args[1].toIntOrNull()?:return false
                    val span = args[2].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    Region.setSpan(id,span)

                }

                "setowner" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return false

                    if ((sender.uniqueId != Region.get(id)!!.ownerUUID) && !sender.hasPermission(OP))return false


                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§3§lオンラインのユーザーを入力してください")
                        return true
                    }

                    Region.setOwner(id,p)

                    sendMessage(sender,"§e§l${args[1]}のオーナーを${args[2]}に変更しました")

                    return true

                }

                "settp" ->{
                    if (!sender.hasPermission(USER))return true

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val loc = sender.location

                    val data = Region.get(id)?:return false

                    if (!Utility.isWithinRange(loc,data.startPosition,data.endPosition,data.world,data.server)){
                        sendMessage(sender,"§c土地の外にテレポートポイントを登録することはできません")
                        return true
                    }

                    Region.setTeleport(args[1].toInt(), loc)

                    sendMessage(sender,"§e§l登録完了！")
                    return true
                }

                "setrent" ->{// mre setrent id p amount

                    if (!hasPermission(sender,USER))return false

                    if (args.size != 4)return false

                    val id = args[1].toIntOrNull()?:return false
                    val rent = args[3].toDoubleOrNull()

                    if (!hasRegionPermission(sender,id))return false

                    if (rent == null || rent< 0.0){
                        sendMessage(sender,"金額の設定に問題があります！")
                        return true
                    }

                    val p = Bukkit.getPlayer(args[2])

                    if (p == null){
                        sendMessage(sender,"§c§l住人がオンラインのときのみ、賃料を変更できます")
                        return false
                    }

                    User.setRentPrice(p,id,rent)

                    sendMessage(sender,"§a§l設定完了！")
                    //TODO:賃料スパンを変えれるようにするか考える
                    sendMessage(p,"§a§lID:${id}の賃料が変更されました！賃料:$rent")

                    return true

                }

                "setstatus" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return false
                    val status = args[2]

                    if (!hasRegionPermission(sender,id))return false

                    if (!hasPermission(sender,OP) && status=="Lock"){ return true }

                    Region.setStatus(id,status)

                    sendMessage(sender,"§a§l${id}のステータスを${status}に変更しました")

                    return true

                }

                "setprice" ->{
                    if (!hasPermission(sender,USER))return false

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toIntOrNull()?:return false

                    if (!hasRegionPermission(sender,id))return false

                    val price = args[2].toDoubleOrNull()

                    if (price==null || price <0.0){
                        sendMessage(sender,"金額の設定に問題があります！")
                        return false
                    }

                    Region.setPrice(id,price)

                    sendMessage(sender,"§a§l${id}の金額を${args[2]}に変更しました")

                }

                "tp" ->{
                    if (!hasPermission(sender, USER))return false

                    if (args.size < 2)return false

                    val id = args[1].toIntOrNull()?:return true

                    val data = Region.get(id)?:return true

                    sender.teleport(data.teleport)

                    return true

                }

                "balance" ->{

                    //TODO:支払う税金を確認できるようにする
//                    if (!hasPermission(sender, USER))return false
//
//                    val name = if (args.size >= 2 && hasPermission(sender,OP)){ args[1] }else {sender.uniqueId.toString()}
//
//                    es.execute {
//                        val db = MySQLManager(plugin,"realestate")
//
//                        val rs = db.query("select id from region where owner_uuid='${name}' or owner_name='${name}';")?:return@execute
//
//                        val total = 0
//                        var totalArea = 0
//                        var totalTax = 0.0
//
//                        while (rs.next()){
//
//                            val id = rs.getInt("id")
//
//                            val rg = Region.get(id)!!
//
//                            val width = rg.startPosition.first.coerceAtLeast(rg.endPosition.first) - rg.startPosition.first.coerceAtMost(rg.endPosition.first)
//                            val height = rg.startPosition.third.coerceAtLeast(rg.endPosition.third) - rg.startPosition.third.coerceAtMost(rg.endPosition.third)
//
//                            totalArea += (width*height).toInt()
//                            totalTax += City.getTax(City.whereRegion(id),id)
//                            total.inc()
//
//                        }
//
//                        sendMessage(sender,"§e§l所有してる土地の数:${total}")
//                        sendMessage(sender,"§e§l所持してる土地の総面積:${totalArea}ブロック")
//                        sendMessage(sender,"§e§l翌月に支払う税額:${String.format("%,.1f",totalTax)}")
//                    }

                }

                else ->{
                    sendMessage(sender,"§c§l不明なコマンドです！")
                    return false

                }
            }


        }

        if (label == "mreop"){

            if (!hasPermission(sender,OP))return false

            if (args.isEmpty()){

                sendMessage(sender,"""
                    §e§l/mreop wand : 範囲指定用のワンドを取得
                    §e§l/mreop create <rg/city> <リージョン名/都市名> <値段/税額> : 新規リージョンを作成します
                    §e§l範囲指定済みの${WAND_NAME}§e§lを持ってコマンドを実行してください
                    §e§l/mreop delete <rg/city> <id> : 指定idのリージョンを削除します
                    §e§l/mreop reload : 再読み込みをします
                    §e§l/mreop where : 現在地点がどのリージョンが確認します
                    §e§l/mreop reset <rg/city> <id> : 指定idのリージョンを再指定します
                    §e§l/mreop disableWorld <add/remove> <world> : 指定ワールドの保護を外します
                    §e§l/mreop tax <id> <tax>: 指定都市の税額を変更します
                    §e§l/mreop buyscore <id> <score>: 指定都市の買うのに必要なスコアを変更します
                    §e§l/mreop livescore <id> <score>: 指定都市の住むのに必要なスコアを変更します
                    §e§l/mreop tp <id> : リソース無しでテレポートする
                    §e§l/mreop init <id> <price> : 指定リージョンを初期化する
                    §e§l/mreop starttax : 手動で税金を徴収する
                    §e§l/mreop search : 指定ユーザーの持っている土地を確認する"
                    §e§l/mreop maxuser <id>: 都市の住める上限を設定する
                    §e§l/mreop calctax <id> : 指定都市で徴収できる税額を計算する
                """.trimIndent())

                return true
            }

            when(args[0]){

                //mreop create city <name> <tax>
                //mreop create rg <name> <tax>
                "create" ->{

                    if (args.size != 4)return false

                    val wand = sender.inventory.itemInMainHand

                    if (!wand.hasItemMeta() || wand.itemMeta.displayName != WAND_NAME){
                        sendMessage(sender,"${WAND_NAME}§e§lを持ってください！")
                        return true
                    }

                    val lore = wand.lore

                    if (lore == null || wand.lore!!.size != 5){
                        sendMessage(sender,"§e§fの範囲指定ができていません！")
                        return true
                    }

                    if (!NumberUtils.isNumber(args[3])){
                        sendMessage(sender,"§3§lパラメータの入力方法が違います！")
                        sendMessage(sender,"§3§l/mreop create city [都市の名前] [税額]")
                        sendMessage(sender,"§3§l/mreop create rg [リージョンの名前] [初期の値段]")
                        return true
                    }

                    val amount = args[3].toDouble()

                    sendMessage(sender,"§a§l現在登録中です・・・")

                    es.execute {
                        val c1 = lore[3].replace("§aStart:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                        val startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                        val c2 = lore[4].replace("§aEnd:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                        val endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())

                        var id = -1

                        if (args[1] == "city"){

                            id = City.create(startPosition,endPosition,args[2],amount,sender.location)

                        }else if (args[1] == "rg"){
                            id = Region.create(startPosition,endPosition,args[2],amount,sender.location)
                        }

                        if (id == -1){
                            sendMessage(sender,"§c§l登録失敗！")
                            return@execute
                        }

                        sendMessage(sender,"§a§l登録完了！")

                        if (args[1] == "rg"){
                            sendMessage(sender,"§a§l”mre:$id”と記入した看板を置いてください！")
                        }


                    }
                }

                "delete" ->{

                    if (args.size != 3)return false

                    if (!NumberUtils.isNumber(args[2])){
                        sendMessage(sender,"§c§l数字を入力してください")
                        return true
                    }

                    val id = args[2].toInt()

                    val isRg = args[1] == "rg"

                    if (isRg){
                        if (Region.get(id) == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true

                        }

                        Region.delete(id)
                        sendMessage(sender,"§a§l削除完了！")

                        return true

                    }

                    if (City.get(id) == null){
                        sendMessage(sender,"§c§l存在しない都市です！")
                        return true

                    }
                    City.delete(id)
                    sendMessage(sender,"§a§l削除完了！")

                }

                "wand" ->{
                    val wand = ItemStack(Material.STICK)
                    val meta = wand.itemMeta
                    meta.displayName(Component.text(WAND_NAME))
                    wand.itemMeta = meta
                    sender.inventory.addItem(wand)
                    return true

                }

                "reload" ->{

                    es.execute {
                        Region.load()
                        City.load()

                        for (p in Bukkit.getOnlinePlayers()){
                            User.load(p)
                        }

                        plugin.loadConfig()

                        sendMessage(sender,"§e§lリロード完了")

                    }

                }

                "disableWorld" ->{

                    if (args.size != 3)return true

                    if (args[2].isBlank()){
                        sendMessage(sender,"§3§l保護を外すワールドを指定してください")
                        return true
                    }

                    if (args[1] == "add"){
                        disableWorld.add(args[2])

                        es.execute {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"追加完了！")
                        }
                    }
                    if (args[1] == "remove"){
                        disableWorld.remove(args[2])

                        es.execute {
                            plugin.config.set("disableWorld", disableWorld)
                            plugin.saveConfig()
                            sendMessage(sender,"削除完了！")

                        }
                    }
                }

                "where" ->{

                    val loc = sender.location

                    es.execute {
                        sendMessage(sender, "§e§l=====================================")

                        for (rg in Region.map()) {

                            val data = rg.value

                            if(Utility.isWithinRange(loc, data.startPosition, data.endPosition, data.world,rg.value.server)) {
                                sendMessage(sender, "§e§lRegionID:${rg.key}")
                                sendMessage(sender, "§7Name:${rg.value.name}")
                                sendMessage(sender, "§8Price:${rg.value.price}")
                                sendMessage(sender, "§7Owner:${Region.getOwner(rg.value)}")
                                sendMessage(sender,"§8Tax:${City.getTax(City.whereRegion(rg.key),rg.key)}")

                            }
                        }

                        for (c in City.map()){

                            val data = c.value

                            if(Utility.isWithinRange(loc, data.startPosition, data.endPosition, data.world,data.server)) {
                                sendMessage(sender, "§e§lCityID:${c.key}")
                                sendMessage(sender, "§7Name:${c.value.name}")
                                sendMessage(sender, "§8Tax:${c.value.tax}")
                                sendMessage(sender, "§7MaxUser:${c.value.maxUser}")
                            }

                        }

                        sendMessage(sender, "§e§l=====================================")

                    }
                }

                //都市の範囲の再設定
                "reset" ->{//mreop reset city id

                    if (args.size != 3)return false

                    val wand = sender.inventory.itemInMainHand

                    if (!wand.hasItemMeta() || wand.itemMeta.displayName != WAND_NAME){
                        sendMessage(sender,"${WAND_NAME}§e§lを持ってください！")
                        return true
                    }

                    val lore = wand.lore

                    if (lore == null || wand.lore!!.size != 5){
                        sendMessage(sender,"§e§fの範囲指定ができていません！")
                        return true
                    }

                    if (!NumberUtils.isNumber(args[2]))return true

                    val id = args[2].toInt()

                    val isRg = args[1] == "rg"

                    val c1 = lore[3].replace("§aStart:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                    val startPosition = Triple(c1[0].toDouble(),c1[1].toDouble(),c1[2].toDouble())

                    val c2 = lore[4].replace("§aEnd:§fX:","")
                            .replace("Y","").replace("Z","")
                            .replace(":","").split(",")

                    val endPosition = Triple(c2[0].toDouble(),c2[1].toDouble(),c2[2].toDouble())


                    if (isRg){
                        val data = Region.get(id)

                        if (data == null){
                            sendMessage(sender,"§c§l存在しない土地です！")
                            return true
                        }

                        data.startPosition = startPosition
                        data.endPosition = endPosition

                        Region.set(id,data)

                        sendMessage(sender,"§a§l再設定完了！")
                        return true
                    }

                    val data = City.get(id)

                    if (data == null){
                        sendMessage(sender,"§c§l存在しない土地です！")
                        return true
                    }

                    data.startPosition = startPosition
                    data.endPosition = endPosition

                    City.set(id, data)

                    sendMessage(sender,"§a§l再設定完了！")
                }

                "tax" ->{

                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toInt()
                    val tax= args[2].toDouble()

                    City.setTax(id,tax)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true
                }

                "tp" ->{

                    val id = args[1].toInt()

                    val data = Region.get(id)?:return true

                    sender.teleport(data.teleport)

                    return true

                }

                "init" ->{

                    val id = args[1].toInt()

                    val price =  args[2].toDouble()

                    Region.initRegion(id,price)

                    sendMessage(sender,"§a§l初期化完了")

                    return true
                }

                "starttax" ->{

                    es.execute {
                        sender.sendMessage("税金の徴収開始")
                        User.tax()
                        sender.sendMessage("税金の徴収完了")

                    }

                }

                "search" ->{

                    val uuid = Bukkit.getPlayer(args[1])?.uniqueId

                    if (uuid==null){

                        es.execute {
                            val mysql = MySQLManager(plugin,"mre")

                            val rs = mysql.query("select id from region where owner_name = '${args[1]}';")?:return@execute

                            while (rs.next()){
                                val id = rs.getInt("id")
                                sendClickMessage(sender,"§e§lID:$id","mreop tp $id")
                            }

                            rs.close()
                            mysql.close()

                        }

                        return true
                    }
                    for (rg in Region.map().filter { it.value.ownerUUID == uuid }.keys){
                        sendClickMessage(sender,"§e§lID:${rg}","mreop tp $rg")
                    }

                }

                "maxuser" ->{

                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toInt()
                    val amount= args[2].toInt()

                    City.setMaxUser(id,amount)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true


                }

                "calctax" ->{//mreop calctax <id>

                    if(args.size != 2)return false

                    val cityID = args[1].toInt()

                    var tax = 0.0

                    es.execute {
                        for (rg in Region.map()){

                            if (City.whereRegion(rg.key) !=cityID)continue

                            if (rg.value.ownerUUID == null)continue

                            tax += City.getTax(cityID,rg.key)

                        }

                        sendMessage(sender,"ID:$cityID の回収可能税額は、$tax です。")

                    }

                    return true

                }

                "remit" ->{//mreop remit <id>

                    val id = args[1].toIntOrNull()?:return false

                    val rg = Region.get(id)?:return false
                    rg.isRemitTax = !rg.isRemitTax

                    if (rg.isRemitTax){
                        sendMessage(sender,"§a§l$id の税金を免除するようにしました")
                    }else{
                        sendMessage(sender,"§a§l$id の税金を免除を解除しました")
                    }

                    Region.set(id,rg)

                    return true
                }

                "buyscore" ->{
                    if (args.size != 3)return false
                    if (!NumberUtils.isNumber(args[1]) || !NumberUtils.isNumber(args[2]))return false

                    val id = args[1].toInt()
                    val score= args[2].toInt()

                    City.setBuyScore(id,score)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true

                }

                "livescore" ->{
                    if (args.size != 3)return false

                    val id = args[1].toIntOrNull()?:return true
                    val score= args[2].toIntOrNull()?:return true

                    City.setLiveScore(id,score)

                    sendMessage(sender,"§a§l設定完了！")

                    return  true

                }

                "unpaid" ->{//mreop unpaid cityId <amount>

                    val id = args[1].toIntOrNull()?:return true
                    val amount = args[2].toDoubleOrNull()?:return true

                    val data = City.get(id)?:return true
                    data.defaultPrice = amount

                    City.set(id,data)

                    sendMessage(sender,"§a§l設定完了！")

                }

                else ->{

                    sendMessage(sender,"§c§l不明なコマンドです！")

                    return false

                }

            }

            return false
        }



        return true
    }

    private fun hasPermission(p:Player, permission:String):Boolean{

        if (p.hasPermission(permission))return true

        sendMessage(p,"§c§lあなたはこのコマンドを使うことができません！")
        return false

    }


    /**
     * 指定リージョンの編集権限を持っているかどうか
     */
    private fun hasRegionPermission(p:Player,id:Int):Boolean{

        if (p.hasPermission(OP))return true

        val data = Region.get(id)?:return false

        if (data.status == "Lock")return false

        if (data.ownerUUID == p.uniqueId)return true

        val userData = User.get(p,id)?:return false

        if (userData.allowAll && userData.status == "Share")return true

        return false

    }
}