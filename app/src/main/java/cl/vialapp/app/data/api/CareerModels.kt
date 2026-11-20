package cl.vialapp.app.data.api

// GUÍA DEL ARCHIVO:
// Modelos del garaje y vehículos, con disponibilidad definida por el servidor.

import com.google.gson.annotations.SerializedName

data class CareerState(
    val coins: Int,
    @SerializedName("xp_coins") val xpCoins: Int,
    @SerializedName("bonus_coins") val bonusCoins: Int,
    val stages: List<CareerStage>,
    val zone: String,
    @SerializedName("equipped_vehicle") val equippedVehicle: String,
    @SerializedName("equipped_paint") val equippedPaint: String,
    val vehicles: List<GarageVehicle>,
    val paints: List<GaragePaint>,
    val accessories: List<GarageAccessory>? = null,
    @SerializedName("equipped_accessories") val equippedAccessories: Map<String,String>? = null
)
data class CareerStage(val id: String, val title: String, val zone: String, val index: Int,
    @SerializedName("reward_coins") val rewardCoins: Int, val requirements: List<CareerGoal>,
    val claimed: Boolean, val available: Boolean, val claimable: Boolean)
data class CareerGoal(val metric: String, val label: String, val goal: Int, val progress: Int)
data class GarageVehicle(val id: String, val name: String, val price: Int,
    @SerializedName("required_stages") val requiredStages: Int,
    val style: String, val owned: Boolean, val available: Boolean)
data class GaragePaint(val id: String, val name: String, val hex: String, val price: Int, val owned: Boolean)
data class CareerAction(val action: String, @SerializedName("item_id") val itemId: String)

data class GarageAccessory(val id:String, val slot:String, val name:String, val price:Int,
    @SerializedName("required_stages") val requiredStages:Int, val owned:Boolean, val available:Boolean)
