package com.uson.skim.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class SkimServerApplication

fun main(args: Array<String>) {
	runApplication<SkimServerApplication>(*args)
}
