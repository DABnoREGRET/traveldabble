package com.dabber.traveldabble.di

import com.dabber.traveldabble.routing.RoutingService
import org.koin.dsl.module

val serverModule = module {
    single { RoutingService() }
}
