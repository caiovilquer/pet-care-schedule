package dev.vilquer.petcarescheduler.application.adapter.input.security

import org.springframework.security.oauth2.jwt.Jwt

typealias CurrentJwt = Jwt

fun Jwt.tutorId(): Long = this.subject.toLong()