package com.dabber.traveldabble.ui.mock

import com.dabber.traveldabble.model.*
import com.dabber.traveldabble.ui.theme.CoverAlpine
import com.dabber.traveldabble.ui.theme.CoverForest
import com.dabber.traveldabble.ui.theme.CoverOcean
import com.dabber.traveldabble.ui.theme.CoverRoyal
import com.dabber.traveldabble.ui.theme.CoverSunset

object MockData {

    val hanoiPlaces = listOf(
        Place("vn1", "Sofitel Legend Metropole", PlaceCategory.STAY, 21.0253, 105.8564, 4.9f, "Historic French-colonial luxury hotel in the heart of Hanoi.", "Check-in 14:00"),
        Place("vn2", "St. Joseph's Cathedral", PlaceCategory.SIGHT, 21.0288, 105.8495, 4.7f, "Neo-Gothic 19th-century cathedral surrounded by lively street cafes.", "8:00 - 17:00"),
        Place("vn3", "Cafe Giang (Egg Coffee)", PlaceCategory.FOOD, 21.0355, 105.8530, 4.8f, "Birthplace of Hanoi's legendary cà phê trứng since 1946.", "7:00 - 22:00"),
        Place("vn4", "Temple of Literature", PlaceCategory.SIGHT, 21.0294, 105.8360, 4.8f, "Vietnam's first national university, founded in 1070 with courtyards and stone turtles.", "8:00 - 17:30"),
        Place("vn5", "Bun Cha Huong Lien", PlaceCategory.FOOD, 21.0167, 105.8528, 4.6f, "Famous grilled pork noodle spot where Anthony Bourdain and Obama dined.", "8:00 - 20:30"),
        Place("vn6", "Tuan Chau Marina Port", PlaceCategory.TRANSIT, 20.9272, 106.9892, 4.7f, "Main cruise harbor connecting Hanoi to emerald Ha Long Bay.", "6:00 - 18:00"),
        Place("vn7", "Sung Sot (Surprise) Cave", PlaceCategory.SIGHT, 20.8755, 107.0911, 4.9f, "Massive limestone chamber with dramatic stalactite formations in Ha Long Bay.", "8:00 - 16:30"),
        Place("vn8", "Ti Top Island Peak", PlaceCategory.ACTIVITY, 20.8600, 107.0780, 4.8f, "Crescent beach and 400-step climb for the most iconic 360-degree panorama of the bay.", "7:30 - 17:00"),
        Place("vn9", "Luon Cave Lagoon", PlaceCategory.ACTIVITY, 20.8711, 107.0850, 4.7f, "Kayak through a sea cave tunnel into a secluded lagoon home to golden monkeys.", "8:00 - 17:00"),
    )

    val centralPlaces = listOf(
        Place("vn10", "An Bang Beach Villa", PlaceCategory.STAY, 15.9125, 108.3411, 4.8f, "Peaceful beachfront resort with tropical gardens near Hoi An.", "Check-in 14:00"),
        Place("vn11", "Hoi An Ancient Town", PlaceCategory.SIGHT, 15.8801, 108.3280, 4.9f, "UNESCO World Heritage ancient trading port glowing with thousands of silk lanterns.", "Always open"),
        Place("vn12", "Japanese Covered Bridge", PlaceCategory.SIGHT, 15.8771, 108.3259, 4.8f, "Historic 16th-century arched wooden bridge with intricate carvings.", "8:00 - 21:00"),
        Place("vn13", "Banh Mi Phuong", PlaceCategory.FOOD, 15.8795, 108.3340, 4.7f, "World-renowned crunchy banh mi loaded with pate, grilled pork, and fresh herbs.", "6:30 - 21:30"),
        Place("vn14", "Golden Bridge Ba Na Hills", PlaceCategory.SIGHT, 15.9990, 107.9967, 4.9f, "150-meter golden skyway suspended by giant stone hands above the clouds.", "7:00 - 17:30"),
        Place("vn15", "Marble Mountains", PlaceCategory.SIGHT, 16.0041, 108.2633, 4.7f, "Five limestone peaks housing Buddhist grottoes, pagodas, and panoramic sea views.", "7:00 - 17:00"),
        Place("vn16", "My Khe Beach", PlaceCategory.ACTIVITY, 16.0592, 108.2467, 4.8f, "Wide golden sand beach with gentle surf, beach clubs, and palm trees.", "Always open"),
        Place("vn17", "Dragon Bridge Fire Show", PlaceCategory.SIGHT, 16.0611, 108.2272, 4.8f, "Iconic dragon-shaped bridge breathing fire and water every weekend at 21:00.", "21:00 Sat & Sun"),
    )

