package com.dabber.traveldabble.seed

import com.dabber.traveldabble.auth.PasswordService
import com.dabber.traveldabble.db.*
import com.dabber.traveldabble.model.PlaceCategory
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object SeedData {

    fun seed() {
        transaction {
            if (Users.selectAll().where { Users.username eq "demo" }.count() > 0) {
                return@transaction
            }

            val demoUserId = UUID.randomUUID()
            val now = System.currentTimeMillis()

            // 1. Create demo user
            Users.insert {
                it[id] = demoUserId
                it[username] = "demo"
                it[email] = "demo@traveldabble.local"
                it[passwordHash] = PasswordService.hash("demo123")
                it[displayName] = "Demo Traveler"
                it[createdAt] = now
            }

            // 2. Create Destinations
            val dHanoi = insertDestination("Hanoi", "Vietnam", "Old Quarter streets, egg coffee and centuries-old temples", 4.8, listOf("Culture", "Food", "Heritage"), listOf(0xFF059669.toInt(), 0xFF0D9488.toInt()))
            val dHaLong = insertDestination("Ha Long Bay", "Vietnam", "Emerald waters, floating villages and limestone karsts", 4.9, listOf("Nature", "Adventure", "Cruise"), listOf(0xFF059669.toInt(), 0xFF0D9488.toInt()))
            val dHoiAn = insertDestination("Hoi An", "Vietnam", "Lantern-lit ancient town, tailor shops and riverside dining", 4.8, listOf("Culture", "Food", "Heritage"), listOf(0xFFF97316.toInt(), 0xFFDC2626.toInt()))
            val dDaNang = insertDestination("Da Nang", "Vietnam", "Golden Bridge at Ba Na Hills and pristine My Khe Beach", 4.7, listOf("Beach", "Mountains", "City"), listOf(0xFF0284C7.toInt(), 0xFF06B6D4.toInt()))
            val dHaGiang = insertDestination("Ha Giang", "Vietnam", "Epic mountain passes, Ma Pi Leng and Dong Van Karst", 4.9, listOf("Adventure", "Mountains", "Trekking"), listOf(0xFFEAB308.toInt(), 0xFF059669.toInt()))
            val dNinhBinh = insertDestination("Ninh Binh", "Vietnam", "Trang An grottoes, Hang Mua dragon peak and river karsts", 4.8, listOf("Nature", "Culture", "Boat Tour"), listOf(0xFF059669.toInt(), 0xFF047857.toInt()))
            val dSaigon = insertDestination("Ho Chi Minh City", "Vietnam", "Vibrant Saigon street food, rooftop bars and rich history", 4.7, listOf("Food", "City", "Nightlife"), listOf(0xFFF97316.toInt(), 0xFFDC2626.toInt()))
            val dPhongNha = insertDestination("Phong Nha", "Vietnam", "World's largest cave systems and pristine national park jungles", 4.9, listOf("Adventure", "Nature", "Caving"), listOf(0xFF047857.toInt(), 0xFF065F46.toInt()))
            val dSaPa = insertDestination("Sa Pa", "Vietnam", "Terraced emerald rice fields and Fansipan mountain peak", 4.7, listOf("Mountains", "Trekking", "Nature"), listOf(0xFF059669.toInt(), 0xFF10B981.toInt()))

            // 3. Create Places
            val pMetropole = insertPlace("Sofitel Legend Metropole", PlaceCategory.STAY.name, 21.0253, 105.8564, 4.9, "Historic French-colonial luxury hotel in the heart of Hanoi.", "Check-in 14:00")
            val pCathedral = insertPlace("St. Joseph's Cathedral", PlaceCategory.SIGHT.name, 21.0288, 105.8495, 4.7, "Neo-Gothic 19th-century cathedral surrounded by lively street cafes.", "8:00 - 17:00")
            val pCafeGiang = insertPlace("Cafe Giang (Egg Coffee)", PlaceCategory.FOOD.name, 21.0355, 105.8530, 4.8, "Birthplace of Hanoi's legendary ca phe trung since 1946.", "7:00 - 22:00")
            val pTempleLit = insertPlace("Temple of Literature", PlaceCategory.SIGHT.name, 21.0294, 105.8360, 4.8, "Vietnam's first national university, founded in 1070 with courtyards and stone turtles.", "8:00 - 17:30")
            val pBunCha = insertPlace("Bun Cha Huong Lien", PlaceCategory.FOOD.name, 21.0167, 105.8528, 4.6, "Famous grilled pork noodle spot where Anthony Bourdain and Obama dined.", "8:00 - 20:30")
            val pTuanChau = insertPlace("Tuan Chau Marina Port", PlaceCategory.TRANSIT.name, 20.9272, 106.9892, 4.7, "Main cruise harbor connecting Hanoi to emerald Ha Long Bay.", "6:00 - 18:00")
            val pSungSot = insertPlace("Sung Sot (Surprise) Cave", PlaceCategory.SIGHT.name, 20.8755, 107.0911, 4.9, "Massive limestone chamber with dramatic stalactite formations in Ha Long Bay.", "8:00 - 16:30")
            val pTiTop = insertPlace("Ti Top Island Peak", PlaceCategory.ACTIVITY.name, 20.8600, 107.0780, 4.8, "Crescent beach and 400-step climb for the most iconic 360-degree panorama of the bay.", "7:30 - 17:00")
            val pLuonCave = insertPlace("Luon Cave Lagoon", PlaceCategory.ACTIVITY.name, 20.8711, 107.0850, 4.7, "Kayak through a sea cave tunnel into a secluded lagoon home to golden monkeys.", "8:00 - 17:00")

            val pAnBang = insertPlace("An Bang Beach Villa", PlaceCategory.STAY.name, 15.9125, 108.3411, 4.8, "Peaceful beachfront resort with tropical gardens near Hoi An.", "Check-in 14:00")
            val pHoiAnTown = insertPlace("Hoi An Ancient Town", PlaceCategory.SIGHT.name, 15.8801, 108.3280, 4.9, "UNESCO World Heritage ancient trading port glowing with thousands of silk lanterns.", "Always open")
            val pBridge = insertPlace("Japanese Covered Bridge", PlaceCategory.SIGHT.name, 15.8771, 108.3259, 4.8, "Historic 16th-century arched wooden bridge with intricate carvings.", "8:00 - 21:00")
            val pBanhMiPhuong = insertPlace("Banh Mi Phuong", PlaceCategory.FOOD.name, 15.8795, 108.3340, 4.7, "World-renowned crunchy banh mi loaded with pate, grilled pork, and fresh herbs.", "6:30 - 21:30")
            val pGoldenBridge = insertPlace("Golden Bridge Ba Na Hills", PlaceCategory.SIGHT.name, 15.9990, 107.9967, 4.9, "150-meter golden skyway suspended by giant stone hands above the clouds.", "7:00 - 17:30")
            val pMarbleMtn = insertPlace("Marble Mountains", PlaceCategory.SIGHT.name, 16.0041, 108.2633, 4.7, "Five limestone peaks housing Buddhist grottoes, pagodas, and panoramic sea views.", "7:00 - 17:00")
            val pMyKhe = insertPlace("My Khe Beach", PlaceCategory.ACTIVITY.name, 16.0592, 108.2467, 4.8, "Wide golden sand beach with gentle surf, beach clubs, and palm trees.", "Always open")
            val pDragonBridge = insertPlace("Dragon Bridge Fire Show", PlaceCategory.SIGHT.name, 16.0611, 108.2272, 4.8, "Iconic dragon-shaped bridge breathing fire and water every weekend at 21:00.", "21:00 Sat & Sun")

            val pHaGiangBase = insertPlace("Ha Giang Motorbike Base", PlaceCategory.STAY.name, 22.8233, 104.9839, 4.8, "Adventure hub for loop riders, bike rentals, and local mountain guides.", "6:00 - 22:00")
            val pQuanBa = insertPlace("Quan Ba Heaven Gate", PlaceCategory.SIGHT.name, 23.0645, 104.9922, 4.8, "Panoramic pass overlooking the fairy Twin Mountains and Tam Son valley.", "Sunrise - sunset")
            val pDongVan = insertPlace("Dong Van Karst Ancient Town", PlaceCategory.SIGHT.name, 23.2789, 105.3622, 4.7, "Century-old clay and stone houses nestled beneath towering limestone cliffs.", "Always open")
            val pMaPiLeng = insertPlace("Ma Pi Leng Pass Skywalk", PlaceCategory.SIGHT.name, 23.2458, 105.4208, 4.9, "King of mountain passes carved into vertical cliff faces above Tu San canyon.", "Sunrise - sunset")
            val pNhoQue = insertPlace("Nho Que River Boat Tour", PlaceCategory.ACTIVITY.name, 23.2201, 105.4410, 4.9, "Turquoise river boat cruise through Tu San, Southeast Asia's deepest canyon.", "7:30 - 17:30")
            val pDuGia = insertPlace("Du Gia Waterfall & Homestay", PlaceCategory.ACTIVITY.name, 22.9056, 105.2133, 4.8, "Crystal blue mountain swimming hole surrounded by Tay ethnic minority stilt houses.", "Sunrise - sunset")

            val pMyst = insertPlace("The Myst Dong Khoi", PlaceCategory.STAY.name, 10.7745, 106.7058, 4.8, "Artisan boutique hotel celebrating old Saigon architecture with rooftop pool.", "Check-in 14:00")
            val pBenThanh = insertPlace("Ben Thanh Market", PlaceCategory.FOOD.name, 10.7725, 106.6980, 4.5, "Historic marketplace packed with street food stalls, spices, coffee, and crafts.", "6:00 - 22:00")
            val pPostOffice = insertPlace("Saigon Central Post Office", PlaceCategory.SIGHT.name, 10.7798, 106.6999, 4.7, "Grand French colonial post office designed with vaulted ceilings and telephone booths.", "7:30 - 18:00")
            val pWarMuseum = insertPlace("War Remnants Museum", PlaceCategory.SIGHT.name, 10.7794, 106.6923, 4.8, "Compelling museum chronicling modern Vietnamese history and peace photography.", "7:30 - 17:30")
            val pCucGach = insertPlace("Cuc Gach Quan Dining", PlaceCategory.FOOD.name, 10.7915, 106.6872, 4.7, "Traditional country-style Vietnamese homecooking in a restored French villa.", "9:00 - 23:00")
            val pCuChi = insertPlace("Cu Chi Tunnels Historical Complex", PlaceCategory.ACTIVITY.name, 11.1432, 106.4632, 4.7, "Intricate underground tunnel network stretching over 250 kilometers.", "7:00 - 17:00")
            val pFloatingMkt = insertPlace("Cai Rang Floating Market", PlaceCategory.ACTIVITY.name, 10.0058, 105.7483, 4.8, "Vibrant Mekong Delta floating market with wooden boats selling fruits and noodle soup.", "5:00 - 11:00")

            val pTrangAn = insertPlace("Trang An Grottoes Boat Tour", PlaceCategory.ACTIVITY.name, 20.2588, 105.9172, 4.9, "UNESCO World Heritage rowboat excursion through limestone caves and emerald rivers.", "7:00 - 17:00")
            val pHangMua = insertPlace("Hang Mua Dragon Viewpoint", PlaceCategory.SIGHT.name, 20.2319, 105.9422, 4.8, "500 stone steps leading to a stone dragon guarding breathtaking views over Tam Coc.", "6:00 - 19:00")
            val pBaiDinh = insertPlace("Bai Dinh Great Pagoda", PlaceCategory.SIGHT.name, 20.2741, 105.8672, 4.7, "Southeast Asia's largest Buddhist temple complex with 500 Arhat stone statues.", "6:00 - 21:00")
            val pTamCoc = insertPlace("Tam Coc Rice Paddies", PlaceCategory.SIGHT.name, 20.2198, 105.9388, 4.8, "Scenic river journey through golden rice fields nestled between karst peaks.", "7:00 - 17:30")

            // 4. Create Trip 1: Hanoi & Ha Long Bay Explorer
            val t1Id = insertTrip(demoUserId, "Hanoi & Ha Long Bay Explorer", "Hanoi & Ha Long", "Vietnam", "Oct 15", "Oct 22", 12, listOf(0xFF059669.toInt(), 0xFF0D9488.toInt()), 2, now)
            val t1d1 = insertDayPlan(t1Id, 1, "Wed, Oct 15")
            insertActivity(t1d1, pMetropole, "14:00", "15:30", "Check in at Sofitel Legend Metropole")
            insertActivity(t1d1, pCathedral, "16:00", "17:30", "Stroll to St. Joseph's Cathedral & Hoan Kiem")
            insertActivity(t1d1, pCafeGiang, "17:45", "18:45", "Authentic egg coffee tasting")
            insertActivity(t1d1, pBunCha, "19:30", "21:00", "Obama Bun Cha grilled pork noodles dinner")

            val t1d2 = insertDayPlan(t1Id, 2, "Thu, Oct 16")
            insertActivity(t1d2, pTempleLit, "09:00", "11:30", "Explore Temple of Literature & gardens")
            insertActivity(t1d2, pCathedral, "13:30", "15:30", "Old Quarter silk street & craft shops")
            insertActivity(t1d2, pMetropole, "17:00", "18:30", "Bamboo Bar cocktails at the Metropole")

            val t1d3 = insertDayPlan(t1Id, 3, "Fri, Oct 17")
            insertActivity(t1d3, pTuanChau, "08:30", "11:45", "Limousine bus transfer to Tuan Chau Marina")
            insertActivity(t1d3, pSungSot, "14:00", "16:00", "Cruise excursion to Sung Sot (Surprise) Cave")
            insertActivity(t1d3, pLuonCave, "16:30", "18:00", "Sunset kayaking in Luon Cave lagoon")

            val t1d4 = insertDayPlan(t1Id, 4, "Sat, Oct 18")
            insertActivity(t1d4, pTiTop, "07:30", "09:30", "Climb Ti Top Island for panoramic bay view")
            insertActivity(t1d4, pTuanChau, "11:30", "15:00", "Scenic return cruise & limousine to Hanoi")

            val b1Id = insertBudget(t1Id, 1950.0, listOf("Lodging" to 850.0, "Food" to 400.0, "Transport" to 300.0, "Activities" to 300.0, "Other" to 100.0))
            insertExpense(b1Id, "Sofitel Legend Metropole (2 nights)", "Lodging", 440.0, "Oct 15")
            insertExpense(b1Id, "Ha Long Heritage Cruise (1 night)", "Lodging", 410.0, "Oct 17")
            insertExpense(b1Id, "Limousine roundtrip Hanoi - Ha Long", "Transport", 70.0, "Oct 14")
            insertExpense(b1Id, "Bun Cha Huong Lien dinner", "Food", 22.0, "Oct 15")
            insertExpense(b1Id, "Giang Egg Coffee tasting", "Food", 12.0, "Oct 15")
            insertExpense(b1Id, "Temple of Literature tickets x2", "Activities", 8.0, "Oct 16")

            // 5. Create Trip 2: Central Vietnam Heritage & Coast
            val t2Id = insertTrip(demoUserId, "Central Vietnam Heritage & Coast", "Da Nang & Hoi An", "Vietnam", "Nov 5", "Nov 12", 33, listOf(0xFFF97316.toInt(), 0xFFDC2626.toInt()), 2, now)
            val t2d1 = insertDayPlan(t2Id, 1, "Wed, Nov 5")
            insertActivity(t2d1, pAnBang, "14:00", "15:30", "Check in at An Bang Beach Villa")
            insertActivity(t2d1, pHoiAnTown, "16:30", "19:00", "Walk through lantern-lit Hoi An Ancient Town")
            insertActivity(t2d1, pBanhMiPhuong, "19:30", "20:30", "Dinner at Banh Mi Phuong")

            val t2d2 = insertDayPlan(t2Id, 2, "Thu, Nov 6")
            insertActivity(t2d2, pBridge, "09:00", "11:00", "Japanese Covered Bridge & ancient assembly halls")
            insertActivity(t2d2, pMyKhe, "15:00", "18:00", "Afternoon swim & relax at My Khe Beach")
            insertActivity(t2d2, pDragonBridge, "20:30", "21:30", "Watch Dragon Bridge Fire Show in Da Nang")

            val t2d3 = insertDayPlan(t2Id, 3, "Fri, Nov 7")
            insertActivity(t2d3, pGoldenBridge, "08:30", "13:00", "Cable car to Golden Bridge at Ba Na Hills")
            insertActivity(t2d3, pMarbleMtn, "14:30", "17:00", "Explore caves & temples of Marble Mountains")

            val b2Id = insertBudget(t2Id, 1600.0, listOf("Lodging" to 650.0, "Food" to 380.0, "Transport" to 220.0, "Activities" to 250.0, "Other" to 100.0))
            insertExpense(b2Id, "An Bang Beach Villa (3 nights)", "Lodging", 360.0, "Nov 5")
            insertExpense(b2Id, "Ba Na Hills Cable Car + Golden Bridge x2", "Activities", 72.0, "Nov 7")
            insertExpense(b2Id, "Banh Mi Phuong & Hoi An Cao Lau dinner", "Food", 18.0, "Nov 5")
            insertExpense(b2Id, "Marble Mountains entry & elevator", "Activities", 10.0, "Nov 7")

            // 6. Create Trip 3: Ha Giang Loop
            val t3Id = insertTrip(demoUserId, "Ha Giang Loop Motorbike Adventure", "Ha Giang & Dong Van", "Vietnam", "Dec 3", "Dec 8", 61, listOf(0xFFEAB308.toInt(), 0xFF059669.toInt()), 2, now)
            val t3d1 = insertDayPlan(t3Id, 1, "Wed, Dec 3")
            insertActivity(t3d1, pHaGiangBase, "07:30", "09:00", "Motorbike briefing & helmet fitting")
            insertActivity(t3d1, pQuanBa, "10:30", "12:30", "Quan Ba Heaven Gate & Fairy Mountains")
            insertActivity(t3d1, pDongVan, "15:30", "18:00", "Arrive in Dong Van Ancient Town")

            val t3d2 = insertDayPlan(t3Id, 2, "Thu, Dec 4")
            insertActivity(t3d2, pMaPiLeng, "08:30", "11:30", "Ride along epic Ma Pi Leng Pass")
            insertActivity(t3d2, pNhoQue, "13:00", "15:30", "Boat cruise on turquoise Nho Que River")
            insertActivity(t3d2, pDuGia, "17:00", "19:00", "Du Gia Waterfall homestay & family dinner")

            val b3Id = insertBudget(t3Id, 850.0, listOf("Lodging" to 200.0, "Food" to 220.0, "Transport" to 250.0, "Activities" to 180.0))
            insertExpense(b3Id, "Honda XR150 Motorbike rental (4 days)", "Transport", 120.0, "Dec 3")
            insertExpense(b3Id, "Nho Que River boat tour x2", "Activities", 18.0, "Dec 4")
            insertExpense(b3Id, "Dong Van Homestay + dinner", "Lodging", 35.0, "Dec 3")

            // 7. Create Trip 4: Saigon & Mekong Delta
            val t4Id = insertTrip(demoUserId, "Saigon & Mekong Delta Escape", "Ho Chi Minh City", "Vietnam", "Jan 10", "Jan 16", null, listOf(0xFF7C3AED.toInt(), 0xFFDC2626.toInt()), 2, now)
            val t4d1 = insertDayPlan(t4Id, 1, "Sat, Jan 10")
            insertActivity(t4d1, pMyst, "14:00", "15:30", "Check in at The Myst Dong Khoi")
            insertActivity(t4d1, pPostOffice, "16:00", "17:30", "Visit Saigon Central Post Office")
            insertActivity(t4d1, pCucGach, "18:30", "20:30", "Dinner at Cuc Gach Quan")

            val t4d2 = insertDayPlan(t4Id, 2, "Sun, Jan 11")
            insertActivity(t4d2, pBenThanh, "08:30", "11:00", "Morning coffee & street food at Ben Thanh")
            insertActivity(t4d2, pWarMuseum, "13:30", "16:00", "War Remnants Museum exhibition")

            val t4d3 = insertDayPlan(t4Id, 3, "Mon, Jan 12")
            insertActivity(t4d3, pCuChi, "08:00", "13:00", "Cu Chi Tunnels historical exploration")
            insertActivity(t4d3, pFloatingMkt, "15:00", "19:00", "Travel to Can Tho for Floating Market")

            val b4Id = insertBudget(t4Id, 1400.0, listOf("Lodging" to 580.0, "Food" to 360.0, "Transport" to 240.0, "Activities" to 220.0))
            insertExpense(b4Id, "The Myst Dong Khoi (2 nights)", "Lodging", 280.0, "Jan 10")
            insertExpense(b4Id, "Cuc Gach Quan gourmet dinner", "Food", 48.0, "Jan 10")
            insertExpense(b4Id, "Cu Chi Tunnels tour with speedboat", "Activities", 75.0, "Jan 12")

            // 8. Create Trip 5: Ninh Binh
            val t5Id = insertTrip(demoUserId, "Ninh Binh Karsts & Ancient Capital", "Ninh Binh", "Vietnam", "Feb 20", "Feb 24", null, listOf(0xFF059669.toInt(), 0xFF047857.toInt()), 1, now)
            val t5d1 = insertDayPlan(t5Id, 1, "Fri, Feb 20")
            insertActivity(t5d1, pTrangAn, "08:30", "12:00", "Trang An World Heritage boat tour")
            insertActivity(t5d1, pHangMua, "15:30", "18:00", "Climb Hang Mua Dragon Viewpoint for sunset")

            val t5d2 = insertDayPlan(t5Id, 2, "Sat, Feb 21")
            insertActivity(t5d2, pBaiDinh, "09:00", "12:30", "Bai Dinh Great Pagoda complex visit")
            insertActivity(t5d2, pTamCoc, "14:00", "16:30", "Tam Coc riverboat through golden paddies")

            val b5Id = insertBudget(t5Id, 600.0, listOf("Lodging" to 220.0, "Food" to 150.0, "Transport" to 120.0, "Activities" to 110.0))
            insertExpense(b5Id, "Trang An Grottoes boat ticket", "Activities", 11.0, "Feb 20")
            insertExpense(b5Id, "Hang Mua dragon peak entrance", "Activities", 4.5, "Feb 20")
            insertExpense(b5Id, "Ninh Binh Karst Eco Lodge (2 nights)", "Lodging", 110.0, "Feb 20")

            // Add demo user as owner of all 5 trips
            listOf(t1Id, t2Id, t3Id, t4Id, t5Id).forEach { tripId ->
                TripMembers.insert {
                    it[TripMembers.id] = UUID.randomUUID()
                    it[TripMembers.tripId] = tripId
                    it[TripMembers.userId] = demoUserId
                    it[role] = "owner"
                    it[joinedAt] = now
                }
            }

            // Create initial in-app notifications for demo user
            Notifications.insert {
                it[userId] = demoUserId.toString()
                it[type] = "trip_reminder"
                it[title] = "Upcoming Trip: Hanoi & Ha Long"
                it[body] = "Your trip starts in 12 days on Oct 15! Check your itinerary and pack light."
                it[data] = """{"tripId":"$t1Id"}"""
                it[read] = false
                it[createdAt] = now
            }
            Notifications.insert {
                it[userId] = demoUserId.toString()
                it[type] = "collaboration"
                it[title] = "Welcome to TravelDabble"
                it[body] = "Explore Vietnam itineraries or chat with your AI travel copilot to plan your next adventure."
                it[data] = null
                it[read] = true
                it[createdAt] = now - 3600000
            }
        }
    }

    private fun insertDestination(name: String, country: String, tagline: String, rating: Double, tags: List<String>, cover: List<Int>): UUID {
        val id = UUID.randomUUID()
        Destinations.insert {
            it[Destinations.id] = id
            it[Destinations.name] = name
            it[Destinations.country] = country
            it[Destinations.tagline] = tagline
            it[Destinations.rating] = rating
            it[Destinations.tags] = Mappers.json.encodeToString(tags)
            it[Destinations.coverColors] = Mappers.json.encodeToString(cover)
        }
        return id
    }

    private fun insertPlace(name: String, category: String, lat: Double, lng: Double, rating: Double, description: String, openHours: String): UUID {
        val id = UUID.randomUUID()
        Places.insert {
            it[Places.id] = id
            it[Places.name] = name
            it[Places.category] = category
            it[Places.lat] = lat
            it[Places.lng] = lng
            it[Places.rating] = rating
            it[Places.description] = description
            it[Places.openHours] = openHours
        }
        return id
    }

    private fun insertTrip(userId: UUID, title: String, destination: String, country: String, startDate: String, endDate: String, daysUntil: Int?, cover: List<Int>, travelers: Int, createdAt: Long): UUID {
        val id = UUID.randomUUID()
        Trips.insert {
            it[Trips.id] = id
            it[Trips.userId] = userId
            it[Trips.title] = title
            it[Trips.destination] = destination
            it[Trips.country] = country
            it[Trips.startDate] = startDate
            it[Trips.endDate] = endDate
            it[Trips.daysUntil] = daysUntil
            it[Trips.coverColors] = Mappers.json.encodeToString(cover)
            it[Trips.travelers] = travelers
            it[Trips.createdAt] = createdAt
        }
        return id
    }

    private fun insertDayPlan(tripId: UUID, dayNumber: Int, dateLabel: String): UUID {
        val id = UUID.randomUUID()
        DayPlans.insert {
            it[DayPlans.id] = id
            it[DayPlans.tripId] = tripId
            it[DayPlans.dayNumber] = dayNumber
            it[DayPlans.dateLabel] = dateLabel
        }
        return id
    }

    private fun insertActivity(dayPlanId: UUID, placeId: UUID, startTime: String, endTime: String, note: String?): UUID {
        val id = UUID.randomUUID()
        Activities.insert {
            it[Activities.id] = id
            it[Activities.dayPlanId] = dayPlanId
            it[Activities.placeId] = placeId
            it[Activities.startTime] = startTime
            it[Activities.endTime] = endTime
            it[Activities.note] = note
        }
        return id
    }

    private fun insertBudget(tripId: UUID, total: Double, categories: List<Pair<String, Double>>): UUID {
        val id = UUID.randomUUID()
        Budgets.insert {
            it[Budgets.id] = id
            it[Budgets.tripId] = tripId
            it[Budgets.total] = total
            it[Budgets.categories] = Mappers.json.encodeToString(categories)
        }
        return id
    }

    private fun insertExpense(budgetId: UUID, title: String, category: String, amount: Double, date: String): UUID {
        val id = UUID.randomUUID()
        Expenses.insert {
            it[Expenses.id] = id
            it[Expenses.budgetId] = budgetId
            it[Expenses.title] = title
            it[Expenses.category] = category
            it[Expenses.amount] = amount
            it[Expenses.date] = date
        }
        return id
    }
}
