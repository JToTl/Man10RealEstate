package red.man10.realestate.estateTicket

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import red.man10.realestate.Plugin
import red.man10.realestate.region.City
import red.man10.realestate.region.Region
import red.man10.realestate.util.MySQLManager

object EstateTicketCalculator {

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

    fun sumEstateTicketValue(tickets:Array<EstateTicketItem>): Double{

        var sum=0.0

        tickets.forEach { item->
            sum+= item.getDiscountValue()
        }

        return sum

    }

    fun getValidTickets(tickets:Array<EstateTicketItem>):Array<EstateTicketItem>{
        val mysql= MySQLManager(Plugin.plugin,"Man10RealEstate Checking Tickets")

        val inClause = tickets.map { it.getId() }.joinToString(prefix = "(", postfix = ")", separator = ",")

        val rs=mysql.query("SELECT id,isValid FROM ESTATE_TICKET WHERE ID IN ${inClause};")?:run{
            mysql.close()
            return arrayOf()
        }

        val validIdList=mutableListOf<Int>()

        while (rs.next()){
            if(rs.getBoolean("is_valid")){
                validIdList.add(rs.getInt("id"))
            }
        }
        rs.close()
        mysql.close()

        return tickets.filter { validIdList.contains(it.getId()) }.toTypedArray()

    }

}