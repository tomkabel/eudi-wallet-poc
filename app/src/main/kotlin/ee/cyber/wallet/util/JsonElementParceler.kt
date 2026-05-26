package ee.cyber.wallet.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Custom Parceler for JsonElement to enable parceling of DocumentField.
 *
 * Serializes JsonElement to JSON string for parceling and deserializes it back when reading.
 */
object JsonElementParceler : Parceler<JsonElement?> {
    override fun create(parcel: Parcel): JsonElement? {
        val jsonString = parcel.readString()
        return if (jsonString != null) {
            Json.parseToJsonElement(jsonString)
        } else {
            null
        }
    }

    override fun JsonElement?.write(parcel: Parcel, flags: Int) {
        if (this != null) {
            parcel.writeString(this.toString())
        } else {
            parcel.writeString(null)
        }
    }
}
