package red.man10.realestate.estateTicket

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.util.MySQLManager

object EstateTicketCalculator {


    data class EstateTicket(val id:Int,val value: Double,val cityId:String)

    fun getAvailableEstateTickets(player: Player,region: Region): Array<EstateTicketItem>{
        val array=mutableListOf<EstateTicketItem>()
        player.inventory.contents.slice(0..8).filterNotNull().filter {
            item->
            EstateTicketItem.isEstateTicketItem(item) && (EstateTicketItem(item).getCityId() == (region.data.city
                ?: false))
        }.forEach {
            array.add(EstateTicketItem(it))
        }
        return array.toTypedArray()
    }

    fun sumEstateTicketValue(tickets:Array<EstateTicket>): Double{

        var sum=0.0

        tickets.forEach { ticket->
            sum+= ticket.value
        }

        return sum

    }

    fun getValidTickets(tickets:Array<EstateTicketItem>):Array<EstateTicket>{
        val mysql= MySQLManager(Plugin.plugin,"Man10RealEstate Checking Tickets")

        val inClause = tickets.map { it.getId() }.joinToString(prefix = "(", postfix = ")", separator = ",")

        val rs=mysql.query("SELECT id,is_valid,value,city_id FROM ESTATE_TICKET WHERE ID IN ${inClause};")?:run{
            mysql.close()
            return arrayOf()
        }

        val validList=mutableListOf<EstateTicket>()

        while (rs.next()){
            if(rs.getBoolean("is_valid")){
                validList.add(EstateTicket(rs.getInt("id"), rs.getDouble("value"),rs.getString("city_id")))
            }
        }
        rs.close()
        mysql.close()

        return validList.toTypedArray()

    }

}