    val haGiangPlaces = listOf(
        Place("vn18", "Ha Giang Motorbike Base", PlaceCategory.STAY, 22.8233, 104.9839, 4.8f, "Adventure hub for loop riders, bike rentals, and local mountain guides.", "6:00 - 22:00"),
        Place("vn19", "Quan Ba Heaven Gate", PlaceCategory.SIGHT, 23.0645, 104.9922, 4.8f, "Panoramic pass overlooking the fairy Twin Mountains and Tam Son valley.", "Sunrise - sunset"),
        Place("vn20", "Dong Van Karst Ancient Town", PlaceCategory.SIGHT, 23.2789, 105.3622, 4.7f, "Century-old clay and stone houses nestled beneath towering limestone cliffs.", "Always open"),
        Place("vn21", "Ma Pi Leng Pass Skywalk", PlaceCategory.SIGHT, 23.2458, 105.4208, 4.9f, "King of mountain passes carved into vertical cliff faces above Tu San canyon.", "Sunrise - sunset"),
        Place("vn22", "Nho Que River Boat Tour", PlaceCategory.ACTIVITY, 23.2201, 105.4410, 4.9f, "Turquoise river boat cruise through Tu San, Southeast Asia's deepest canyon.", "7:30 - 17:30"),
        Place("vn23", "Du Gia Waterfall & Homestay", PlaceCategory.ACTIVITY, 22.9056, 105.2133, 4.8f, "Crystal blue mountain swimming hole surrounded by Tay ethnic minority stilt houses.", "Sunrise - sunset"),
    )

    val saigonPlaces = listOf(
        Place("vn24", "The Myst Dong Khoi", PlaceCategory.STAY, 10.7745, 106.7058, 4.8f, "Artisan boutique hotel celebrating old Saigon architecture with rooftop pool.", "Check-in 14:00"),
        Place("vn25", "Ben Thanh Market", PlaceCategory.FOOD, 10.7725, 106.6980, 4.5f, "Historic marketplace packed with street food stalls, spices, coffee, and crafts.", "6:00 - 22:00"),
        Place("vn26", "Saigon Central Post Office", PlaceCategory.SIGHT, 10.7798, 106.6999, 4.7f, "Grand French colonial post office designed with vaulted ceilings and telephone booths.", "7:30 - 18:00"),
        Place("vn27", "War Remnants Museum", PlaceCategory.SIGHT, 10.7794, 106.6923, 4.8f, "Compelling museum chronicling modern Vietnamese history and peace photography.", "7:30 - 17:30"),
        Place("vn28", "Cuc Gach Quan Dining", PlaceCategory.FOOD, 10.7915, 106.6872, 4.7f, "Traditional country-style Vietnamese homecooking in a restored French villa.", "9:00 - 23:00"),
        Place("vn29", "Cu Chi Tunnels Historical Complex", PlaceCategory.ACTIVITY, 11.1432, 106.4632, 4.7f, "Intricate underground tunnel network stretching over 250 kilometers.", "7:00 - 17:00"),
        Place("vn30", "Cai Rang Floating Market", PlaceCategory.ACTIVITY, 10.0058, 105.7483, 4.8f, "Vibrant Mekong Delta floating market with wooden boats selling fruits and noodle soup.", "5:00 - 11:00"),
    )

