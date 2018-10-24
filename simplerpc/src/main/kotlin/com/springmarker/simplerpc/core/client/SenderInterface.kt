package com.springmarker.simplerpc.core.client

import java.lang.reflect.Method

/**
 * 此接口用于定义如何发送消息，主要定义
 *
 * @author Frank
 * @date 2018/10/16 22:29
 */
interface SenderInterface {

    fun send(method: Method, args: Array<out Any>): Any?


}