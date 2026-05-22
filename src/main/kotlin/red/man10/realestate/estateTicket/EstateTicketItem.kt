package red.man10.realestate.estateTicket

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import red.man10.realestate.Plugin
import red.man10.realestate.estateTicket.EstateTicketCalculator.EstateTicket
import red.man10.realestate.util.MySQLManager
import red.man10.realestate.util.Utility

class EstateTicketItem(val item: ItemStack) {


    companion object{
        private val ESTATE_TICKET_ID_KEY = NamespacedKey(Plugin.plugin, "estateTicketId")
        private val ESTATE_TICKET_CITY_KEY = NamespacedKey(Plugin.plugin, "estateTicketCity")

        fun isEstateTicketItem(item: ItemStack):Boolean{
            if (item.isEmpty)return false

            return item.persistentDataContainer.has(
                ESTATE_TICKET_ID_KEY,
                PersistentDataType.INTEGER
            )
        }

        fun createEstateTicketItem(cityId: String, value: Double):ItemStack{
            val mysql= MySQLManager(Plugin.plugin,"Man10RealEstate Creating Tickets")

            val query="INSERT into ESTATE_TICKET (is_valid,value,city_id) VALUES (true,${value},${cityId})"

            val item=Plugin.estateTicketItem.clone()

            val id=mysql.insert(query,"id") as Int

            val meta=item.itemMeta
            meta.persistentDataContainer.set(ESTATE_TICKET_ID_KEY,PersistentDataType.INTEGER,id)
            meta.persistentDataContainer.set(ESTATE_TICKET_CITY_KEY,PersistentDataType.STRING,cityId)

            meta.displayName(Component.text("§a土地チケット"))
            meta.lore(
                listOf(
                    Component.text("§e土地を購入する際にお金の代わりとして使うことができます"),
                    Component.text("§e使い方：/mre buyWithTicket <土地id>"),
                    Component.text("§c注意：購入時はホットバーにある全ての土地チケットを使用します"),
                    Component.text(""),
                    Component.text("対象都市：${cityId}"),
                    Component.text("金額：${Utility.format(value)}")
                )
            )

            item.itemMeta = meta

            return item
        }

    }

    fun getId():Int{
        return item.persistentDataContainer.getOrDefault(
            ESTATE_TICKET_ID_KEY,
            PersistentDataType.INTEGER,
            -1
        )
    }

    fun getCityId():String?{
        return item.persistentDataContainer.get(
            ESTATE_TICKET_CITY_KEY,
            PersistentDataType.STRING
        )
    }

}
