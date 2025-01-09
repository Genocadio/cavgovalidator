package com.nexxserve.cavgodrivers

import java.math.BigInteger

fun validateSecureId(secureId: String, tripId: String): Boolean {
    // Split secureId into result and tripIdSum
    val parts = secureId.split("-")
    if (parts.size != 2) throw IllegalArgumentException("Invalid secureId format")

    val result = parts[0].toBigIntegerOrNull() ?: throw IllegalArgumentException("Invalid result in secureId")
    val tripIdSum = parts[1].toBigIntegerOrNull() ?: throw IllegalArgumentException("Invalid tripIdSum in secureId")

    // Calculate numeric equivalent of tripId and sum of its digits
    val numericTripId = tripId.map { it.digitToIntOrNull(16) ?: 0 }
    if (numericTripId.isEmpty()) {
        throw IllegalArgumentException("Invalid tripId: $tripId")
    }

    val calculatedTripIdSum = numericTripId.sum().toBigInteger()

    // Validate tripIdSum
    if (calculatedTripIdSum != tripIdSum) {
        println("Trip ID sum mismatch: Expected $tripIdSum, Calculated $calculatedTripIdSum")
        return false
    }

    // Reconstruct the Luhn number
    val reconstructedNumber = (result * tripIdSum).toString()
    println("Reconstructed Luhn Number: $reconstructedNumber")

    // Extract the base number (all but the last digit)
    val baseNumber = reconstructedNumber.dropLast(1)

    // Validate Luhn checksum
    val checksum = calculateLuhnChecksum(baseNumber)
    val expectedLuhnNumber = baseNumber + checksum.toString()

    val isValid = expectedLuhnNumber == reconstructedNumber
    if (!isValid) {
        println("Luhn validation failed: Expected $expectedLuhnNumber, Got $reconstructedNumber")
    }

    return isValid
}


/**
 * Calculate Luhn checksum for a number string.
 * @param number The base number to calculate the checksum for.
 * @return The Luhn checksum digit.
 */
fun calculateLuhnChecksum(number: String): Int {
    var sum = 0
    var isSecond = false

    // Process digits from right to left
    for (i in number.length - 1 downTo 0) {
        var digit = number[i].toString().toInt()

        if (isSecond) {
            digit *= 2
            if (digit > 9) digit -= 9
        }

        sum += digit
        isSecond = !isSecond
    }

    // Return the checksum digit
    return (10 - (sum % 10)) % 10
}