    val ninhBinhPlaces = listOf(
        Place("vn31", "Trang An Grottoes Boat Tour", PlaceCategory.ACTIVITY, 20.2588, 105.9172, 4.9f, "UNESCO World Heritage rowboat excursion through limestone caves and emerald rivers.", "7:00 - 17:00"),
        Place("vn32", "Hang Mua Dragon Viewpoint", PlaceCategory.SIGHT, 20.2319, 105.9422, 4.8f, "500 stone steps leading to a stone dragon guarding breathtaking views over Tam Coc.", "6:00 - 19:00"),
        Place("vn33", "Bai Dinh Great Pagoda", PlaceCategory.SIGHT, 20.2741, 105.8672, 4.7f, "Southeast Asia's largest Buddhist temple complex with 500 Arhat stone statues.", "6:00 - 21:00"),
        Place("vn34", "Tam Coc Rice Paddies", PlaceCategory.SIGHT, 20.2198, 105.9388, 4.8f, "Scenic river journey through golden rice fields nestled between karst peaks.", "7:00 - 17:30"),
    )

    val hanoiTrip = Trip(
        id = "t1",
        title = "Hanoi & Ha Long Bay Explorer",
        destination = "Hanoi & Ha Long",
        country = "Vietnam",
        startDate = "Oct 15",
        endDate = "Oct 22",
        daysUntil = 12,
        coverColors = CoverOcean,
        travelers = 2,
        days = listOf(
            DayPlan(1, "Wed, Oct 15", listOf(
                ActivityItem("a1", hanoiPlaces[0], "14:00", "15:30", "Check in at Sofitel Legend Metropole"),
                ActivityItem("a2", hanoiPlaces[1], "16:00", "17:30", "Stroll to St. Joseph's Cathedral & Hoan Kiem"),
                ActivityItem("a3", hanoiPlaces[2], "17:45", "18:45", "Authentic egg coffee tasting"),
                ActivityItem("a4", hanoiPlaces[4], "19:30", "21:00", "Obama Bun Cha grilled pork noodles dinner"),
            )),
            DayPlan(2, "Thu, Oct 16", listOf(
                ActivityItem("a5", hanoiPlaces[3], "09:00", "11:30", "Explore Temple of Literature & gardens"),
                ActivityItem("a6", hanoiPlaces[1], "13:30", "15:30", "Old Quarter silk street & craft shops"),
                ActivityItem("a7", hanoiPlaces[0], "17:00", "18:30", "Bamboo Bar cocktails at the Metropole"),
            )),
            DayPlan(3, "Fri, Oct 17", listOf(
                ActivityItem("a8", hanoiPlaces[5], "08:30", "11:45", "Limousine bus transfer to Tuan Chau Marina"),
                ActivityItem("a9", hanoiPlaces[6], "14:00", "16:00", "Cruise excursion to Sung Sot (Surprise) Cave"),
                ActivityItem("a10", hanoiPlaces[8], "16:30", "18:00", "Sunset kayaking in Luon Cave lagoon"),
            )),
            DayPlan(4, "Sat, Oct 18", listOf(
                ActivityItem("a11", hanoiPlaces[7], "07:30", "09:30", "Climb Ti Top Island for panoramic bay view"),
                ActivityItem("a12", hanoiPlaces[5], "11:30", "15:00", "Scenic return cruise & limousine to Hanoi"),
            )),
        ),
        budget = Budget(
            total = 1950.0,
            categories = listOf("Lodging" to 850.0, "Food" to 400.0, "Transport" to 300.0, "Activities" to 300.0, "Other" to 100.0),
            expenses = listOf(
                Expense("e1", "Sofitel Legend Metropole (2 nights)", "Lodging", 440.0, "Oct 15"),
                Expense("e2", "Ha Long Heritage Cruise (1 night)", "Lodging", 410.0, "Oct 17"),
                Expense("e3", "Limousine roundtrip Hanoi - Ha Long", "Transport", 70.0, "Oct 14"),
                Expense("e4", "Bun Cha Huong Lien dinner", "Food", 22.0, "Oct 15"),
                Expense("e5", "Giang Egg Coffee tasting", "Food", 12.0, "Oct 15"),
                Expense("e6", "Temple of Literature tickets x2", "Activities", 8.0, "Oct 16"),
            ),
        ),
    )

