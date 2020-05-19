package red.man10.realestate.region

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.SignChangeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Constants.Companion.disableWorld
import red.man10.realestate.Constants.Companion.regionData
import red.man10.realestate.Constants.Companion.regionUserData
import red.man10.realestate.Constants.Companion.sendMessage
import red.man10.realestate.Plugin
import java.lang.Exception

class ProtectRegionEvent:Listener{

    val allowList = mutableListOf(
            Material.CHEST,
            Material.ENDER_CHEST,
            Material.HOPPER,
            Material.TRAPPED_CHEST,
            Material.DISPENSER,
            Material.DROPPER,
            Material.FURNACE,
            Material.BARREL,
            Material.SHULKER_BOX)

    @EventHandler
    fun blockBreakEvent(e:BlockBreakEvent){

        val p = e.player

        if (!canBreak(p,e.block.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを破壊する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun blockPlaceEvent(e:BlockPlaceEvent){
        val p = e.player

        if (!canBreak(p,e.block.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを設置する権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun blockTouchEvent(e:PlayerInteractEvent){
        if (e.action != Action.RIGHT_CLICK_BLOCK)return
        if (!e.hasBlock())return

        val p = e.player

        if (e.hasBlock()&&e.clickedBlock!!.state is org.bukkit.block.Sign){
            e.isCancelled = true
            return
        }

        if (!canBreak(p,e.clickedBlock!!.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません！")
            e.isCancelled = true
        }
    }

    @EventHandler
    fun signEvent(e:SignChangeEvent){
        val p = e.player

        if (!canBreak(p,p.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    @EventHandler
    fun breakEntity(e:HangingBreakByEntityEvent){
        val p = e.remover?:return

        if (p !is Player)return

        if (!canBreak(p,p.location,e)){
            sendMessage(p,"§4§lあなたにはこの場所でブロックを触る権限がありません")
            e.isCancelled = true
        }

    }

    fun canBreak(p:Player,loc: org.bukkit.Location,eventType:Any):Boolean{

        //adminなどはプロテクト無視
        if (p.hasPermission("mre.op"))return true

        for (id in regionData){

            val data = id.value

            if (isWithinRange(loc,data.startCoordinate,data.endCoordinate)){
                if (data.status == "Lock")return false
                if (data.owner_uuid == p.uniqueId)return true

                val pd = (regionUserData[p]?:return false)[id.key]?:return false

                if (pd.status == "Lock")return false
                if (pd.allowAll)return true

                if ((eventType is BlockBreakEvent || eventType is BlockPlaceEvent) && pd.allowBlock)return true
                if ((eventType is SignChangeEvent || eventType is HangingBreakByEntityEvent) && pd.allowBlock)return true

                if (eventType is PlayerInteractEvent && pd.allowInv){
                    if (eventType.hasBlock() && allowList.contains(eventType.clickedBlock!!.type))return true

                }

                if (eventType is PlayerInteractEvent && pd.allowDoor && !allowList.contains(eventType.clickedBlock!!.type))return true

                return false
            }
        }

        if (disableWorld.contains(loc.world.name)){
            return true
        }
        return false
    }

    ////////////////////////////////////////////////////////////
    //立体の対角線の頂点から、指定座標が立体の中にあるかどうか判定するメソッド
    ////////////////////////////////////////////////////////////
    fun isWithinRange(loc: org.bukkit.Location, start:Triple<Double,Double,Double>, end:Triple<Double,Double,Double>):Boolean{

        val x = loc.blockX
        val y = loc.blockY
        val z = loc.blockZ

        if (x < start.first.coerceAtMost(end.first) || x > start.first.coerceAtLeast(end.first))return false
        if (y < start.second.coerceAtMost(end.second) || y > start.second.coerceAtLeast(end.second))return false
        if (z < start.third.coerceAtMost(end.third) || z > start.third.coerceAtLeast(end.third))return false

        return true

    }

}