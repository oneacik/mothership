package com.ksidelta.library.logger

import org.slf4j.LoggerFactory;


class Logger(val klass: Class<*>) {
    val logger = LoggerFactory.getLogger(klass)

    /**
     * format is "blah blah {} blah"
     */
    fun log(format: String, vararg o: Any) {
        logger.error(format, *o)
    }
}