    val centralTrip = Trip(
        id = "t2",
        title = "Central Vietnam Heritage & Coast",
        destination = "Da Nang & Hoi An",
        country = "Vietnam",
        startDate = "Nov 5",
        endDate = "Nov 12",
        daysUntil = 33,
        coverColors = CoverSunset,
        travelers = 2,
        days = listOf(
            DayPlan(1, "Wed, Nov 5", listOf(
                ActivityItem("b1", centralPlaces[0], "14:00", "15:30", "Check in at An Bang Beach Villa"),
                ActivityItem("b2", centralPlaces[1], "16:30", "19:00", "Walk through lantern-lit Hoi An Ancient Town"),
                ActivityItem("b3", centralPlaces[3], "19:30", "20:30", "Dinner at Banh Mi Phuong"),
            )),
            DayPlan(2, "Thu, Nov 6", listOf(
                ActivityItem("b4", centralPlaces[2], "09:00", "11:00", "Japanese Covered Bridge & ancient assembly halls"),
                ActivityItem("b5", centralPlaces[6], "15:00", "18:00", "Afternoon swim & relax at My Khe Beach"),
                ActivityItem("b6", centralPlaces[7], "20:30", "21:30", "Watch Dragon Bridge Fire Show in Da Nang"),
            )),
            DayPlan(3, "Fri, Nov 7", listOf(
                ActivityItem("b7", centralPlaces[4], "08:30", "13:00", "Cable car to Golden Bridge at Ba Na Hills"),
                ActivityItem("b8", centralPlaces[5], "14:30", "17:00", "Explore caves & temples of Marble Mountains"),
            )),
        ),
        budget = Budget(
            total = 1600.0,
            categories = listOf("Lodging" to 650.0, "Food" to 380.0, "Transport" to 220.0, "Activities" to 250.0, "Other" to 100.0),
            expenses = listOf(
                Expense("eb1", "An Bang Beach Villa (3 nights)", "Lodging", 360.0, "Nov 5"),
                Expense("eb2", "Ba Na Hills Cable Car + Golden Bridge x2", "Activities", 72.0, "Nov 7"),
                Expense("eb3", "Banh Mi Phuong & Hoi An Cao Lau dinner", "Food", 18.0, "Nov 5"),
                Expense("eb4", "Marble Mountains entry & elevator", "Activities", 10.0, "Nov 7"),
            ),
        ),
    )

    val haGiangTrip = Trip(
        id = "t3",
        title = "Ha Giang Loop Motorbike Adventure",
        destination = "Ha Giang & Dong Van",
        country = "Vietnam",
        startDate = "Dec 3",
        endDate = "Dec 8",
        daysUntil = 61,
        coverColors = CoverAlpine,
        travelers = 2,
        days = listOf(
            DayPlan(1, "Wed, Dec 3", listOf(
                ActivityItem("c1", haGiangPlaces[0], "07:30", "09:00", "Motorbike briefing & helmet fitting"),
                ActivityItem("c2", haGiangPlaces[1], "10:30", "12:30", "Quan Ba Heaven Gate & Fairy Mountains"),
                ActivityItem("c3", haGiangPlaces[2], "15:30", "18:00", "Arrive in Dong Van Ancient Town"),
            )),
            DayPlan(2, "Thu, Dec 4", listOf(
                ActivityItem("c4", haGiangPlaces[3], "08:30", "11:30", "Ride along epic Ma Pi Leng Pass"),
                ActivityItem("c5", haGiangPlaces[4], "13:00", "15:30", "Boat cruise on turquoise Nho Que River"),
                ActivityItem("c6", haGiangPlaces[5], "17:00", "19:00", "Du Gia Waterfall homestay & family dinner"),
            )),
        ),
        budget = Budget(
            total = 850.0,
            categories = listOf("Lodging" to 200.0, "Food" to 220.0, "Transport" to 250.0, "Activities" to 180.0),
            expenses = listOf(
                Expense("ec1", "Honda XR150 Motorbike rental (4 days)", "Transport", 120.0, "Dec 3"),
                Expense("ec2", "Nho Que River boat tour x2", "Activities", 18.0, "Dec 4"),
                Expense("ec3", "Dong Van Homestay + dinner", "Lodging", 35.0, "Dec 3"),
            ),
        ),
    )

