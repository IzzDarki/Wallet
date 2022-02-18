package com.izzdarki.wallet.utils

import java.util.*

class ItemProperty {

    companion object {
        const val INVALID_ID = 0
    }

    var propertyID: Int
    var name: String
    var value: String
    var secret = false

    constructor(name: String, value: String, secret: Boolean, propertiesNeededToCreateNewID: MutableList<ItemProperty>) {
        this.propertyID = generateNewPropertyID(propertiesNeededToCreateNewID)
        this.name = name
        this.value = value
        this.secret = secret
    }

    constructor(propertyID: Int, name: String, value: String, secret: Boolean) {
        this.propertyID = propertyID
        this.name = name
        this.value = value
        this.secret = secret
    }

    private fun generateNewPropertyID(properties: MutableList<ItemProperty>): Int {
        var newID: Int
        val random = Random()
        do {
            newID = random.nextInt()
        } while (!isNewIDValid(newID, properties))
        return newID
    }

    private fun isNewIDValid(propertyID: Int, properties: MutableList<ItemProperty>): Boolean {
        return if (propertyID == INVALID_ID)
                false
            else
                !doesPropertyIDExist(propertyID, properties)
    }

    private fun doesPropertyIDExist(propertyID: Int, properties: MutableList<ItemProperty>): Boolean {
        for (property in properties) {
            if (propertyID == property.propertyID)
                return true
        }
        return false
    }
}