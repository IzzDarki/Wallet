package com.izzdarki.wallet.data

import java.util.*

data class CredentialField(
    var propertyID: Int = INVALID_ID, // Id is not needed anymore, but needed for migration
    var name: String,
    var value: String,
    var secret: Boolean = false,
){

    constructor(name: String, value: String, secret: Boolean, propertiesNeededToCreateNewID: MutableList<CredentialField>) : this(
        propertyID = generateNewPropertyID(propertiesNeededToCreateNewID),
        name,
        value,
        secret
    )

    companion object {
        const val INVALID_ID = 0

        private fun generateNewPropertyID(properties: MutableList<CredentialField>): Int {
            var newID: Int
            val random = Random()
            do {
                newID = random.nextInt()
            } while (!isNewIDValid(newID, properties))
            return newID
        }

        private fun isNewIDValid(propertyID: Int, properties: MutableList<CredentialField>): Boolean {
            return if (propertyID == INVALID_ID)
                false
            else
                !doesPropertyIDExist(propertyID, properties)
        }

        private fun doesPropertyIDExist(propertyID: Int, properties: MutableList<CredentialField>): Boolean {
            for (property in properties) {
                if (propertyID == property.propertyID)
                    return true
            }
            return false
        }

    }

}