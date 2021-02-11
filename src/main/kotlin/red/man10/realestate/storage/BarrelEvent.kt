package red.man10.realestate.storage

import org.bukkit.Material
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractEvent
import red.man10.realestate.Utility.sendMessage
import red.man10.realestate.storage.Barrel.addPermission
import red.man10.realestate.storage.Barrel.hasItem
import red.man10.realestate.storage.Barrel.hasPermission
import red.man10.realestate.storage.Barrel.isSpecialBarrel
import red.man10.realestate.storage.Barrel.openStorage
import red.man10.realestate.storage.Barrel.setStorageItem
import red.man10.realestate.storage.Barrel.title

object BarrelEvent:Listener {

    private val blockMap = HashMap<Player,Block>()
    private val isOpen = mutableListOf<Triple<Int,Int,Int>>()

    @EventHandler
    fun setBarrelEvent(e:BlockPlaceEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        addPermission(e.player,barrelState)
    }

    @EventHandler
    fun openBarrelEvent(e:PlayerInteractEvent){

        if (e.action != Action.RIGHT_CLICK_BLOCK)return

        val block = e.clickedBlock?:return

        if (block.type != Material.BARREL)return

        val barrelState = block.state
        if (barrelState !is Barrel)return

        if(!isSpecialBarrel(barrelState))return

        val p = e.player

        if (p.isSneaking){
            if (e.hasItem() &&e.item!!.type == Material.PAPER){

                if (!hasPermission(p,barrelState))return

                addPermission(p,barrelState,e.item!!)

                sendMessage(p,"§e§l権限の設定に成功しました！")
            }

            if (!e.hasItem()){
                e.isCancelled = true
            }

            return
        }

        e.isCancelled = true

        if (!hasPermission(p,barrelState)){
            sendMessage(p,"§c§lあなたはこの樽を開く権限がありません！")
            return
        }

        val loc = block.location

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
            return
        }

        isOpen.add(Triple(loc.blockX,loc.blockY,loc.blockZ))

        openStorage(barrelState,p)

        blockMap[p] = block

    }

    @EventHandler
    fun closeInventory(e:InventoryCloseEvent){

        if (e.view.title != title)return

        val p = e.player

        setStorageItem(e.inventory,blockMap[p]?:return)

        val loc = blockMap[p]!!.location

        isOpen.remove(Triple(loc.blockX,loc.blockY,loc.blockZ))
        blockMap.remove(p)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun breakBarrel(e:BlockBreakEvent){

        val block = e.block

        if (block.type != Material.BARREL)return

        val state = block.state
        if (state !is Barrel)return

        if ((state.customName?:return) != title)return

        val loc = block.location

        val p = e.player

        if (!hasPermission(p,state)){
            sendMessage(p,"§c§lあなたはこの樽を壊す権限がありません")
            e.isCancelled = true
            return
        }

        if (isOpen.contains(Triple(loc.blockX,loc.blockY,loc.blockZ))){
            sendMessage(p,"§c§l現在他のプレイヤーが開いています！")
            e.isCancelled = true
            return
        }

        if(hasItem(state)){
            sendMessage(p,"§c§l中にアイテムが入っています！")
            e.isCancelled = true
            return
        }

        //barrel.dropStorage(state)

    }

}