    val saigonTrip = Trip(
        id = "t4",
        title = "Saigon & Mekong Delta Escape",
        destination = "Ho Chi Minh City",
        country = "Vietnam",
        startDate = "Jan 10",
        endDate = "Jan 16",
        daysUntil = null,
        coverColors = CoverRoyal,
        travelers = 2,
        days = listOf(
            DayPlan(1, "Sat, Jan 10", listOf(
                ActivityItem("d1", saigonPlaces[0], "14:00", "15:30", "Check in at The Myst Dong Khoi"),
                ActivityItem("d2", saigonPlaces[2], "16:00", "17:30", "Visit Saigon Central Post Office"),
                ActivityItem("d3", saigonPlaces[4], "18:30", "20:30", "Dinner at Cuc Gach Quan"),
            )),
            DayPlan(2, "Sun, Jan 11", listOf(
                ActivityItem("d4", saigonPlaces[1], "08:30", "11:00", "Morning coffee & street food at Ben Thanh"),
                ActivityItem("d5", saigonPlaces[3], "13:30", "16:00", "War Remnants Museum exhibition"),
            )),
            DayPlan(3, "Mon, Jan 12", listOf(
                ActivityItem("d6", saigonPlaces[5], "08:00", "13:00", "Cu Chi Tunnels historical exploration"),
                ActivityItem("d7", saigonPlaces[6], "15:00", "19:00", "Travel to Can Tho for Floating Market"),
            )),
        ),
        budget = Budget(
            total = 1400.0,
            categories = listOf("Lodging" to 580.0, "Food" to 360.0, "Transport" to 240.0, "Activities" to 220.0),
            expenses = listOf(
                Expense("ed1", "The Myst Dong Khoi (2 nights)", "Lodging", 280.0, "Jan 10"),
                Expense("ed2", "Cuc Gach Quan gourmet dinner", "Food", 48.0, "Jan 10"),
                Expense("ed3", "Cu Chi Tunnels tour with speedboat", "Activities", 75.0, "Jan 12"),
            ),
        ),
    )

    val ninhBinhTrip = Trip(
        id = "t5",
        title = "Ninh Binh Karsts & Ancient Capital",
        destination = "Ninh Binh",
        country = "Vietnam",
        startDate = "Feb 20",
        endDate = "Feb 24",
        daysUntil = null,
        coverColors = CoverForest,
        travelers = 1,
        days = listOf(
            DayPlan(1, "Fri, Feb 20", listOf(
                ActivityItem("f1", ninhBinhPlaces[0], "08:30", "12:00", "Trang An World Heritage boat tour"),
                ActivityItem("f2", ninhBinhPlaces[1], "15:30", "18:00", "Climb Hang Mua Dragon Viewpoint for sunset"),
            )),
            DayPlan(2, "Sat, Feb 21", listOf(
                ActivityItem("f3", ninhBinhPlaces[2], "09:00", "12:30", "Bai Dinh Great Pagoda complex visit"),
                ActivityItem("f4", ninhBinhPlaces[3], "14:00", "16:30", "Tam Coc riverboat through golden paddies"),
            )),
        ),
        budget = Budget(
            total = 600.0,
            categories = listOf("Lodging" to 220.0, "Food" to 150.0, "Transport" to 120.0, "Activities" to 110.0),
            expenses = listOf(
                Expense("ef1", "Trang An Grottoes boat ticket", "Activities", 11.0, "Feb 20"),
                Expense("ef2", "Hang Mua dragon peak entrance", "Activities", 4.5, "Feb 20"),
                Expense("ef3", "Ninh Binh Karst Eco Lodge (2 nights)", "Lodging", 110.0, "Feb 20"),
            ),
        ),
    )

