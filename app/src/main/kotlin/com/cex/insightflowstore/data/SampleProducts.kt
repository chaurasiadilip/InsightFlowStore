package com.cex.insightflowstore.data

import com.cex.insightflowstore.model.Product

object SampleProducts {
    val items = listOf(
        Product(
            id = "prod_001",
            name = "AeroPro Wireless Noise-Cancelling Headphones",
            category = "Audio",
            price = 249.99,
            rating = 4.8,
            description = "Premium acoustic architecture with hybrid ANC and 36-hour battery life."
        ),
        Product(
            id = "prod_002",
            name = "Zenith Ultra 5G Smartphone 256GB",
            category = "Phones",
            price = 899.00,
            rating = 4.9,
            description = "Flagship device featuring OLED display, 50MP triple camera, and next-gen silicon."
        ),
        Product(
            id = "prod_003",
            name = "Pulse Fit Smartwatch v3",
            category = "Wearables",
            price = 179.50,
            rating = 4.6,
            description = "Continuous ECG, advanced sleep tracking, water-resistant up to 50 meters."
        ),
        Product(
            id = "prod_004",
            name = "ErgoMechanical Bluetooth Keyboard",
            category = "Accessories",
            price = 119.00,
            rating = 4.7,
            description = "Hot-swappable switches, wireless multi-device pairing, and RGB backlighting."
        ),
        Product(
            id = "prod_005",
            name = "UltraVision 4K Gaming Monitor 27-inch",
            category = "Computing",
            price = 449.00,
            rating = 4.8,
            description = "165Hz refresh rate, 1ms response time, IPS HDR400 color accurate panel."
        ),
        Product(
            id = "prod_006",
            name = "HyperFast 65W GaN Travel Charger",
            category = "Accessories",
            price = 39.99,
            rating = 4.5,
            description = "Dual USB-C and USB-A ports, pocket-sized compact form factor."
        ),
        Product(
            id = "prod_007",
            name = "SwiftBook Pro 14 M-Series Laptop",
            category = "Computing",
            price = 1299.00,
            rating = 4.9,
            description = "Ultra-thin aluminum chassis, 18-hour battery life, and 32GB unified memory."
        ),
        Product(
            id = "prod_008",
            name = "SoundPod Mini Smart Speaker",
            category = "Audio",
            price = 49.00,
            rating = 4.3,
            description = "360-degree room filling sound with voice assistant integration."
        ),
        Product(
            id = "prod_009",
            name = "OmniCharge MagSafe Battery Pack 10000mAh",
            category = "Accessories",
            price = 59.90,
            rating = 4.4,
            description = "Magnetic wireless charging on the go with digital percentage display."
        ),
        Product(
            id = "prod_010",
            name = "Lumina Smart LED Desk Lamp with Qi Charger",
            category = "Home Office",
            price = 79.00,
            rating = 4.6,
            description = "Customizable color temperature, ambient light sensor, and wireless charging pad."
        )
    )
}
