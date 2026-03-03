package com.github.rodrigotimoteo.kboyemu.di

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

/**
 * Koin module that auto-discovers all annotated classes (`@Single`, `@KoinViewModel`, etc.)
 * under the `com.github.rodrigotimoteo.kboyemu` package tree via KSP code generation.
 *
 * @author rodrigotimoteo
 */
@Module
@ComponentScan("com.github.rodrigotimoteo.kboyemu")
class AppModule