    val trips = listOf(
        hanoiTrip,
        centralTrip,
        haGiangTrip,
        saigonTrip,
        ninhBinhTrip,
    )

    val destinations = listOf(
        Destination("d1", "Ha Long Bay", "Vietnam", "Emerald waters, floating villages and towering limestone karsts", 4.9f,
            listOf("Nature", "Cruise", "Adventure"), CoverOcean),
        Destination("d2", "Hoi An", "Vietnam", "Lantern-lit ancient town, tailor shops and riverside heritage dining", 4.8f,
            listOf("Culture", "Food", "Heritage"), CoverSunset),
        Destination("d3", "Hanoi", "Vietnam", "36 Old Quarter guild streets, egg coffee and French colonial avenues", 4.8f,
            listOf("Culture", "Food", "City"), CoverOcean),
        Destination("d4", "Da Nang", "Vietnam", "Golden Bridge at Ba Na Hills, Marble Mountains and My Khe beach", 4.7f,
            listOf("Beach", "Mountains", "City"), CoverAlpine),
        Destination("d5", "Ha Giang", "Vietnam", "Ma Pi Leng Pass, Tu San Canyon and Dong Van Karst Plateau", 4.9f,
            listOf("Adventure", "Mountains", "Trekking"), CoverAlpine),
        Destination("d6", "Ninh Binh", "Vietnam", "Trang An grottoes, Hang Mua dragon peak and Tam Coc rice fields", 4.8f,
            listOf("Nature", "Culture", "Boat Tour"), CoverForest),
        Destination("d7", "Ho Chi Minh City", "Vietnam", "Vibrant Saigon street food, rooftop bars and rich history", 4.7f,
            listOf("Food", "City", "Nightlife"), CoverRoyal),
        Destination("d8", "Phong Nha", "Vietnam", "World's largest cave systems and pristine jungle expeditions", 4.9f,
            listOf("Adventure", "Nature", "Caving"), CoverForest),
        Destination("d9", "Sa Pa", "Vietnam", "Emerald terraced rice fields and Fansipan roof of Indochina", 4.7f,
            listOf("Mountains", "Trekking", "Nature"), CoverForest),
    )

    val exploreTags = listOf("All", "Culture", "Food", "Nature", "Beach", "Mountains", "Adventure", "Heritage", "City")

    val chatSuggestions = listOf(
        "Plan 3 days in Hanoi & Ha Long",
        "Best street food spots in Hoi An",
        "How to do the Ha Giang motorbike loop?",
        "Best coffee shops in Hanoi Old Quarter",
    )

    val chatMessages = listOf(
        ChatMessage("m1", fromAi = true, text = "Xin chào! I'm your Vietnam travel copilot. I can draft itineraries across Hanoi, Ha Long Bay, Hoi An, Da Nang, Saigon, and beyond. What would you like to plan?"),
        ChatMessage("m2", fromAi = false, text = "Can you sketch Day 1 in Hanoi? We land at Noi Bai around 13:00."),
        ChatMessage(
            "m3", fromAi = true,
            text = "Landing at 13:00, here is a relaxed first afternoon and evening in Hanoi's historic French Quarter & Old Quarter:",
            attachment = ChatAttachment(
                title = "Day 1, Hanoi Arrival & Old Quarter",
                rows = listOf(
                    "14:00" to "Check in at Sofitel Legend Metropole Hanoi",
                    "16:00" to "Stroll to St. Joseph's Cathedral & Hoan Kiem Lake",
                    "17:45" to "Original Egg Coffee tasting at Cafe Giang",
                    "19:30" to "Dinner at Bun Cha Huong Lien (Obama Bun Cha)",
                ),
            ),
        ),
        ChatMessage("m4", fromAi = false, text = "Add this to my Hanoi & Ha Long trip!"),
        ChatMessage("m5", fromAi = true, text = "Done! Day 1 is saved to your itinerary. Would you like me to add Day 2 for Temple of Literature and an evening Street Food Tour, or plan your Ha Long Bay luxury cruise?"),
    )
}
