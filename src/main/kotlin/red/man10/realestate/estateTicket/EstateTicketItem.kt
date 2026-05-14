package red.man10.realestate.estateTicket

import org.bukkit.inventory.ItemStack

class EstateTicketItem(val item: ItemStack) {


    companion object{

        fun isEstateTicketItem(item: ItemStack):Boolean{
            TODO()
        }

    }

    fun getDiscountValue(): Double{
        TODO()
    }

    fun getId():Int{
        TODO()
    }

    fun getCityId():String{
        TODO()
    }

}