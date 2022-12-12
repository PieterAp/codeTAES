package com.example.fastuga

class OrderModel {
    var orderId = 0
    var orderTime: String? = null
    var customerName: String? = null
    var pickup_address: String? = null
    var delivery_address: String? = null
    var delivered_by: String? = null
    var status: String? = null
    var ticketNumber = 0
    var profit = 0
    var distance = 0.0

    constructor() {}
    constructor(
        orderId: Int,
        orderTime: String?,
        customerName: String?,
        pickup_address: String?,
        delivery_address: String?,
        delivered_by: String?,
        status: String?,
        ticketNumber: Int,
        profit: Int,
        distance: Double
    ) {
        this.orderId = orderId
        this.orderTime = orderTime
        this.customerName = customerName
        this.pickup_address = pickup_address
        this.delivery_address = delivery_address
        this.delivered_by = delivered_by
        this.status = status
        this.ticketNumber = ticketNumber
        this.profit = profit
        this.distance = distance
    }
}